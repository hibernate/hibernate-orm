package org.hibernate.orm.test.locking;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.Locking;
import org.hibernate.StaleStateException;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/// Deletes required rows after initial materialization but before follow-on
/// acquisition. A separate transaction commits the deletion from the reader's
/// statement inspector, making the acquisition gap deterministic without sleeps.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = FollowOnLockingMissingRowsTest.Item.class)
@SessionFactory
@RequiresDialect(H2Dialect.class)
@Tag("db-locking")
@Jira( "https://hibernate.atlassian.net/browse/HHH-21027" )
class FollowOnLockingMissingRowsTest {

	@BeforeEach
	void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Item( 1, "first" ) );
			session.persist( new Item( 2, "second" ) );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest(name = "deleteAll = {0}")
	@ValueSource(booleans = { false, true })
	@FailureExpected(reason = "Here's the rub")
	void missingRequiredRowsCauseStaleFailure(boolean deleteAll, SessionFactoryScope scope) {
		final var selectCount = new AtomicInteger();
		final UnaryOperator<String> inspector = sql -> {
			if ( sql.startsWith( "select " ) && sql.contains( "follow_on_missing_row_item" )
					&& selectCount.incrementAndGet() == 2 ) {
				assertThat( sql ).as( "follow-on acquisition statement" ).contains( "for update" );
				// The writer has its own session and does not use this inspector.
				scope.inTransaction( writer -> {
					final int deleted = writer.createMutationQuery(
							deleteAll ? "delete from MissingRowItem" : "delete from MissingRowItem where id = 2" )
							.executeUpdate();
					assertThat( deleted ).isEqualTo( deleteAll ? 2 : 1 );
				} );
			}
			return sql;
		};
		try ( var reader = scope.getSessionFactory().withOptions().statementInspector( inspector ).openSession() ) {
			final var transaction = reader.beginTransaction();
			try {
				try {
					final var items = reader.createQuery( "from MissingRowItem order by id", Item.class )
							.setLockMode( PESSIMISTIC_WRITE )
							.setFollowOnStrategy( Locking.FollowOn.FORCE )
							.getResultList();
					assertThat( selectCount.get() ).isEqualTo( 2 );
					fail( "Expected a stale-state failure after deleting " + (deleteAll ? "both rows" : "row 2")
							+ " before follow-on acquisition; instead returned " + items.stream()
							.map( item -> item.id + ":" + reader.getCurrentLockMode( item ) ).toList() );
				}
				catch (OptimisticLockException | StaleStateException expected) {
					assertThat( selectCount.get() ).as( "failure during follow-on acquisition" ).isEqualTo( 2 );
				}
			}
			finally {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			}
		}
	}

	@Entity(name = "MissingRowItem")
	@Table(name = "follow_on_missing_row_item")
	static class Item {
		@Id
		int id;
		@Version
		int version;
		String title;

		Item() {
		}

		Item(int id, String title) {
			this.id = id;
			this.title = title;
		}
	}
}
