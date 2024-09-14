/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.embeddables.collection.xml;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * <code>Person</code> -
 *
 * @author Vlad Mihalcea
 */
public class Person implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long id;

	private int version;

	private List<ContactInformation> contacts;

	public Long getId() {
		return this.id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public int getVersion() {
		return this.version;
	}

	public void setVersion(final int version) {
		this.version = version;
	}

	@Override
	public String toString() {
		String result = getClass().getSimpleName() + " ";
		if ( id != null ) {
			result += "id: " + id;
		}
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Person person = (Person) o;
		return version == person.version && Objects.equals( id, person.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, version, contacts );
	}

	public void setContacts(List<ContactInformation> contacts) {
		this.contacts = contacts;
	}

	public List<ContactInformation> getContacts() {
		return contacts;
	}


}
