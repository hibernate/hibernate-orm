//$
package org.hibernate.test.annotations.collectionelement.indexedCollection;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;

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
    @CollectionTable(
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
