/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.softdelete;

import java.sql.Statement;
import java.util.List;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.type.YesNoConverter;

import org.hibernate.testing.jdbc.SQLStatementInspector;
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
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = SimpleSoftDeleteTests.SimpleEntity.class )
@SessionFactory(useCollectingStatementInspector = true)
public class SimpleSoftDeleteTests {
	@BeforeEach
	void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new SimpleEntity( 1, "first" ) );
			session.persist( new SimpleEntity( 2, "second" ) );
			session.persist( new SimpleEntity( 3, "third" ) );

			session.flush();

			session.doWork( (connection) -> {
				final Statement statement = connection.createStatement();
				statement.execute( "update simple set removed = 'Y' where id = 1" );
			} );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.doWork( (connection) -> {
			final Statement statement = connection.createStatement();
			statement.execute( "delete from simple" );
		} ) );
	}

	@Test
	void testSelectionQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			// should not return #1
			assertThat( session.createQuery( "from SimpleEntity" ).list() ).hasSize( 2 );
		} );
	}

	@Test
	void testLoading(SessionFactoryScope scope) {
		// Load
		scope.inTransaction( (session) -> {
			assertThat( session.get( SimpleEntity.class, 1 ) ).isNull();
			assertThat( session.get( SimpleEntity.class, 2 ) ).isNotNull();
			assertThat( session.get( SimpleEntity.class, 3 ) ).isNotNull();
		} );

		// Proxy
		scope.inTransaction( (session) -> {
			final SimpleEntity reference = session.getReference( SimpleEntity.class, 1 );
			try {
				reference.getName();
				fail( "Expecting to fail" );
			}
			catch (ObjectNotFoundException expected) {
			}

			final SimpleEntity reference2 = session.getReference( SimpleEntity.class, 2 );
			reference2.getName();

			final SimpleEntity reference3 = session.getReference( SimpleEntity.class, 3 );
			reference3.getName();
		} );
	}

	@Test
	void testDeletion(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final SimpleEntity reference = session.getReference( SimpleEntity.class, 2 );
			session.remove( reference );
			session.flush();

			final List<SimpleEntity> active = session
					.createSelectionQuery( "from SimpleEntity", SimpleEntity.class )
					.list();
			// #1 was "deleted" up front and we just "deleted" #2... only #3 should be active
			assertThat( active ).hasSize( 1 );
			assertThat( active.get(0).getId() ).isEqualTo( 3 );
		} );
	}

	@Test
	void testFullUpdateMutationQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final int affected = session.createMutationQuery( "update SimpleEntity set name = null" ).executeUpdate();
			assertThat( affected ).isEqualTo( 2 );
		} );
	}

	@Test
	void testRestrictedUpdateMutationQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final int affected = session
					.createMutationQuery( "update SimpleEntity set name = null where name = 'second'" )
					.executeUpdate();
			assertThat( affected ).isEqualTo( 1 );
		} );
	}

	@Test
	void testFullDeleteMutationQuery(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction( (session) -> {
			statementInspector.clear();
			final int affected = session.createMutationQuery( "delete SimpleEntity" ).executeUpdate();
			// only #2 and #3
			assertThat( affected ).isEqualTo( 2 );
		} );
	}

	@Test
	void testRestrictedDeleteMutationQuery(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction( (session) -> {
			statementInspector.clear();
			final int affected = session
					.createMutationQuery( "delete SimpleEntity where name = 'second'" )
					.executeUpdate();
			// only #2
			assertThat( affected ).isEqualTo( 1 );
		} );
	}

	/**
	 * @implNote Uses YesNoConverter to work across all databases, even those
	 * not supporting an actual BOOLEAN datatype
	 */
	@Entity(name="SimpleEntity")
	@Table(name="simple")
	@SoftDelete(columnName = "removed", converter = YesNoConverter.class)
	public static class SimpleEntity {
		@Id
		private Integer id;
		private String name;

		public SimpleEntity() {
		}

		public SimpleEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
