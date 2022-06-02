package org.openlca.app.editors.graph.edit;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editpolicies.RootComponentEditPolicy;
import org.openlca.app.editors.graph.GraphConfig;
import org.openlca.app.editors.graph.GraphFile;
import org.openlca.app.editors.graph.layouts.TreeConnectionRouter;
import org.openlca.app.editors.graph.model.Graph;
import org.openlca.app.editors.graph.model.GraphFactory;
import org.openlca.jsonld.Json;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.gef.LayerConstants.CONNECTION_LAYER;

/**
 * EditPart for the GraphModel instance.
 * <p>
 * This edit part server as the main diagram container, the white area where
 * everything else is in. Also responsible for the container's layout (the way
 * the container rearranges is contents) and the container's capabilities (edit
 * policies).
 * </p>
 * <p>
 * This edit part must implement the PropertyChangeListener interface, so it can
 * be notified of property changes in the corresponding model element.
 * </p>
 */
public class GraphEditPart extends AbstractComponentEditPart<Graph> {

	/**
	 * Upon activation, attach to the GraphConfig element as a property change
	 * listener.
	 */
	@Override
	public void activate() {
		if (!isActive()) {
			super.activate();
			getModel().getConfig().addPropertyChangeListener(this);
		}
	}

	/**
	 * Upon deactivation, detach from the GraphConfig element as a property change
	 * listener.
	 */
	@Override
	public void deactivate() {
		if (isActive()) {
			super.deactivate();
			getModel().getConfig().removePropertyChangeListener(this);
		}
	}

	@Override
	protected void createEditPolicies() {
		// Disallows the removal of this edit part.
		installEditPolicy(EditPolicy.COMPONENT_ROLE,
			new RootComponentEditPolicy());
		installEditPolicy(EditPolicy.CONTAINER_ROLE,
			new GraphContainerEditPolicy());
		// Handles constraint changes (e.g. moving and/or resizing) of model
		// elements within the graph and creation of new model elements.
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new GraphXYLayoutEditPolicy());
	}

	@Override
	protected IFigure createFigure() {
		var f = new FreeformLayer() {

			@Override
			public void paint(Graphics g) {
				var theme = getModel().getConfig().getTheme();
				g.pushState();
				g.setBackgroundColor(theme.graphBackgroundColor());
				g.fillRectangle(new Rectangle(getLocation(), getSize()));
				g.popState();
				super.paint(g);
			}

		};

		f.setBorder(new MarginBorder(3));
		f.setLayoutManager(new FreeformLayout());
		return f;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (GraphConfig.CONFIG_PROP.equals(prop)) {
			refresh();
		}
		else super.propertyChange(evt);
	}

	@Override
	protected void refreshVisuals() {
		var cLayer = (ConnectionLayer) getLayer(CONNECTION_LAYER);
		var connectionRouter = getModel().getConfig().isRouted()
					? new TreeConnectionRouter()
					: ConnectionRouter.NULL;
		cLayer.setConnectionRouter(connectionRouter);
		super.refreshVisuals();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<NodeEditPart> getChildren() {
		return (List<NodeEditPart>) super.getChildren();
	}

}
