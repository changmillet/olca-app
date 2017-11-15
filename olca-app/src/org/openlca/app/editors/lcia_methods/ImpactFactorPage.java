package org.openlca.app.editors.lcia_methods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Event;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentDialog;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.ISelectionChangedListener;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.util.Strings;

import com.google.common.eventbus.Subscribe;

class ImpactFactorPage extends ModelPage<ImpactMethod> {

	private ImpactMethodEditor editor;
	private FormToolkit toolkit;
	private ImpactFactorTable factorTable;
	private ImpactCategoryViewer categoryViewer;
	private ScrolledForm form;
	private ImageHyperlink commentControl;

	ImpactFactorPage(ImpactMethodEditor editor) {
		super(editor, "ImpactFactorsPage", M.ImpactFactors);
		this.editor = editor;
		editor.onSaved(() -> onSaved());
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(this);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Section section = UI.section(body, toolkit, M.ImpactFactors);
		UI.gridData(section, true, true);
		Composite client = toolkit.createComposite(section);
		section.setClient(client);
		UI.gridLayout(client, 1);
		createCategoryViewer(client);
		factorTable = new ImpactFactorTable(editor);
		factorTable.render(client, section);
		categoryViewer.selectFirst();
		form.reflow(true);
	}

	private void onSaved() {
		if (categoryViewer == null || factorTable == null)
			return;
		ImpactCategoryDescriptor descriptor = categoryViewer.getSelected();
		if (descriptor == null)
			return;
		categoryViewer.setInput(getDescriptorList());
		categoryViewer.select(descriptor);
		for (ImpactCategory cat : editor.getModel().impactCategories) {
			if (equal(descriptor, cat)) {
				categoryViewer.select(Descriptors.toDescriptor(cat));
				break;
			}
		}
	}

	private void createCategoryViewer(Composite client) {
		Composite container = toolkit.createComposite(client);
		UI.gridLayout(container, Database.isConnected() ? 3 : 2, 10, 0);
		UI.gridData(container, true, false);
		new Label(container, SWT.NONE).setText(M.ImpactCategory);
		categoryViewer = new ImpactCategoryViewer(container);
		CategoryChange categoryChange = new CategoryChange();
		categoryViewer.addSelectionChangedListener(categoryChange);
		categoryViewer.setInput(getDescriptorList());
		editor.getEventBus().register(categoryChange);
		commentControl = new ImageHyperlink(container, SWT.NONE);
		UI.gridData(commentControl, false, false).verticalAlignment = SWT.TOP;
		Controls.onClick(commentControl, (e) -> {
			ImpactCategoryDescriptor category = categoryViewer.getSelected();
			if (category == null)
				return;
			String path = CommentPaths.get(category) + ".impactFactors";
			if (!editor.getComments().has(path))
				return;
			new CommentDialog(path, editor.getComments()).open();
		});
		if (!Database.isConnected())
			return;
		commentControl.setImage(Icon.SHOW_COMMENTS.get());
		commentControl.setToolTipText("#Show comments");
	}

	private void updateCommentControl() {
		if (!Database.isConnected())
			return;
		ImpactCategoryDescriptor category = categoryViewer.getSelected();
		if (category == null) {
			commentControl.setVisible(false);
			return;
		}
		String path = CommentPaths.get(category) + ".impactFactors";
		commentControl.setVisible(editor.getComments().has(path));
	}

	private List<ImpactCategoryDescriptor> getDescriptorList() {
		List<ImpactCategoryDescriptor> list = new ArrayList<>();
		for (ImpactCategory category : getModel().impactCategories)
			list.add(Descriptors.toDescriptor(category));
		Collections.sort(list,
				(o1, o2) -> Strings.compare(o1.getName(), o2.getName()));
		return list;
	}

	private boolean equal(ImpactCategoryDescriptor descriptor,
			ImpactCategory category) {
		if (descriptor == null && category == null)
			return true;
		if (descriptor == null || category == null)
			return false;
		if (category.getId() != 0L && descriptor.getId() != 0L)
			return descriptor.getId() == category.getId();
		// new impact categories have an ID of 0. Thus, we take also other
		// attributes to check equality
		if (category.getRefId() != null && descriptor.getRefId() != null)
			return Objects.equals(category.getRefId(), descriptor.getRefId());
		return Objects.equals(category.getName(), descriptor.getName())
				&& Objects.equals(category.referenceUnit,
						descriptor.getReferenceUnit());
	}

	private class CategoryChange implements
			ISelectionChangedListener<ImpactCategoryDescriptor> {

		@Override
		public void selectionChanged(ImpactCategoryDescriptor selection) {
			if (selection == null) {
				factorTable.setImpactCategory(null, false);
				updateCommentControl();
				return;
			}
			for (ImpactCategory cat : getModel().impactCategories) {
				if (equal(selection, cat)) {
					factorTable.setImpactCategory(cat, true);
					break;
				}
			}
			updateCommentControl();
		}

		@Subscribe
		public void categoryChange(Event event) {
			if (!event.match(editor.IMPACT_CATEGORY_CHANGE))
				return;
			categoryViewer.setInput(getDescriptorList());
			factorTable.refresh();
		}
	}

}
