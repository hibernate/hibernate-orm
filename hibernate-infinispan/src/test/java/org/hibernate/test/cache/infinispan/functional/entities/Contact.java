/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional.entities;
import java.io.Serializable;

/**
 * Entity that has a many-to-one relationship to a Customer
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class Contact implements Serializable {
	Integer id;
	String name;
	String tlf;
	Customer customer;
	// mapping added programmatically
	long version;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTlf() {
		return tlf;
	}

	public void setTlf(String tlf) {
		this.tlf = tlf;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Contact))
			return false;
		Contact c = (Contact) o;
		return c.id.equals(id) && c.name.equals(name) && c.tlf.equals(tlf);
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + (id == null ? 0 : id.hashCode());
		result = 31 * result + name.hashCode();
		result = 31 * result + tlf.hashCode();
		return result;
	}

}
