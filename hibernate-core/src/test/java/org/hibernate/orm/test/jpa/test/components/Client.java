/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.test.components;
import java.io.Serializable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * The client domain entity
 *
 */
@Entity
public class Client implements Serializable {
	private int id;
	private Name name;

	public Client() {
	}

	public Client(int id, Name name) {
		this.id = id;
		this.name = name;
	}

	public Client(int id, String firstName, String lastName) {
		this( id, new Name( firstName, lastName ) );
	}

	@Id
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Embedded
	public Name getName() {
		return name;
	}

	public void setName(Name name) {
		this.name = name;
	}
}
