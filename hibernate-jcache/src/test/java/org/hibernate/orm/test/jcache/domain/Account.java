/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jcache.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ACCOUNT")
public class Account {

	@Id
	@GeneratedValue
	@Column(name = "ACCOUNT_ID")
	private Long id;

	@OneToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST }, optional = false)
	@JoinColumn(name = "person_id", unique = true, nullable = false)
	private Person person;

	public Account() {
		//
	}

	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String toString() {
		return super.toString();
	}
}
