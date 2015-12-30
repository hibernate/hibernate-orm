/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.guide.collection;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.jboss.logging.Logger;
import org.junit.Test;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OrderColumn;
import java.util.ArrayList;
import java.util.List;

import static org.hibernate.jpa.test.util.TransactionUtil.*;

/**
 * @author Vlad Mihalcea
 */
public class BasicTypeOrderColumnElementCollectionTest extends BaseEntityManagerFunctionalTestCase {

    private static final Logger log = Logger.getLogger( BasicTypeOrderColumnElementCollectionTest.class );

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
            Person.class
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            Person person = new Person();
            person.id = 1L;
            person.getPhones().add("123-456-7890");
            person.getPhones().add("456-000-1234");
            entityManager.persist(person);
        });
        doInJPA(this::entityManagerFactory, entityManager -> {
            Person person = entityManager.find(Person.class, 1L);
            log.info("Remove one element");
            person.getPhones().remove(0);
        });
    }

    @Entity(name = "Person")
    public static class Person  {

        @Id
        private Long id;

        @ElementCollection
        @OrderColumn(name = "order_id")
        private List<String> phones = new ArrayList<>();

        public List<String> getPhones() {
            return phones;
        }
    }
}
