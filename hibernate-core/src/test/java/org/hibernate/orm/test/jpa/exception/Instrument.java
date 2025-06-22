/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.exception;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;


/**
 * @author Hardy Ferentschik
 */
@Entity
public class Instrument {

	@Id
	@GeneratedValue
	private int id;

	private String name;

	private Type type;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@PrePersist
	public void prePersist() {
		throw new RuntimeException( "Instrument broken." );
	}

	public enum Type {
		WIND, STRINGS, PERCUSSION
	}
}
