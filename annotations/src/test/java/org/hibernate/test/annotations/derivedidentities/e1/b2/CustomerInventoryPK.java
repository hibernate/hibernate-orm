package org.hibernate.test.annotations.derivedidentities.e1.b2;

import java.io.Serializable;


public class CustomerInventoryPK implements Serializable {

	private Integer id;

	private Customer customer;

	public CustomerInventoryPK() {
	}

	public CustomerInventoryPK(Integer id, Customer customer) {
		this.id = id;
		this.customer = customer;
	}

	public boolean equals(Object other) {
		if ( other == this ) {
			return true;
		}
		if ( other == null || getClass() != other.getClass() ) {
			return false;
		}
		CustomerInventoryPK cip = ( CustomerInventoryPK ) other;
		return ( getCustomer().getId() == cip.getCustomer().getId() && ( id == cip.id ||
				( id != null && id.equals( cip.id ) ) ) );
	}

	public int hashCode() {
		return ( id == null ? 0 : id.hashCode() ) ^ getCustomer().getId();
	}

	public Integer getId() {
		return id;
	}

	public Customer getCustomer() {
		return customer;
	}


}
