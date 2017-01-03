package org.hibernate.jpa.test.metamodel;

import javax.persistence.Entity;

@Entity
public class Person extends BaseEmbeddedEntity<PersonId, String, Person> {

    private String firstName;
    private String lastName;

    public Person() {
    }

    public Person(PersonId id, String firstName, String lastName) {
        super(id);
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
