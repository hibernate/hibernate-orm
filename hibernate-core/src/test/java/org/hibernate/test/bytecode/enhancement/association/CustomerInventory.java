/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.association;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */

import java.util.Comparator;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

@Entity
public class CustomerInventory implements Comparator<CustomerInventory> {

	@Id
	private Long id;

	@Id
	private int custId;

	@ManyToOne(cascade = CascadeType.MERGE)
	private Customer customer;

	@ManyToOne(cascade = CascadeType.MERGE)
	private String vehicle;

	@Version
	private int version;

	public CustomerInventory() {
	}

	CustomerInventory(Customer customer, String vehicle) {
		this.customer = customer;
		this.vehicle = vehicle;
		;
	}

	public String getVehicle() {
		return vehicle;
	}

	public Long getId() {
		return id;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public int getCustId() {
		return custId;
	}

	public int getVersion() {
		return version;
	}

	public int compare(CustomerInventory cdb1, CustomerInventory cdb2) {
		return cdb1.id.compareTo( cdb2.id );
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == this ) {
			return true;
		}
		if ( obj == null || !( obj instanceof CustomerInventory ) ) {
			return false;
		}
		if ( this.id == ( (CustomerInventory) obj ).id ) {
			return true;
		}
		if ( this.id != null && ( (CustomerInventory) obj ).id == null ) {
			return false;
		}
		if ( this.id == null && ( (CustomerInventory) obj ).id != null ) {
			return false;
		}

		return this.id.equals( ( (CustomerInventory) obj ).id );
	}

	@Override
	public int hashCode() {
		int result = id.hashCode();
		result = 31 * result + custId;
		return result;
	}

}
