/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.basic;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

/**
 * @author Florian Rampp
 * @author Steve Ebersole
 */
@Entity
public class Parent {

	@Id
	Long id;

	@OneToOne(cascade = CascadeType.ALL, mappedBy = "parent")
	Child child;

	void setChild(Child child) {
		this.child = child;
		child.setParent(this);
	}

}
