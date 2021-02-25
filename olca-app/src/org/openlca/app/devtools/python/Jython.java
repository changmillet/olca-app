package org.openlca.app.devtools.python;

import java.io.File;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.app.util.ErrorReporter;
import org.python.util.PythonInterpreter;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

class Jython {

	static AtomicBoolean folderInitialized = new AtomicBoolean(false);

	private Jython() {
	}

	static void exec(String script) {
		var pyDir = new File(App.getWorkspace(), "python");
		if (!folderInitialized.get()) {
			initFolder(pyDir);
			System.setProperty("python.path", pyDir.getAbsolutePath());
			System.setProperty("python.home", pyDir.getAbsolutePath());
			folderInitialized.set(true);
		}
		try {

			// add class bindings from `bindings.properties`
			var imports = new StringBuilder();
			var props = new Properties();
			props.load(Jython.class.getResourceAsStream(
					"bindings.properties"));
			props.forEach((name, fullName) -> {
				imports.append("import ")
						.append(fullName)
						.append(" as ")
						.append(name)
						.append("\n");
			});

			// create the interpreter and execute the script
			try (var py = new PythonInterpreter()) {
				py.set("log", LoggerFactory.getLogger(Jython.class));
				py.set("db", Database.get());

				// first try to execute the imports
				try {
					py.exec(imports.toString());
				} catch (Exception e) {
					ErrorReporter.on(
							"Failed to execute imports",
							"Python imports:\n" + imports.toString(), e);
					return;
				}

				py.exec(script);
			}

		} catch (Exception e) {
			var log = LoggerFactory.getLogger(Jython.class);
			log.error("failed execute script", e);
		}

	}

	/** Extract the Python library in the workspace folder. */
	private static synchronized void initFolder(File pyDir) {
		if (folderInitialized.get())
			return;
		try {

			// check if the Python folder is tagged with the
			// current application version
			var versionFile = new File(pyDir, ".version");
			if (versionFile.exists()) {
				var version = Files.readString(versionFile.toPath());
				if (Objects.equals(version, App.getVersion()))
					return;
			}

			// replace it with the current version of the
			// packed Python (Jython) library
			if (pyDir.exists()) {
				FileUtils.deleteDirectory(pyDir);
			}
			Files.createDirectories(pyDir.toPath());
			var pyJar = "libs/jython-standalone-2.7.2.jar";
			try (var is = RcpActivator.getStream(pyJar)) {
				ZipUtil.unpack(is, pyDir, (entry) -> {
					if (entry.startsWith("Lib/") && entry.length() > 4) {
						return entry.substring(4);
					} else {
						return null;
					}
				});
			}
			Files.writeString(versionFile.toPath(), App.getVersion());
		} catch (Exception e) {
			var log = LoggerFactory.getLogger(Jython.class);
			log.error("failed to initialize Python folder " + pyDir, e);
		}
	}

}