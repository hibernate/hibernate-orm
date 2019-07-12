/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria;

import java.util.HashSet;
import java.util.Set;

public class OrderAddress {

	private int orderAddressId;

	public int getOrderAddressId() {
		return orderAddressId;
	}

	private Address deliveryAddress;

	public Address getDeliveryAddress() {
		return deliveryAddress;
	}

	public void setDeliveryAddress(Address deliveryAddress) {
		this.deliveryAddress = deliveryAddress;
	}

	private Set<Address> notifiedAddresses = new HashSet<Address>();

	public Set<Address> getNotifiedAddresses() {
    	return notifiedAddresses;
  	}

	public void setNotifiedAddresses(Set<Address> notifiedAddresses) {
		this.notifiedAddresses = notifiedAddresses;
	}

	public String toString() {
    return "" + orderAddressId + " - " + getDeliveryAddress() + " - " + getNotifiedAddresses();
  }
}
