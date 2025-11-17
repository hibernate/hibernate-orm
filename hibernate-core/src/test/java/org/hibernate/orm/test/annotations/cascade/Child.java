/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * @author Jeff Schnitzer
 */
@Entity
public class Child
{
	/** */
	@Id
	@GeneratedValue
	public Long id;

	/** */
	@ManyToOne(cascade=CascadeType.PERSIST)
	@JoinColumn(name="parentId", nullable=false)
	Parent parent;

	/** */
	public Child() {}

	/** */
	public Child(Parent p)
	{
		this.parent = p;
	}

	/** */
	public Parent getParent() { return this.parent; }
	public void setParent(Parent value) { this.parent = value; }
}
