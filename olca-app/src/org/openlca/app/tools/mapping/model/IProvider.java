package org.openlca.app.tools.mapping.model;

import java.util.List;

import org.openlca.core.database.IDatabase;
import org.openlca.io.maps.FlowMap;
import org.openlca.io.maps.FlowRef;

/**
 * Provides flow data from an underlying data source and implements
 * functionality for synchronizing them with a database.
 */
public interface IProvider {

	/**
	 * Get a list of all flow references from the underlying data source.
	 */
	List<FlowRef> getFlowRefs();

	/**
	 * Synchronizes the given flow references with the database.
	 */
	public void persist(List<FlowRef> refs, IDatabase db);

	/**
	 * Synchronize the source flows of the given mapping with this provider.
	 */
	public void syncSourceFlows(FlowMap fm);

	/**
	 * Synchronize the target flows of the given mapping with this provider.
	 */
	public void syncTargetFlows(FlowMap fm);
}