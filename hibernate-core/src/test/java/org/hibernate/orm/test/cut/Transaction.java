/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cut;


/**
 * @author Gavin King
 */
public class Transaction {

	private Long id;
	private String description;
	private MonetoryAmount value;
	private CompositeDateTime timestamp;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public MonetoryAmount getValue() {
		return value;
	}

	public void setValue(MonetoryAmount value) {
		this.value = value;
	}

	public CompositeDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(CompositeDateTime timestamp) {
		this.timestamp = timestamp;
	}

}
