/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.naturalid;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class Account implements Serializable {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	@ManyToOne
	@JoinColumns({
			@JoinColumn(name = "customer_customernumber", referencedColumnName = "customerNumber"),
			@JoinColumn(name = "customer_customername", referencedColumnName = "name")
	})
	private Customer customer;

	Account() {

	}

	public Account(Integer id, Customer customer) {
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
		if ( !(obj instanceof Account)) {
			return false;
		}
		Account that = (Account) obj;
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Account{" +
				"id=" + id +
				'}';
	}
}
