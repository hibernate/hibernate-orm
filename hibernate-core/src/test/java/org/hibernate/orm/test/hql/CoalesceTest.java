/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hql;

import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.TypedQuery;


import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertThat;

/**
 * @author Johannes Buehler
 */
@JiraKey( value = "HHH-10463")
public class CoalesceTest extends BaseCoreFunctionalTestCase {

    private Person person;

    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[] {
                Person.class
        };
    }

    @Before
    public void setUp() {
        doInHibernate( this::sessionFactory, session -> {
            person = new Person();
            person.setName("Johannes");
            person.setSurname("Buehler");
            session.persist(person);
        } );
    }

    @Test
    public void HHH_10463_TestCoalesce() {
        doInHibernate( this::sessionFactory, session -> {
            TypedQuery<Person> query = session.createQuery( "from Person p where p.name = coalesce(:name , p.name) ", Person.class );
            query.setParameter("name", "Johannes");
            List<Person> resultList = query.getResultList();
            assertThat(resultList, hasItem(person));
        } );

    }

    @Test
    public void HHH_10463_NullInCoalesce() {
        doInHibernate( this::sessionFactory, session -> {
            TypedQuery<Person> query = session.createQuery( "from Person p where p.name = coalesce(:name, p.name) ", Person.class );
            query.setParameter("name", null);
            List<Person> resultList = query.getResultList();
            assertThat(resultList, hasItem(person));
        } );
    }

    @Test
    public void HHH_10463_NullInCoalesce_PostgreSQL_Workaround() {
        doInHibernate( this::sessionFactory, session -> {
            TypedQuery<Person> query = session.createQuery( "from Person p where p.name = coalesce(cast( :name as string) , p.name) ", Person.class );
            query.setParameter("name", null);
            List<Person> resultList = query.getResultList();
            assertThat(resultList, hasItem(person));
        } );
    }

    @Entity(name = "Person")
    public static class Person {

        @Id
        @GeneratedValue
        private Integer id;

        @Column
        private String name;

        @Column
        private String surname;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSurname() {
            return surname;
        }

        public void setSurname(String surname) {
            this.surname = surname;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person )) return false;

            Person person = (Person) o;

            return id != null ? id.equals(person.id) : person.id == null;

        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }
    }

}
