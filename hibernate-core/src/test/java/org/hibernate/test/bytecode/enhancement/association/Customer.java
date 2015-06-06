/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.association;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Version;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Entity
public class Customer {

	@Id
	private int id;

	@OneToOne
	private User user;

	private String firstName;

	private String lastName;

	@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private List<CustomerInventory> customerInventories;

	@Version
	private int version;

	public Customer() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer customerId) {
		this.id = customerId;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public List<CustomerInventory> getInventories() {
		if ( customerInventories == null ) {
			customerInventories = new ArrayList<CustomerInventory>();
		}
		return customerInventories;
	}

	public void setInventories (List<CustomerInventory> inventories) {
		this.customerInventories = inventories;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public void addInventory(CustomerInventory inventory) {
		List<CustomerInventory> list = getInventories();
		list.add( inventory );
		customerInventories = list;
	}

	public CustomerInventory addInventory(String item) {
		CustomerInventory inventory = new CustomerInventory( this, item );
		getInventories().add( inventory );
		return inventory;
	}

	public int getVersion() {
		return version;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		return id == ( (Customer) o ).id;
	}

	@Override
	public int hashCode() {
		return new Integer( id ).hashCode();
	}

	@Override
	public String toString() {
		return this.getFirstName() + " " + this.getLastName();
	}
}
