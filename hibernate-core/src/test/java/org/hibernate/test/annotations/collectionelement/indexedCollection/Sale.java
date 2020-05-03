/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$
package org.hibernate.test.annotations.collectionelement.indexedCollection;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;

import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

/**
 * @author Emmanuel Bernard
 */
@Entity
@GenericGenerator(name="increment", strategy = "increment")
public class Sale {
	@Id @GeneratedValue private Integer id;
	@ElementCollection
    @JoinTable(
        name = "contact",
        joinColumns = @JoinColumn(name = "n_key_person"))
    @CollectionId(
        columns = @Column(name = "n_key_contact"),
        type = @Type(type = "long"),
        generator = "increment" ) 
	private List<Contact> contacts = new ArrayList<Contact>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public List<Contact> getContacts() {
		return contacts;
	}

	public void setContacts(List<Contact> contacts) {
		this.contacts = contacts;
	}
}
