/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand;


import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.id.IncrementGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Gavin King
 */
@Entity
@Table(name = "PERSON")
@SQLInsert(sql = "INSERT INTO PERSON (NAME, PERID) VALUES ( UPPER(? || ''), ? )")
@SQLUpdate(sql = "UPDATE PERSON SET NAME=UPPER(? || '') WHERE PERID=?")
@SQLDelete(sql = "DELETE FROM PERSON WHERE PERID=?")
@SQLSelect(sql = "SELECT PERID, NAME FROM PERSON WHERE PERID=?")
public class Person {
	@Id
	@GenericGenerator(type = IncrementGenerator.class)
	@Column(name = "PERID")
	private Long id;

	@Column(name = "NAME", nullable = false)
	private String name;

	public Person(String name) {
		this.name = name;
	}

	public Person() {
	}

	/**
	 * @return Returns the id.
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id The id to set.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
}
