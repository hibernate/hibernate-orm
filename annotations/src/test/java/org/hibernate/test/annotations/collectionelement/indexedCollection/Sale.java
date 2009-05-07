//$
package org.hibernate.test.annotations.collectionelement.indexedCollection;

import java.util.List;
import java.util.ArrayList;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Column;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.GenericGenerator;

/**
 * @author Emmanuel Bernard
 */
@Entity
@GenericGenerator(name="increment", strategy = "increment")
public class Sale {
	@Id @GeneratedValue private Integer id;

	@CollectionOfElements //TODO migrate to @ElementCollection, what about @CollectionId
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
