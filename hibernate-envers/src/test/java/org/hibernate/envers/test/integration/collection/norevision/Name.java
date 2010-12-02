package org.hibernate.envers.test.integration.collection.norevision;

import org.hibernate.envers.Audited;

import java.io.Serializable;

@Audited
public class Name implements Serializable {

    private Integer id;

    private String name;

    private Person person;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }
}
