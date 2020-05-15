/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetoone.hhh9798;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
public class Shipment {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	protected Long id;

	@NotNull
	protected Date createdOn = new Date();

	@NotNull
	protected ShipmentState shipmentState = ShipmentState.TRANSIT;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinTable(
			name = "ITEM_SHIPMENT",
			joinColumns =
			@JoinColumn(name = "SHIPMENT_ID"),
			inverseJoinColumns =
			@JoinColumn(name = "ITEM_ID",
					nullable = false,
					unique = true)
	)
	protected Item auction;

	public Shipment() {
	}

	public Shipment(Item auction) {
		this.auction = auction;
	}

	public Long getId() {
		return id;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	public ShipmentState getShipmentState() {
		return shipmentState;
	}

	public void setShipmentState(ShipmentState shipmentState) {
		this.shipmentState = shipmentState;
	}

	public Item getAuction() {
		return auction;
	}

	public void setAuction(Item auction) {
		this.auction = auction;
	}
}
