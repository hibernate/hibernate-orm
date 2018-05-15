/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.functional;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Table(name = "contacts")
public class Contact implements Serializable {
	@Id
	@Column(name = "id")
	public Long id;

	@Column(name = "type")
	public String type;

	@Column(name = "firstname")
	public String firstName;

	@Column(name = "lastname")
	public String lastName;

	@ManyToOne
	@JoinColumn(name = "folder_id")
	public Folder folder;

	public Contact() {
	}

	public Contact(Long id, String firstName, String lastName, String type, Folder folder) {
		this.firstName = firstName;
		this.folder = folder;
		this.id = id;
		this.lastName = lastName;
		this.type = type;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( ! ( o instanceof Contact ) ) return false;

		Contact contact = (Contact) o;

		if ( id != null ? !id.equals( contact.id ) : contact.id != null ) return false;
		if ( firstName != null ? !firstName.equals( contact.firstName ) : contact.firstName != null ) return false;
		if ( lastName != null ? !lastName.equals( contact.lastName ) : contact.lastName != null ) return false;
		if ( type != null ? !type.equals( contact.type ) : contact.type != null ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + ( type != null ? type.hashCode() : 0 );
		result = 31 * result + ( firstName != null ? firstName.hashCode() : 0 );
		result = 31 * result + ( lastName != null ? lastName.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "Contact(id = " + id + ", type = " + type + ", firstName = " + firstName + ", lastName = " + lastName + ")";
	}
}
