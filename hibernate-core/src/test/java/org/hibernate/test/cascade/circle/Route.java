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
import java.util.Iterator;
import java.util.Set;


public class Route {
	
//	@Id
//	@SequenceGenerator(name="ROUTE_SEQ", sequenceName="ROUTE_SEQ", initialValue=1, allocationSize=1)
//	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="ROUTE_SEQ")
	private Long routeID;

	private long version;

	/** A List of nodes contained in this route. */
//	@OneToMany(targetEntity=Node.class, fetch=FetchType.EAGER, cascade=CascadeType.ALL, mappedBy="route")
	private Set nodes = new HashSet();

	private Set vehicles = new HashSet();

	private String name;
	
//	@Transient
	private String transientField = null;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	protected Set getNodes() {
		return nodes;
	}

	protected void setNodes(Set nodes) {
		this.nodes = nodes;
	}

	protected Set getVehicles() {
		return vehicles;
	}

	protected void setVehicles(Set vehicles) {
		this.vehicles = vehicles;
	}

	protected void setRouteID(Long routeID) {
		this.routeID = routeID;
	}

	public Long getRouteID() {
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
