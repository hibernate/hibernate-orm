package org.hibernate.orm.test.locking;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.Locking;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Verifies physical row protection for scalar pessimistic queries. Each probe
/// updates a selected row through a separate transaction with a bounded H2 lock
/// wait, then rolls back. Direct locking provides a positive control.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = FollowOnScalarLockingTest.Item.class)
@SessionFactory
@RequiresDialect(H2Dialect.class)
@Tag("db-locking")
@Jira( "https://hibernate.atlassian.net/browse/HHH-21026" )
class FollowOnScalarLockingTest {

	@BeforeEach
	void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Item( 1, "same title" ) );
			session.persist( new Item( 2, "same title" ) );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void directScalarQueryProtectsRows(SessionFactoryScope scope) {
		assertScalarProtection( scope );
	}

	@ParameterizedTest
	@ValueSource(strings = { "i, i.title", "new list(i, i.title)" })
	void mixedFollowOnResultsProtectRows(String selection, SessionFactoryScope scope) {
		scope.inTransaction( reader -> {
			assertThat( reader.createQuery( "select " + selection + " from ScalarLockItem i", Object.class )
					.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE ).getResultList() ).hasSize( 2 );
			for ( int id : new int[] { 1, 2 } ) {
				assertThat( concurrentUpdateTimesOut( scope, id ) ).as( "row %s must be protected", id ).isTrue();
			}
		} );
	}

	@Test
	void forcedFollowOnScalarQueryIsRejected(SessionFactoryScope scope) {
		assertScalarRejection( scope, false, Locking.FollowOn.FORCE );
	}

	@Test
	void distinctScalarQueryIsRejected(SessionFactoryScope scope) {
		assertScalarRejection( scope, true, Locking.FollowOn.ALLOW );
	}

	private void assertScalarRejection(SessionFactoryScope scope, boolean distinct, Locking.FollowOn strategy) {
		final List<String> statements = new ArrayList<>();
		try ( var reader = scope.getSessionFactory().withOptions()
				.statementInspector( (UnaryOperator<String>) sql -> { statements.add( sql ); return sql; } ).openSession() ) {
			final var transaction = reader.beginTransaction();
			try {
				assertThatThrownBy( () -> reader.createQuery(
						"select " + (distinct ? "distinct " : "") + "i.title from ScalarLockItem i", String.class )
						.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( strategy ).getResultList() )
						.isInstanceOf( IllegalStateException.class ).hasCauseInstanceOf( IllegalQueryOperationException.class )
						.satisfies( failure -> assertThat( failure.getCause() ).hasMessageStartingWith(
								"Follow-on locking cannot acquire all requested locks from this result shape" ) );
				assertThat( statements ).isEmpty();
			}
			finally {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			}
		}
	}

	private void assertScalarProtection(SessionFactoryScope scope) {
		scope.inTransaction( reader -> {
			assertThat( reader.createQuery( "select i.title from ScalarLockItem i", String.class )
					.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.DISALLOW ).getResultList() )
					.containsExactly( "same title", "same title" );
			for ( int id : new int[] { 1, 2 } ) {
				assertThat( concurrentUpdateTimesOut( scope, id ) ).as( "row %s must be protected", id ).isTrue();
			}
		} );
	}

	private boolean concurrentUpdateTimesOut(SessionFactoryScope scope, int id) {
		try ( var writer = scope.getSessionFactory().openSession() ) {
			final var transaction = writer.beginTransaction();
			try {
				return writer.doReturningWork( connection -> {
					try ( var statement = connection.createStatement() ) {
						final int originalTimeout;
						try ( var result = statement.executeQuery( "call lock_timeout()" ) ) {
							result.next();
							originalTimeout = result.getInt( 1 );
						}
						statement.executeUpdate( "set lock_timeout 100" );
						try ( var update = connection.prepareStatement(
								"update follow_on_scalar_item set title = 'concurrent change' where id = ?" ) ) {
							update.setInt( 1, id );
							try {
								assertThat( update.executeUpdate() ).isEqualTo( 1 );
								return false;
							}
							catch (SQLException conflict) {
								assertThat( conflict.getErrorCode() ).as( "H2 lock timeout" ).isEqualTo( 50200 );
								assertThat( conflict.getSQLState() ).isEqualTo( "HYT00" );
								return true;
							}
						}
						finally {
							statement.executeUpdate( "set lock_timeout " + originalTimeout );
						}
					}
				} );
			}
			finally {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			}
		}
	}

	@Entity(name = "ScalarLockItem")
	@Table(name = "follow_on_scalar_item")
	static class Item {
		@Id
		int id;
		String title;

		Item() {
		}

		Item(int id, String title) {
			this.id = id;
			this.title = title;
		}
	}
}
