/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria.internal.hhh11877;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Foo {

	private long id;
	private boolean bar;

	@Column(nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Id
	public long getId() {
		return this.id;
	}
	public void setId(final long id) {
		this.id = id;
	}

	public boolean isBar() {
		return this.bar;
	}
	public void setBar(final boolean bar) {
		this.bar = bar;
	}
}
