/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade;

import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

/**
 * @author Jeff Schnitzer
 */
@Entity
public class Parent
{
	/** */
	@Id
	@GeneratedValue
	public Long id;

	/** */
	@OneToMany(cascade=CascadeType.ALL, mappedBy="parent")
	public Set<Child> children;

	/** */
	@OneToOne(cascade=CascadeType.ALL)
	@JoinColumn(name="defaultChildId", nullable=false)
	Child defaultChild;

	/** */
	public Parent() {}

	/** */
	public Child getDefaultChild() { return this.defaultChild; }
	public void setDefaultChild(Child value) { this.defaultChild = value; }

	/** */
	public Set<Child> getChildren() { return this.children; }
	public void setChildren(Set<Child> value) { this.children = value; }
}
