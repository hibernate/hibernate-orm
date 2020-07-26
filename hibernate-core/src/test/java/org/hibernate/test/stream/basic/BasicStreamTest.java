/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.stream.basic;

import java.util.stream.Stream;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.boot.MetadataSources;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Steve Ebersole
 */
public class BasicStreamTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( MyEntity.class );
	}

	@Test
	public void basicStreamTest() {
		Session session = openSession();
		session.getTransaction().begin();

		// mainly we want to make sure that closing the Stream releases the ScrollableResults too
		assertThat( ( (SessionImplementor) session ).getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources(), is( false ) );
		final Stream<MyEntity> stream = session.createQuery( "from MyEntity", MyEntity.class ).stream();
		assertThat( ( (SessionImplementor) session ).getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources(), is( true ) );
		stream.forEach( System.out::println );
		assertThat( ( (SessionImplementor) session ).getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources(), is( false ) );
		stream.close();
		assertThat( ( (SessionImplementor) session ).getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources(), is( false ) );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10824")
	public void testQueryStream() {
		Session session = openSession();
		try {

			session.getTransaction().begin();
			MyEntity e= new MyEntity();
			e.id = 1;
			e.name = "Test";
			session.persist( e );
			session.getTransaction().commit();
			session.clear();

			// Test stream query without type.
			Object result = session.createQuery( "From MyEntity" ).stream().findFirst().orElse( null );
			assertTyping( MyEntity.class, result );

			// Test stream query with type.
			result = session.createQuery( "From MyEntity", MyEntity.class ).stream().findFirst().orElse( null );
			assertTyping( MyEntity.class, result );

			// Test stream query using forEach
			session.createQuery( "From MyEntity", MyEntity.class ).stream().forEach( i -> {
				assertTyping( MyEntity.class, i );
			} );

			Stream<Object[]> data = session.createQuery( "SELECT me.id, me.name FROM MyEntity me" ).stream();
			data.forEach( i -> {
				assertTyping( Integer.class, i[0] );
				assertTyping( String.class, i[1] );
			});

		}
		finally {
			session.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11743")
	public void testTupleStream() {
		doInHibernate( this::sessionFactory, session -> {
			MyEntity entity = new MyEntity();
			entity.id = 2;
			entity.name = "an entity";
			session.persist( entity );
		} );

		//test tuple stream using criteria
		doInHibernate( this::sessionFactory, session -> {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> criteria = cb.createTupleQuery();
			Root<MyEntity> me = criteria.from( MyEntity.class );
			criteria.multiselect( me.get( "id" ), me.get( "name" ) );
			Stream<Tuple> data = session.createQuery( criteria ).stream();
			data.forEach( tuple -> assertTyping( Tuple.class, tuple ) );
		} );

		//test tuple stream using JPQL
		doInHibernate( this::sessionFactory, session -> {
			Stream<Tuple> data = session.createQuery( "SELECT me.id, me.name FROM MyEntity me", Tuple.class ).stream();
			data.forEach( tuple -> assertTyping( Tuple.class, tuple ) );
		} );
	}

	@Entity(name = "MyEntity")
	@Table(name="MyEntity")
	public static class MyEntity {
		@Id
		public Integer id;
		public String name;
	}

}
