/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade.circle;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "t_vehicles")
public class Vehicle {
	@Id
	@GeneratedValue(generator = "increment")
	private Integer vehicleID;

	@Version
	@Column(name = "vers")
	private long version;

	@Basic(optional = false)
	private String name;

	@ManyToOne(optional = false)
	@JoinColumn(name = "route_fk")
	private Route route;

	@OneToMany(mappedBy = "vehicle", cascade = {CascadeType.MERGE,CascadeType.REFRESH})
	private Set<Transport> transports = new HashSet<>();

	@Transient
	private String transientField = "vehicle original value";

	protected void setVehicleID(Integer vehicleID) {
		this.vehicleID = vehicleID;
	}

	public Integer getVehicleID() {
		return vehicleID;
	}

	public long getVersion() {
		return version;
	}

	protected void setVersion(long version) {
		this.version = version;
	}

	public Set<Transport> getTransports() {
		return transports;
	}

	public void setTransports(Set<Transport> transports) {
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
