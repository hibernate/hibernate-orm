/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.proxy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "person")
@IdClass(PersonId.class)
public abstract class Person {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private int id;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "clientId")
	private int clientId;

	@Column(name = "name")
	private String name;

	@Column(name = "title")
	private String title;

	public Person() {
		someInitMethod();
	}

	public void someInitMethod() {
	}

	public int getId() {
		return id;
	}

	public int getClientId() {
		return clientId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTitle(String name) {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
