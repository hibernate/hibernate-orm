/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.loadplans.process.inheritance;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 * @author Steve Ebersole
 */
@Entity
public class Employee extends User {

	// illustrates the problematic situation described in HHH-8980

	@ManyToOne(optional = false)
	Department belongsTo;

	public Employee(Integer id, Department belongsTo) {
		super( id );
		this.belongsTo = belongsTo;
	}

	public Employee() {
	}
}
