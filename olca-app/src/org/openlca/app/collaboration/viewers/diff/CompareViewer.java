package org.openlca.app.collaboration.viewers.diff;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.viewers.trees.Trees;

public class CompareViewer extends DiffNodeViewer {

	public CompareViewer(Composite parent) {
		super(parent, false);
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		TreeViewer viewer = Trees.createViewer(parent);
		configureViewer(viewer, false);
		return viewer;
	}

}