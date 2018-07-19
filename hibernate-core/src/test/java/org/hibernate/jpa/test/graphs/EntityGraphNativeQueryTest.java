/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.graphs;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.EntityGraph;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-12476" )
public class EntityGraphNativeQueryTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Foo.class, Bar.class, Baz.class };
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, em -> {
			Bar bar = new Bar();
			em.persist( bar );

			Baz baz = new Baz();
			em.persist( baz );

			Foo foo = new Foo();
			foo.bar = bar;
			foo.baz = baz;
			em.persist( foo );
		} );
	}

	@Test
	public void testQuery() {
		Foo foo = doInJPA( this::entityManagerFactory, em -> {
			EntityGraph<Foo> fooGraph = em.createEntityGraph( Foo.class );
			fooGraph.addAttributeNodes( "bar", "baz" );

			return em.createQuery( "select f from Foo f", Foo.class )
					.setHint( "javax.persistence.loadgraph", fooGraph )
					.getSingleResult();
		} );

		assertNotNull( foo.bar );
		assertNotNull( foo.baz );
	}

	@Test
	public void testNativeQueryLoadGraph() {
		try {
			doInJPA( this::entityManagerFactory, em -> {
				EntityGraph<Foo> fooGraph = em.createEntityGraph( Foo.class );
				fooGraph.addAttributeNodes( "bar", "baz" );

				em.createNativeQuery(
					"select " +
					"	f.id as id, " +
					"	f.bar_id as bar_id, " +
					"	f.baz_id as baz_id " +
					"from Foo f", Foo.class )
				.setHint( QueryHints.HINT_LOADGRAPH, fooGraph )
				.getSingleResult();

				fail("Should throw exception");
			} );
		}
		catch (Exception e) {
			assertEquals( "A native SQL query cannot use EntityGraphs", e.getMessage() );
		}
	}

	@Test
	public void testNativeQueryFetchGraph() {
		try {
			doInJPA( this::entityManagerFactory, em -> {
				EntityGraph<Foo> fooGraph = em.createEntityGraph( Foo.class );
				fooGraph.addAttributeNodes( "bar", "baz" );

				em.createNativeQuery(
					"select " +
					"	f.id as id, " +
					"	f.bar_id as bar_id, " +
					"	f.baz_id as baz_id " +
					"from Foo f", Foo.class )
				.setHint( QueryHints.HINT_FETCHGRAPH, fooGraph )
				.getSingleResult();

				fail("Should throw exception");
			} );
		}
		catch (Exception e) {
			assertEquals( "A native SQL query cannot use EntityGraphs", e.getMessage() );
		}
	}

    @Entity(name = "Foo")
    public static class Foo {

		@Id
		@GeneratedValue
		public Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		public Bar bar;

		@ManyToOne(fetch = FetchType.LAZY)
		public Baz baz;
	}

	@Entity(name = "Bar")
	public static class Bar {

		@Id
		@GeneratedValue
		public Integer id;

        @OneToMany(mappedBy = "bar")
        public Set<Foo> foos = new HashSet<>();
	}

	@Entity(name = "Baz")
    public static class Baz {

		@Id
		@GeneratedValue
        public Integer id;

        @OneToMany(mappedBy = "baz")
        public Set<Foo> foos = new HashSet<>();

	}

}
