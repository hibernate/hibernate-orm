/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade.circle;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Node {
	private Integer nodeID;
	private long version;
	private String name;
	private Route route = null;
	private Set<Transport> deliveryTransports = new HashSet<>();
	private Set<Transport> pickupTransports = new HashSet<>();
	private Tour tour;

	private String transientField = "node original value";

	public Set<Transport> getDeliveryTransports() {
		return deliveryTransports;
	}

	public void setDeliveryTransports(Set<Transport> deliveryTransports) {
		this.deliveryTransports = deliveryTransports;
	}

	public Set<Transport> getPickupTransports() {
		return pickupTransports;
	}

	public void setPickupTransports(Set<Transport> pickupTransports) {
		this.pickupTransports = pickupTransports;
	}

	public Integer getNodeID() {
		return nodeID;
	}

	public long getVersion() {
		return version;
	}

	protected void setVersion(long version) {
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Route getRoute() {
		return route;
	}

	public void setRoute(Route route) {
		this.route = route;
	}

	public Tour getTour() {
		return tour;
	}

	public void setTour(Tour tour) {
		this.tour = tour;
	}

	public String toString()
	{
		StringBuilder buffer = new StringBuilder();

		buffer.append( name + " id: " + nodeID );
		if ( route != null ) {
			buffer.append( " route name: " )
					.append( route.getName() )
					.append( " tour name: " )
					.append( ( tour == null ? "null" : tour.getName() ) );
		}
		if ( pickupTransports != null ) {
			for (Iterator it = pickupTransports.iterator(); it.hasNext();) {
				buffer.append("Pickup transports: " + it.next());
			}
		}

		if ( deliveryTransports != null ) {
			for (Iterator it = deliveryTransports.iterator(); it.hasNext();) {
				buffer.append("Delviery transports: " + it.next());
			}
		}

		return buffer.toString();
	}

	public String getTransientField() {
		return transientField;
	}

	public void setTransientField(String transientField) {
		this.transientField = transientField;
	}

	protected void setNodeID(Integer nodeID) {
		this.nodeID = nodeID;
	}

}
