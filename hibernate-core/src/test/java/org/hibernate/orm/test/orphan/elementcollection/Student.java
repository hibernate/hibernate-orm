/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.orphan.elementcollection;

import java.util.Set;

public class Student {

	private String id;
	private String firstName;
	private String lastName;
	private Set< StudentEnrolledClass > enrolledClasses;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Set<StudentEnrolledClass> getEnrolledClasses() {
		return enrolledClasses;
	}

	public void setEnrolledClasses(Set<StudentEnrolledClass> enrolledClasses) {
		this.enrolledClasses = enrolledClasses;
	}
}
