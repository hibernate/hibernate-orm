/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.reattachment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Child entity
 *
 * @author Steve Ebersole
 */
@Entity
public class Child {
	@Id
	@Column(name = "NAME")
	private String name;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PARENT")
	private Parent parent;

	public Child() {
	}

	public Child(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Parent getParent() {
		return parent;
	}

	public void setParent(Parent parent) {
		this.parent = parent;
	}
}
