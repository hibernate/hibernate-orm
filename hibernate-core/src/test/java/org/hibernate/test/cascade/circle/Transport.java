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



public class Transport {
	
//	@Id
//	@SequenceGenerator(name="TRANSPORT_SEQ", sequenceName="TRANSPORT_SEQ", initialValue=1, allocationSize=1)
//	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="TRANSPORT_SEQ")
	private Long transportID;

	private long version;

	private String name;
	
	/** node value object at which the order is picked up */
//	@ManyToOne(optional=false, cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, fetch=FetchType.EAGER)
//	@JoinColumn(name="PICKUPNODEID", /*nullable=false,*/insertable=true, updatable=true)
	private Node pickupNode = null;

	/** node value object at which the order is delivered */
//	@ManyToOne(optional=false, cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, fetch=FetchType.EAGER)
//	@JoinColumn(name="DELIVERYNODEID", /*nullable=false,*/ insertable=true, updatable=true)
	private Node deliveryNode = null;

	private Vehicle vehicle;
	
//	@Transient
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

	protected void setTransportID(Long transportID) {
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

	public Long getTransportID() {
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
