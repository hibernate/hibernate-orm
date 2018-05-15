/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.orphan.one2one.pk.unidirectional;


/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class Employee {
	private Long id;
	private EmployeeInfo info;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public EmployeeInfo getInfo() {
		return info;
	}

	public void setInfo(EmployeeInfo info) {
		this.info = info;
	}
}
