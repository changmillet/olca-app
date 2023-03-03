package org.openlca.app.wizards.io;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.jsonld.ZipStore;
import org.openlca.jsonld.input.JsonImport;
import org.openlca.jsonld.input.UpdateMode;
import org.slf4j.LoggerFactory;

public class JsonImportWizard extends Wizard implements IImportWizard {

	private Page page;
	private File initialFile;

	public static void of(File file) {
		Wizards.forImport(
				"wizard.import.json",
				(JsonImportWizard w) -> w.initialFile = file);
	}

	public JsonImportWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle("openLCA JSON-LD Import");
		setDefaultPageImageDescriptor(
				Icon.IMPORT_ZIP_WIZARD.descriptor());
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void addPages() {
		page = new Page(initialFile);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		var zip = page.zip;
		if (zip == null || !zip.exists())
			return false;
		try {
			Database.getWorkspaceIdUpdater().beginTransaction();
			doRun(zip);
			return true;
		} catch (Exception e) {
			ErrorReporter.on("JSON import failed", e);
			return false;
		} finally {
			Database.getWorkspaceIdUpdater().endTransaction();
			Navigator.refresh();
			Cache.evictAll();
		}
	}

	private void doRun(File zip) throws Exception {
		var mode = page.updateMode;
		LoggerFactory.getLogger(getClass())
				.info("Import JSON LD package {} with update mode = {}", zip, mode);
		getContainer().run(true, true, (monitor) -> {
			monitor.beginTask(M.Import, IProgressMonitor.UNKNOWN);
			try (var store = ZipStore.open(zip)) {
				var importer = new JsonImport(store, Database.get());
				importer.setUpdateMode(mode);
				importer.run();
			} catch (Exception e) {
				throw new InvocationTargetException(e);
			}
		});
	}

	/**
	 * Contains settings for the JSON-LD import.
	 */
	private static class Page extends WizardPage {

		private final UpdateMode[] mods = {
				UpdateMode.NEVER,
				UpdateMode.IF_NEWER,
				UpdateMode.ALWAYS
		};
		UpdateMode updateMode = UpdateMode.NEVER;
		File zip;

		Page(File zip) {
			super("JsonImportPage");
			setTitle("Import an openLCA data package");
			this.zip = zip;
			setPageComplete(zip != null);
		}

		@Override
		public void createControl(Composite parent) {
			var body = new Composite(parent, SWT.NONE);
			UI.gridLayout(body, 1);

			var fileComp = UI.formComposite(body);
			UI.fillHorizontal(fileComp);
			UI.gridLayout(fileComp, 3);
			var fileText = UI.formText(fileComp, "File", SWT.READ_ONLY);
			if (zip != null) {
				fileText.setText(zip.getName());
			}
			UI.fillHorizontal(fileText);
			var browse = new Button(fileComp, SWT.NONE);
			browse.setText(M.Browse);

			Controls.onSelect(browse, e -> {
				var file = FileChooser.openFile()
						.withTitle("Select a zip file with openLCA data...")
						.withExtensions("*.zip")
						.select()
						.orElse(null);
				if (file != null) {
					zip = file;
					setPageComplete(true);
					fileText.setText(file.getName());
				}
			});

			// update mode
			var group = new Group(body, SWT.NONE);
			group.setText("Updating existing data sets in the database");
			UI.gridData(group, true, false);
			UI.gridLayout(group, 1);
			for (UpdateMode mode : mods) {
				Button option = new Button(group, SWT.RADIO);
				option.setText(getText(mode));
				option.setSelection(mode == updateMode);
				Controls.onSelect(option, (e) -> {
					updateMode = mode;
				});
			}
			setControl(body);
		}

		private String getText(UpdateMode mode) {
			return switch (mode) {
				case NEVER -> "Never update a data set that already exists";
				case IF_NEWER -> "Update data sets with newer versions";
				case ALWAYS -> "Overwrite all existing data sets";
			};
		}
	}
}
