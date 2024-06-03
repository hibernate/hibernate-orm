/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hql;

import java.util.List;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.TypedQuery;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertThat;

/**
 * @author Christian Beikov
 */
public class InferenceTest extends BaseCoreFunctionalTestCase {

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
			person.setName( "Johannes" );
			person.setSurname( "Buehler" );
			session.persist( person );
		} );
	}

	@After
	public void cleanUp() {
		doInHibernate( this::sessionFactory, session -> {
			session.createMutationQuery( "delete from Person" ).executeUpdate();
		} );
	}

    @Test
    public void testBinaryArithmeticInference() {
        doInHibernate( this::sessionFactory, session -> {
            TypedQuery<Person> query = session.createQuery( "from Person p where p.id + 1 < :param", Person.class );
            query.setParameter("param", 10);
            List<Person> resultList = query.getResultList();
            assertThat(resultList, hasItem(person));
        } );

    }

    @Test
    @JiraKey("HHH-17386")
    public void testInferenceSourceResetForOnClause() {
        doInHibernate( this::sessionFactory, session -> {
            session.createQuery( "from Person p where p in (select p2 from Person p2 join Person p3 on exists (select 1 from Person p4))", Person.class )
                .getResultList();
        } );

    }

	@Test
	@JiraKey("HHH-18046")
	@SkipForDialect( dialectClass = CockroachDialect.class, reason = "CockroachDB doesn't support multiplication between int and float columns" )
	public void testBinaryArithmeticParameterInference() {
		doInHibernate( this::sessionFactory, session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			JpaCriteriaQuery<Double> cq = cb.createQuery( Double.class );
			JpaRoot<Person> root = cq.from( Person.class );
			cq.select( cb.toDouble( cb.prod( root.get( "id" ), 0.5f ) ) );
			Double result = session.createQuery( cq ).getSingleResult();
			assertThat( result, is( person.getId() * 0.5d ) );
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
