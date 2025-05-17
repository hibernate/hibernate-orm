/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade.circle;


public class Transport {
	private Integer transportID;
	private long version;
	private String name;
	private Node pickupNode;
	private Node deliveryNode;
	private Vehicle vehicle;

	private String transientField = "transport original value";

	public Node getDeliveryNode() {
		return deliveryNode;
	}

	public void setDeliveryNode(Node deliveryNode) {
		this.deliveryNode = deliveryNode;
	}

	public Node getPickupNode() {
		return pickupNode;
	}

	protected void setTransportID(Integer transportID) {
		this.transportID = transportID;
	}

	public void setPickupNode(Node pickupNode) {
		this.pickupNode = pickupNode;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	public Integer getTransportID() {
		return transportID;
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

	public String toString()
	{
		StringBuilder buffer = new StringBuilder();

		buffer.append(name + " id: " + transportID + "\n");

		return buffer.toString();
	}

	public String getTransientField() {
		return transientField;
	}

	public void setTransientField(String transientField) {
		this.transientField = transientField;
	}
}
