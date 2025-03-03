/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.NaturalId;

/**
 * @author Guenther Demetz
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"year", "month"})})
// Remark: Without line above, hibernate creates the combined unique index as follows: unique (month, year)
// It seems that hibernate orders the attributes in alphabetic order
// We indeed want to have the inverted sequence: year, month
// In this way queries with only year in the where-clause can take advantage of this index
// N.B.: Usually a user defines a combined index beginning with the most discriminating property
public class Month  {

	@Id @GeneratedValue
	private int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@NaturalId
	private int year;

	@NaturalId
	private int month;

}
