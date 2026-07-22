/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetoone;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;

/**
 * @author Emmanuel Bernard
 * @author Gail Badner
 */
@Entity
public class Person {
	@Id
	private Integer id;

	@MapsId
	@PrimaryKeyJoinColumn
	@OneToOne(optional=true)
	private PersonAddress personAddress;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public PersonAddress getPersonAddress() {
		return personAddress;
	}

	public void setPersonAddress(PersonAddress personAddress) {
		this.personAddress = personAddress;
	}
}
