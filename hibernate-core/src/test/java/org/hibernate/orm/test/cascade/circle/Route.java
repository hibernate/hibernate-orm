/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade.circle;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Route {
	private Integer routeID;
	private long version;
	private String name;
	private Set<Node> nodes = new HashSet<>();
	private Set<Vehicle> vehicles = new HashSet<>();

	private String transientField = null;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Node> getNodes() {
		return nodes;
	}

	protected void setNodes(Set<Node> nodes) {
		this.nodes = nodes;
	}

	protected Set<Vehicle> getVehicles() {
		return vehicles;
	}

	protected void setVehicles(Set<Vehicle> vehicles) {
		this.vehicles = vehicles;
	}

	protected void setRouteID(Integer routeID) {
		this.routeID = routeID;
	}

	public Integer getRouteID() {
		return routeID;
	}

	public long getVersion() {
		return version;
	}

	protected void setVersion(long version) {
		this.version = version;
	}

	public String toString()
	{
		StringBuilder buffer = new StringBuilder();

		buffer.append("Route name: " + name + " id: " + routeID + " transientField: " + transientField + "\n");
		for (Iterator it = nodes.iterator(); it.hasNext();) {
			buffer.append("Node: " + it.next() );
		}

		for (Iterator it = vehicles.iterator(); it.hasNext();) {
			buffer.append("Vehicle: " + it.next() );
		}

		return buffer.toString();
	}

	public String getTransientField() {
		return transientField;
	}

	public void setTransientField(String transientField) {
		this.transientField = transientField;
	}
}
