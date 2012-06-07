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

public class Node {

//	@Id
//	@SequenceGenerator(name="NODE_SEQ", sequenceName="NODE_SEQ", initialValue=1, allocationSize=1)
//	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="NODE_SEQ")
	private Long nodeID;

	private long version;
	
	private String name;
	
	/** the list of orders that are delivered at this node */
//	@OneToMany(fetch=FetchType.LAZY, cascade={CascadeType.MERGE, CascadeType.REFRESH}, mappedBy="deliveryNode")
	private Set deliveryTransports = new HashSet();
	
	/** the list of orders that are picked up at this node */
//	@OneToMany(fetch=FetchType.LAZY, cascade=CascadeType.ALL, mappedBy="pickupNode")
	private Set pickupTransports = new HashSet();
	
	/** the route to which this node belongs */
//	@ManyToOne(targetEntity=Route.class, optional=false, fetch=FetchType.EAGER)
//	@JoinColumn(name="ROUTEID", nullable=false, insertable=true, updatable=true)
	private Route route = null;
	
	/** the tour this node belongs to, null if this node does not belong to a tour (e.g first node of a route) */
//	@ManyToOne(targetEntity=Tour.class, cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, optional=true, fetch=FetchType.LAZY)
//	@JoinColumn(name="TOURID", nullable=true, insertable=true, updatable=true)
	private Tour tour;
	
//	@Transient
	private String transientField = "node original value";

	public Set getDeliveryTransports() {
		return deliveryTransports;
	}

	public void setDeliveryTransports(Set deliveryTransports) {
		this.deliveryTransports = deliveryTransports;
	}

	public Set getPickupTransports() {
		return pickupTransports;
	}

	public void setPickupTransports(Set pickupTransports) {
		this.pickupTransports = pickupTransports;
	}

	public Long getNodeID() {
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

	protected void setNodeID(Long nodeID) {
		this.nodeID = nodeID;
	}

}
