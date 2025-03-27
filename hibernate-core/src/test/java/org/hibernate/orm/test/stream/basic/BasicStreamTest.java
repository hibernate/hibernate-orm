/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stream.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.internal.util.MutableInteger;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = BasicStreamTest.MyEntity.class )
@SessionFactory
public class BasicStreamTest {

	@BeforeEach
	void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			MyEntity e = new MyEntity();
			e.id = 1;
			e.name = "Test";
			session.persist( e );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testBasicStreamHandling(SessionFactoryScope scope) {
		// make sure that closing the Stream releases the ScrollableResults too
		scope.inTransaction( (session) -> {
			// at start, we should have no registered resources
			assertThat( session.getJdbcCoordinator()
								.getLogicalConnection()
								.getResourceRegistry()
								.hasRegisteredResources(), is( false ) );

			final Stream<MyEntity> stream = session.createQuery( "from MyEntity", MyEntity.class ).stream();
			//noinspection TryFinallyCanBeTryWithResources
			try {
				stream.forEach( System.out::println );
				// we should have registered resources here as the underlying ScrollableResults is still open
				assertThat( session.getJdbcCoordinator()
									.getLogicalConnection()
									.getResourceRegistry()
									.hasRegisteredResources(), is( true ) );
			}
			finally {
				stream.close();
				// after an explicit close, we should have no registered resources
				assertThat( session.getJdbcCoordinator()
									.getLogicalConnection()
									.getResourceRegistry()
									.hasRegisteredResources(), is( false ) );
			}

		} );
	}

	@Test
	public void testStreamAutoClosing(SessionFactoryScope scope) {
		// same as #testBasicStreamHandling but using try-with-resources

		final MutableInteger onCloseCount = new MutableInteger();

		scope.inTransaction( (session) -> {
			// at start, we should have no registered resources
			assertThat( session.getJdbcCoordinator()
					.getLogicalConnection()
					.getResourceRegistry()
					.hasRegisteredResources(), is( false ) );
			assertThat( onCloseCount.get(), equalTo( 0 ) );

			final Query<MyEntity> query = session.createQuery( "from MyEntity", MyEntity.class );
			try ( final Stream<MyEntity> stream = query.stream().onClose( onCloseCount::increment ) ) {
				stream.forEach( System.out::println );

				// we should have registered resources here as the underlying ScrollableResults is still open
				assertThat( session.getJdbcCoordinator()
						.getLogicalConnection()
						.getResourceRegistry()
						.hasRegisteredResources(), is( true ) );
				assertThat( onCloseCount.get(), equalTo( 0 ) );
			}

			assertThat( session.getJdbcCoordinator()
					.getLogicalConnection()
					.getResourceRegistry()
					.hasRegisteredResources(), is( false ) );

			assertThat( onCloseCount.get(), equalTo( 1 ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-10824")
	public void testQueryStreamTyping(SessionFactoryScope scope) {
		// Test untyped query stream
		scope.inTransaction( (session) -> {
			try (Stream stream = session.createQuery( "from MyEntity" ).stream()) {
				Object result = stream.findFirst().orElse( null );
				assertTyping( MyEntity.class, result );
			}
		} );

		// Test typed query stream
		scope.inTransaction( (session) -> {
			try (final Stream<MyEntity> stream = session.createQuery( "from MyEntity", MyEntity.class ).stream()) {
				assertTyping( MyEntity.class, stream.findFirst().orElse( null ) );
			}
		} );

		// Test stream query using forEach
		scope.inTransaction( (session) -> {
			try (Stream<MyEntity> stream = session.createQuery( "from MyEntity", MyEntity.class ).stream()) {
				stream.forEach( i -> {
					assertTyping( MyEntity.class, i );
				} );
			}
		} );

		// Test stream query with Object[] result
		scope.inTransaction( (session) -> {
			try (Stream<Object[]> stream = session.createQuery( "SELECT me.id, me.name from MyEntity me" ).stream()) {
				stream.forEach( i -> {
					assertTyping( Integer.class, i[0] );
					assertTyping( String.class, i[1] );
				} );
			}
		} );

		//test tuple stream using criteria
		scope.inTransaction( session -> {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> criteria = cb.createTupleQuery();
			Root<MyEntity> me = criteria.from( MyEntity.class );
			criteria.multiselect( me.get( "id" ), me.get( "name" ) );
			try (Stream<Tuple> data = session.createQuery( criteria ).stream()) {
				data.forEach( tuple -> assertTyping( Tuple.class, tuple ) );
			}
		} );

		//test tuple stream using JPQL
		scope.inTransaction( session -> {
			try (Stream<Tuple> data = session.createQuery( "SELECT me.id, me.name FROM MyEntity me", Tuple.class )
					.stream()) {
				data.forEach( tuple -> assertTyping( Tuple.class, tuple ) );
			}
		} );
	}

	@Entity(name = "MyEntity")
	@Table(name = "MyEntity")
	public static class MyEntity {
		@Id
		public Integer id;
		public String name;
	}

}
