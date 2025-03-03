/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * @author Florian Rampp
 * @author Steve Ebersole
 */
@Entity
@Table( name = "CHILD")
public class Child {

	@Id
	// A @OneToOne here results in the following DDL: create table child ([...] primary key
	// (parent), unique (parent)).
	// Oracle does not like a unique constraint and a PK on the same column (results in ORA-02261)
	@OneToOne(optional = false)
	private Parent parent;

	public void setParent(Parent parent) {
		this.parent = parent;
	}

}
