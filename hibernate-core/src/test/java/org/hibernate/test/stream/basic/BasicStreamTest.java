/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.stream.basic;

import java.util.Optional;
import java.util.stream.Stream;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.boot.MetadataSources;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.query.Query;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class BasicStreamTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( MyEntity.class );
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
		assertThat( ( (SessionImplementor) session ).getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources(), is( true ) );
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

	@Entity(name = "MyEntity")
	@Table(name="MyEntity")
	public static class MyEntity {
		@Id
		public Integer id;
		public String name;
	}

}
