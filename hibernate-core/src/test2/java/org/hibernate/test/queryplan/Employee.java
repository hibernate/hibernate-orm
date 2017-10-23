/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.queryplan;
import java.util.Date;

/**
 * Leaf subclass
 *
 * @author Steve Ebersole
 */
public class Employee extends User {
	private Date hireDate;

	protected Employee() {
		super();
	}

	public Employee(String name, char sex, String username, Date hireDate) {
		super( name, sex, username );
		this.hireDate = hireDate;
	}

	public Date getHireDate() {
		return hireDate;
	}

	public void setHireDate(Date hireDate) {
		this.hireDate = hireDate;
	}
}
