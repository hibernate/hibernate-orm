/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Inheritance( strategy = InheritanceType.JOINED )
@Table( name = "PERSON_Orderby" )
public class Person implements Serializable {

	private Long idPerson;
	private String firstName, lastName;

	public Person() {
	}

	public void setIdPerson(Long idPerson) {
		this.idPerson = idPerson;
	}

	@Id
	@Column( name = "id_person", nullable = false )
	public Long getIdPerson() {
		return idPerson;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@Column( name = "first_name", length = 40, nullable = false )
	public String getFirstName() {
		return firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Column( name = "last_name", length = 40, nullable = false )
	public String getLastName() {
		return lastName;
	}

}
