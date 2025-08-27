/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.SoftDelete;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@linkplain SoftDelete @SoftDelete} with all default values
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = ImplicitSoftDeleteTests.ImplicitEntity.class)
@SessionFactory
public class ImplicitSoftDeleteTests {
	@BeforeEach
	void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new ImplicitEntity( 1, "first" ) );
			session.persist( new ImplicitEntity( 2, "second" ) );
			session.persist( new ImplicitEntity( 3, "third" ) );
		} );

		scope.inTransaction( (session) -> {
			final ImplicitEntity first = session.getReference( ImplicitEntity.class, 1 );
			session.remove( first );

			session.flush();

			// make sure all 3 are still physically there
			session.doWork( (connection) -> {
				final Statement statement = connection.createStatement();
				final ResultSet resultSet = statement.executeQuery( "select count(1) from implicit_entities" );
				resultSet.next();
				final int count = resultSet.getInt( 1 );
				assertThat( count ).isEqualTo( 3 );
			} );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testSelectionQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			// should not return #1
			assertThat( session.createQuery( "from ImplicitEntity" ).list() ).hasSize( 2 );
		} );
	}

	@Test
	void testLoading(SessionFactoryScope scope) {
		// Load
		scope.inTransaction( (session) -> {
			assertThat( session.get( ImplicitEntity.class, 1 ) ).isNull();
			assertThat( session.get( ImplicitEntity.class, 2 ) ).isNotNull();
			assertThat( session.get( ImplicitEntity.class, 3 ) ).isNotNull();
		} );

		// Proxy
		scope.inTransaction( (session) -> {
			final ImplicitEntity reference = session.getReference( ImplicitEntity.class, 1 );
			try {
				reference.getName();
				fail( "Expecting to fail" );
			}
			catch (ObjectNotFoundException expected) {
			}

			final ImplicitEntity reference2 = session.getReference( ImplicitEntity.class, 2 );
			reference2.getName();

			final ImplicitEntity reference3 = session.getReference( ImplicitEntity.class, 3 );
			reference3.getName();
		} );
	}

	@Test
	void testMultiLoading(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final List<ImplicitEntity> results = session
					.byMultipleIds( ImplicitEntity.class )
					// otherwise the first position would contain a null for #1
					.enableOrderedReturn( false )
					.multiLoad( 1, 2, 3 );
			assertThat( results ).hasSize( 2 );
		} );
	}

	@Test
	void testNaturalIdLoading(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final ImplicitEntity first = session.bySimpleNaturalId( ImplicitEntity.class ).load( "first" );
			assertThat( first ).isNull();

			final ImplicitEntity second = session.bySimpleNaturalId( ImplicitEntity.class ).load( "second" );
			assertThat( second ).isNotNull();
		} );
	}

	@Test
	void testDeletion(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final ImplicitEntity reference = session.getReference( ImplicitEntity.class, 2 );
			session.remove( reference );
			session.flush();

			final List<ImplicitEntity> active = session
					.createSelectionQuery( "from ImplicitEntity", ImplicitEntity.class )
					.list();
			// #1 was "deleted" up front and we just "deleted" #2... only #3 should be active
			assertThat( active ).hasSize( 1 );
			assertThat( active.get(0).getId() ).isEqualTo( 3 );
		} );
	}

	@Test
	void testFullUpdateMutationQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final int affected = session.createMutationQuery( "update ImplicitEntity set name = null" ).executeUpdate();
			assertThat( affected ).isEqualTo( 2 );
		} );
	}

	@Test
	void testRestrictedUpdateMutationQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final int affected = session
					.createMutationQuery( "update ImplicitEntity set name = null where name = 'second'" )
					.executeUpdate();
			assertThat( affected ).isEqualTo( 1 );
		} );
	}

	@Test
	void testFullDeleteMutationQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final int affected = session.createMutationQuery( "delete ImplicitEntity" ).executeUpdate();
			// only #2 and #3
			assertThat( affected ).isEqualTo( 2 );
		} );
	}

	@Test
	void testRestrictedDeleteMutationQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final int affected = session
					.createMutationQuery( "delete ImplicitEntity where name = 'second'" )
					.executeUpdate();
			// only #2
			assertThat( affected ).isEqualTo( 1 );
		} );
	}

	@Entity(name="ImplicitEntity")
	@Table(name="implicit_entities")
	@SoftDelete
	public static class ImplicitEntity {
		@Id
		private Integer id;
		@NaturalId
		private String name;

		public ImplicitEntity() {
		}

		public ImplicitEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
