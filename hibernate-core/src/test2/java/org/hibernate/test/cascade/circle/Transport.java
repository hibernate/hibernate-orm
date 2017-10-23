/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $


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
