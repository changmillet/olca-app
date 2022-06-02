package org.openlca.app.editors.graph.edit;

import java.beans.PropertyChangeEvent;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.openlca.app.editors.graph.GraphConfig;
import org.openlca.app.editors.graph.figures.GridPos;
import org.openlca.app.editors.graph.figures.IOPaneFigure;
import org.openlca.app.editors.graph.model.IOPane;
import org.openlca.app.editors.graph.model.GraphComponent;

import static org.openlca.app.editors.graph.requests.GraphRequestConstants.REQ_ADD_INPUT_EXCHANGE;
import static org.openlca.app.editors.graph.requests.GraphRequestConstants.REQ_ADD_OUTPUT_EXCHANGE;

public class IOPaneEditPart extends AbstractComponentEditPart<IOPane> {

	@Override
	protected IFigure createFigure() {
		var figure = new IOPaneFigure(getModel());
		addButtonActionListener(figure);
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.CONTAINER_ROLE,
			new IOPaneContainerEditPolicy());
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (GraphComponent.SIZE_PROP.equals(prop)
			|| GraphComponent.LOCATION_PROP.equals(prop)) {
			refreshVisuals();
		}
		else super.propertyChange(evt);
	}

	@Override
	public IFigure getContentPane() {
		return getFigure().getContentPane();
	}

	@Override
	protected void addChildVisual(EditPart childEditPart, int index) {
		IFigure child = ((GraphicalEditPart) childEditPart).getFigure();
		getContentPane().add(child, GridPos.fillTop(), index);
	}

	@Override
	public IOPaneFigure getFigure() {
		return (IOPaneFigure) super.getFigure();
	}

	@Override
	public NodeEditPart getParent() {
		return (NodeEditPart) super.getParent();
	}

	protected void addButtonActionListener(IOPaneFigure figure) {
		figure.addExchangeButton.addActionListener($ -> {
			var request = getModel().isForInputs()
				? new Request(REQ_ADD_INPUT_EXCHANGE)
				: new Request(REQ_ADD_OUTPUT_EXCHANGE);
			var command = getCommand(request);
			getViewer().getEditDomain().getCommandStack().execute(command);
		});
	}

}
