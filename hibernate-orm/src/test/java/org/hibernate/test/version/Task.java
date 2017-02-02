/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Task.java 7676 2005-07-29 06:27:10Z oneovthafew $
package org.hibernate.test.version;


public class Task {
	private String description;
	private Person person;
	private int version;
	
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
	Task() {}
	public Task(String description, Person person) {
		this.description = description;
		this.person = person;
		person.getTasks().add(this);
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Person getPerson() {
		return person;
	}
	public void setPerson(Person person) {
		this.person = person;
	}
}
