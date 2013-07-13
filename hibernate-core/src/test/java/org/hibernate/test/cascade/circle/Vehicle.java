//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */

package org.hibernate.test.cascade.circle;
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
