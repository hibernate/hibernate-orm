/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.naturalid;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;

/**
 * @author Emmanuel Bernard
 */
@Entity
@NaturalIdCache
public class Citizen {
	@Id
	private Integer id;
	private String firstname;
	private String lastname;
	@NaturalId
	@ManyToOne
	private State state;
	@NaturalId
	private String ssn;

	public Citizen() {
	}

	public Citizen(Integer id, String firstname, String lastname, State state, String ssn) {
		this.id = id;
		this.firstname = firstname;
		this.lastname = lastname;
		this.state = state;
		this.ssn = ssn;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public String getSsn() {
		return ssn;
	}

	public void setSsn(String ssn) {
		this.ssn = ssn;
	}
}
