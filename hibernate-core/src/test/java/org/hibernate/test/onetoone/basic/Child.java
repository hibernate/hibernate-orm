/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetoone.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
