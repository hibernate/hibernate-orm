/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.query.Query;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertThat;

/**
 * @author Johannes Buehler
 */
@TestForIssue( jiraKey = "HHH-10463")
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
            Query query = session.createQuery( "from Person p where p.name = coalesce(:name , p.name) ");
            query.setParameter("name", "Johannes");
            List<Person> resultList = query.getResultList();
            assertThat(resultList, hasItem(person));
        } );

    }
    
    @Test
    @SkipForDialect( jiraKey = "HHH-10463", value =  PostgreSQL81Dialect.class)
    @SkipForDialect( jiraKey = "HHH-10463", value =  Oracle8iDialect.class)
    public void HHH_10463_NullInCoalesce() {
        doInHibernate( this::sessionFactory, session -> {
            Query query = session.createQuery("from Person p where p.name = coalesce(:name, p.name) ");
            query.setParameter("name", null);
            List<Person> resultList = query.getResultList();
            assertThat(resultList, hasItem(person));
        } );
    }

    @Test
    @RequiresDialect( PostgreSQL81Dialect.class )
    public void HHH_10463_NullInCoalesce_PostgreSQL_Workaround() {
        doInHibernate( this::sessionFactory, session -> {
            Query query = session.createQuery("from Person p where p.name = coalesce(cast( :name as string) , p.name) ");
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