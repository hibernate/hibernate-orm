/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.query;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Employee {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Employeegroup employeegroup;

    @ManyToOne
    private Attrset attrset;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Employeegroup getEmployeegroup() {
		return employeegroup;
	}

	public void setEmployeegroup(Employeegroup employeegroup) {
		this.employeegroup = employeegroup;
	}

	public Attrset getAttrset() {
		return attrset;
	}

	public void setAttrset(Attrset attrset) {
		this.attrset = attrset;
	}

}
