/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetoone.hhh9798;

import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;

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
