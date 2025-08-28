/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.xml;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Employee {
	@Id
	Long id;
	String name;
/*
	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name = "street", column = @Column(name = "HA_street")),
		@AttributeOverride(name = "city", column = @Column(name = "HA_city")),
		@AttributeOverride(name = "state", column = @Column(name = "HA_state")),
		@AttributeOverride(name = "zip", column = @Column(name = "HA_zip")) })
*/
	Address homeAddress;

/*
	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name = "street", column = @Column(name = "MA_street")),
		@AttributeOverride(name = "city", column = @Column(name = "MA_city")),
		@AttributeOverride(name = "state", column = @Column(name = "MA_state")),
		@AttributeOverride(name = "zip", column = @Column(name = "MA_zip")) })
*/
	Address mailAddress;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Address getHomeAddress() {
		return homeAddress;
	}

	public void setHomeAddress(Address homeAddress) {
		this.homeAddress = homeAddress;
	}

	public Address getMailAddress() {
		return mailAddress;
	}

	public void setMailAddress(Address mailAddress) {
		this.mailAddress = mailAddress;
	}
}
