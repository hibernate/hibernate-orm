/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.guide.association;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import javax.persistence.*;

import static org.junit.Assert.assertNotNull;

import static org.hibernate.jpa.test.util.TransactionUtil.*;

/**
 * @author Vlad Mihalcea
 */
public class DerivedIdentifierTest extends BaseEntityManagerFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
                Person.class,
                PersonDetails.class
        };
    }

    @Test
    public void testLifecycle() {
        Long personId = doInJPA(this::entityManagerFactory, entityManager -> {
            Person person = new Person("ABC-123");

            PersonDetails details = new PersonDetails();
            details.setPerson(person);

            entityManager.persist(person);
            entityManager.persist(details);

            return person.getId();
        });

        doInJPA(this::entityManagerFactory, entityManager -> {
            PersonDetails details = entityManager.find(PersonDetails.class, personId);
            assertNotNull(details);
        });
    }

    @Entity(name = "Person")
    public static class Person  {

        @Id
        @GeneratedValue
        private Long id;

        @NaturalId
        private String registrationNumber;

        public Person() {}

        public Person(String registrationNumber) {
            this.registrationNumber = registrationNumber;
        }

        public Long getId() {
            return id;
        }

        public String getRegistrationNumber() {
            return registrationNumber;
        }
    }

    @Entity(name = "PersonDetails")
    public static class PersonDetails  {

        @Id
        private Long id;

        private String nickName;

        @ManyToOne
        @MapsId
        private Person person;

        public String getNickName() {
            return nickName;
        }

        public void setNickName(String nickName) {
            this.nickName = nickName;
        }

        public Person getPerson() {
            return person;
        }

        public void setPerson(Person person) {
            this.person = person;
        }
    }
}
