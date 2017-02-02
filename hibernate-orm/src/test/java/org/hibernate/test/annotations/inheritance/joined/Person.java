/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.annotations.inheritance.joined;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "PERSON")
public class Person {

	@Id
	@GeneratedValue
	private int id;

	private String name;

	private String firtsname;

	public Person() {

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFirstname() {
		return firtsname;
	}

	public void setFirstname(String firstname) {
		this.firtsname = firstname;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

}

