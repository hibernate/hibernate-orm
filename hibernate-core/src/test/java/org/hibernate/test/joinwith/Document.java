
package org.hibernate.test.joinwith;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;

@Entity
public class Document implements Serializable {
    
    private Integer id;
    private Map<Integer, Person> contacts = new HashMap<Integer, Person>();

    @Id
    @GeneratedValue
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @OneToMany
    @CollectionTable
    @MapKeyColumn(name = "position")
    public Map<Integer, Person> getContacts() {
        return contacts;
    }

    public void setContacts(Map<Integer, Person> contacts) {
        this.contacts = contacts;
    }
}
