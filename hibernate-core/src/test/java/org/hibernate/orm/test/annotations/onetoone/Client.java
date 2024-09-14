/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.onetoone;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SecondaryTable;

/**
 * @author Emmanuel Bernard
 */
@Entity
@SecondaryTable(name="CLIENT_EXTENSION")
public class Client {

	private Integer id;
	private String name;
	private Address address;
	private Address secondaryAddress;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "ADDRESS_ID")
	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "SECONDARY_ADDRESS_ID", table="CLIENT_EXTENSION")
	public Address getSecondaryAddress() {
		return secondaryAddress;
	}

	public void setSecondaryAddress(Address secondaryAddress) {
		this.secondaryAddress = secondaryAddress;
	}

	@Id
	@GeneratedValue
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

}
