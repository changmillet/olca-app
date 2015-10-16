package org.openlca.app.db;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.events.DatabaseEvent;
import org.openlca.app.events.DatabaseEvent.Type;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The wizard for database creation.
 */
public class DatabaseWizard extends Wizard {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private DatabaseWizardPage page;

	public DatabaseWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle(Messages.NewDatabase);
	}

	@Override
	public void addPages() {
		page = new DatabaseWizardPage();
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		try {
			String previousDb = null;
			if (Database.get() != null)
				previousDb = Database.get().getName();
			Editors.closeAll();
			IDatabaseConfiguration config = page.getPageData();
			Runner runner = (config instanceof DerbyConfiguration) ? new Runner(
					config, page.getSelectedContent()) : new Runner(config);
			getContainer().run(true, false, runner);
			new DatabaseUpdate(Database.get()).run();
			Navigator.refresh();
			if (previousDb != null)
				App.getEventBus().post(new DatabaseEvent(previousDb, Type.CLOSE));
			App.getEventBus().post(new DatabaseEvent(config.getName(), Type.ACTIVATE));
			return true;
		} catch (Exception e) {
			log.error("Database creation failed", e);
			return false;
		}
	}

	public static void open() {
		DatabaseWizard wizard = new DatabaseWizard();
		WizardDialog dialog = new WizardDialog(UI.shell(), wizard);
		dialog.open();
	}

	private class Runner implements IRunnableWithProgress {

		private IDatabaseConfiguration config;
		private DbTemplate content;

		Runner(IDatabaseConfiguration config) {
			this.config = config;
		}

		Runner(IDatabaseConfiguration config, DbTemplate content) {
			this(config);
			this.content = content;
		}

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			monitor.beginTask(Messages.CreateDatabase,
					IProgressMonitor.UNKNOWN);
			try {
				Database.close();
				if (config instanceof MySQLConfiguration)
					Database.register((MySQLConfiguration) config);
				else if (config instanceof DerbyConfiguration) {
					Database.register((DerbyConfiguration) config);
					extractDerbyTemplate();
				}
				App.getEventBus().post(new DatabaseEvent(config.getName(), Type.CREATE));
				Database.activate(config);
				App.getEventBus().post(new DatabaseEvent(config.getName(), Type.ACTIVATE));
			} catch (Exception e) {
				log.error("Create database failed", e);
			}
			Navigator.refresh();
			monitor.done();
		}

		private void extractDerbyTemplate() {
			File dir = DatabaseDir.getRootFolder(config.getName());
			if (dir.exists()) {
				log.error("could not create database {}; folder with name "
						+ "already exists", config);
				return;
			}
			try {
				content.extract(dir);
			} catch (Exception e) {
				log.error("failed to extract database template", e);
			}
		}

	}

}
