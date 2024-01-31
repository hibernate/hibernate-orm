/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.softdelete.secondary;

import java.sql.Statement;
import java.util.List;

import org.hibernate.ObjectNotFoundException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = { JoinedRoot.class, JoinedSub.class })
@SessionFactory()
public class JoinedSubclassSoftDeleteTests {
	@BeforeEach
	void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new JoinedSub( 1, "first", "some details" ) );
			session.persist( new JoinedSub( 2, "second", "some details" ) );
			session.persist( new JoinedSub( 3, "third", "some details" ) );
			session.flush();
			session.doWork( (connection) -> {
				final Statement statement = connection.createStatement();
				statement.execute( "update joined_root set removed='Y' where id=1" );
			} );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.doWork( (connection) -> {
			final Statement statement = connection.createStatement();
			statement.execute( "delete from joined_sub" );
			statement.execute( "delete from joined_root" );
		} ) );
	}

	@Test
	void testSelectionQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			// should not return #1
			assertThat( session.createQuery( "from JoinedRoot" ).list() ).hasSize( 2 );
			assertThat( session.createQuery( "from JoinedRoot where id = 1" ).list() ).isEmpty();
		} );

		scope.inTransaction( (session) -> {
			// should not return #1
			assertThat( session.createQuery( "from JoinedSub where id = 1" ).list() ).isEmpty();
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17615" )
	void testCountQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			// should not return #1
			assertThat( session.createQuery( "select count(*) from JoinedRoot" ).uniqueResult() ).isEqualTo( 2L );
			assertThat( session.createQuery( "select count(*) from JoinedRoot where id = 1" ).uniqueResult() ).isEqualTo( 0L );
		} );

		scope.inTransaction( (session) -> {
			// should not return #1
			assertThat( session.createQuery( "select count(*) from JoinedSub" ).uniqueResult() ).isEqualTo( 2L );
			assertThat( session.createQuery( "select count(*) from JoinedSub where id = 1" ).uniqueResult() ).isEqualTo( 0L );
		} );
	}

	@Test
	void testLoading(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			assertThat( session.get( JoinedRoot.class, 1 ) ).isNull();
			assertThat( session.get( JoinedRoot.class, 2 ) ).isNotNull();
			assertThat( session.get( JoinedRoot.class, 3 ) ).isNotNull();
		} );

		scope.inTransaction( (session) -> {
			assertThat( session.get( JoinedSub.class, 1 ) ).isNull();
			assertThat( session.get( JoinedSub.class, 2 ) ).isNotNull();
			assertThat( session.get( JoinedSub.class, 3 ) ).isNotNull();
		} );
	}

	@Test
	void testProxies(SessionFactoryScope scope) {
		// JoinedRoot
		scope.inTransaction( (session) -> {
			final JoinedRoot reference = session.getReference( JoinedRoot.class, 1 );
			try {
				reference.getName();
				fail( "Expecting to fail" );
			}
			catch (ObjectNotFoundException expected) {
			}

			final JoinedRoot reference2 = session.getReference( JoinedRoot.class, 2 );
			reference2.getName();

			final JoinedRoot reference3 = session.getReference( JoinedRoot.class, 3 );
			reference3.getName();
		} );

		// JoinedSub
		scope.inTransaction( (session) -> {
			final JoinedSub reference = session.getReference( JoinedSub.class, 1 );
			try {
				reference.getName();
				fail( "Expecting to fail" );
			}
			catch (ObjectNotFoundException expected) {
			}

			final JoinedSub reference2 = session.getReference( JoinedSub.class, 2 );
			reference2.getName();

			final JoinedSub reference3 = session.getReference( JoinedSub.class, 3 );
			reference3.getName();
		} );
	}

	@Test
	void testDeletion(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final JoinedSub reference = session.getReference( JoinedSub.class, 2 );
			session.remove( reference );
			session.flush();

			final List<JoinedSub> active = session
					.createSelectionQuery( "from JoinedSub", JoinedSub.class )
					.list();
			// #1 was "deleted" up front and we just "deleted" #2... only #3 should be active
			assertThat( active ).hasSize( 1 );
			assertThat( active.get(0).getId() ).isEqualTo( 3 );
		} );
	}

	@Test
	void testFullRootUpdateMutationQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final int affected = session.createMutationQuery( "update JoinedRoot set name = null" ).executeUpdate();
			assertThat( affected ).isEqualTo( 2 );
		} );
	}

	@Test
	void testRestrictedRootUpdateMutationQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final int affected = session
					.createMutationQuery( "update JoinedRoot set name = null where name = 'second'" )
					.executeUpdate();
			assertThat( affected ).isEqualTo( 1 );
		} );
	}

	@Test
	void testFullSubUpdateMutationQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final int affected = session.createMutationQuery( "update JoinedSub set name = null" ).executeUpdate();
			assertThat( affected ).isEqualTo( 2 );
		} );
	}

	@Test
	void testRestrictedSubUpdateMutationQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final int affected = session
					.createMutationQuery( "update JoinedSub set name = null where name = 'second'" )
					.executeUpdate();
			assertThat( affected ).isEqualTo( 1 );
		} );
	}

	@Test
	void testFullRootDeleteMutationQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final int affected = session.createMutationQuery( "delete JoinedRoot" ).executeUpdate();
			// only #2 and #3
			assertThat( affected ).isEqualTo( 2 );
		} );
	}

	@Test
	void testRestrictedRootDeleteMutationQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final int affected = session.createMutationQuery( "delete JoinedRoot where name = 'second'" ).executeUpdate();
			// only #2
			assertThat( affected ).isEqualTo( 1 );
		} );
	}

	@Test
	void testFullSubDeleteMutationQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final int affected = session.createMutationQuery( "delete JoinedSub" ).executeUpdate();
			// only #2 and #3
			assertThat( affected ).isEqualTo( 2 );
		} );
	}

	@Test
	void testRestrictedSubDeleteMutationQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final int affected = session.createMutationQuery( "delete JoinedSub where name = 'second'" ).executeUpdate();
			// only #2
			assertThat( affected ).isEqualTo( 1 );
		} );
	}
}
