/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.multiplerelations;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class Address implements Serializable {
	@Id
	@GeneratedValue
	private long id;

	private String city;

	@ManyToMany(cascade = {CascadeType.PERSIST})
	private Set<Person> tenants = new HashSet<Person>();

	@ManyToOne
	@JoinColumn(nullable = false)
	Person landlord;

	public Address() {
	}

	public Address(String city) {
		this.city = city;
	}

	public Address(String city, long id) {
		this.id = id;
		this.city = city;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof Address) ) {
			return false;
		}

		Address address = (Address) o;

		if ( id != address.id ) {
			return false;
		}
		if ( city != null ? !city.equals( address.city ) : address.city != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = (int) (id ^ (id >>> 32));
		result = 31 * result + (city != null ? city.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Address(id = " + id + ", city = " + city + ")";
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public Set<Person> getTenants() {
		return tenants;
	}

	public void setTenants(Set<Person> tenants) {
		this.tenants = tenants;
	}

	public Person getLandlord() {
		return landlord;
	}

	public void setLandlord(Person landlord) {
		this.landlord = landlord;
	}
}
