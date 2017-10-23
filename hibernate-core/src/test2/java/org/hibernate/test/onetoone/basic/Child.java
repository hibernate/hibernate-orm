/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetoone.basic;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

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
