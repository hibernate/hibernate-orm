/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.jpa.test.ops;
import java.io.Serializable;
import java.util.Collection;


/**
 * Employer in a employer-Employee relationship
 *
 * @author Emmanuel Bernard
 */

public class Employer implements Serializable {
	private Integer id;
	private Collection employees;


	public Collection getEmployees() {
		return employees;
	}


	public Integer getId() {
		return id;
	}

	public void setEmployees(Collection set) {
		employees = set;
	}

	public void setId(Integer integer) {
		id = integer;
	}
}
