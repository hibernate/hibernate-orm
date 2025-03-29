/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade.circle;

import java.util.HashSet;
import java.util.Set;


public class Vehicle {

//	@Id
//	@SequenceGenerator(name="TRANSPORT_SEQ", sequenceName="TRANSPORT_SEQ", initialValue=1, allocationSize=1)
//	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="TRANSPORT_SEQ")
	private Long vehicleID;

	private long version;

	private String name;

	private Set transports = new HashSet();

	private Route route;

	private String transientField = "vehicle original value";

	protected void setVehicleID(Long vehicleID) {
		this.vehicleID = vehicleID;
	}

	public Long getVehicleID() {
		return vehicleID;
	}

	public long getVersion() {
		return version;
	}

	protected void setVersion(long version) {
		this.version = version;
	}

	public Set getTransports() {
		return transports;
	}

	public void setTransports(Set transports) {
		this.transports = transports;
	}

	public Route getRoute() {
		return route;
	}

	public void setRoute(Route route) {
		this.route = route;
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

		buffer.append(name + " id: " + vehicleID + "\n");

		return buffer.toString();
	}

	public String getTransientField() {
		return transientField;
	}

	public void setTransientField(String transientField) {
		this.transientField = transientField;
	}
}
