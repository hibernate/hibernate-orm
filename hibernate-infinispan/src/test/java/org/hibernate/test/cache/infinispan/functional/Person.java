package org.hibernate.test.cache.infinispan.functional;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import java.io.Serializable;

/**
 * Test class using EmbeddedId
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
@Entity
public class Person implements Serializable {
    @EmbeddedId
    Name name;

    int age;

    public Person() {}

    public Person(String firstName, String lastName, int age) {
        name = new Name(firstName, lastName);
        this.age = age;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
