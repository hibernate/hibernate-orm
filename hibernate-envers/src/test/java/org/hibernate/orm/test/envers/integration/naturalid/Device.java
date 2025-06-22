/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.naturalid;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class Device implements Serializable {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	@ManyToOne
	@JoinColumn(name = "customer_id", foreignKey = @ForeignKey(name = "fk_dev_cust_id"))
	private Customer customer;

	Device() {

	}

	public Device(Integer id, Customer customer) {
		this.id = id;
		this.customer = customer;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	@Override
	public int hashCode() {
		int result;
		result = ( id != null ? id.hashCode() : 0 );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == this ) {
			return true;
		}
		if ( !( obj instanceof Device ) ) {
			return false;
		}
		Device that = (Device) obj;
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Device{" +
				"id=" + id +
				'}';
	}
}
