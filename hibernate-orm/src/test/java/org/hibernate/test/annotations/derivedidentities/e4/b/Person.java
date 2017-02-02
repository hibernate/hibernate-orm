/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e4.b;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Person {
	@Id
	String ssn;

	public Person() {
	}

	public Person(String ssn) {
		this.ssn = ssn;
	}
}
