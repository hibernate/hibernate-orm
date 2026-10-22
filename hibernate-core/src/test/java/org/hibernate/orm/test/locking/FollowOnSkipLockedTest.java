package org.hibernate.orm.test.locking;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Table;

import org.hibernate.Locking;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.Timeouts.SKIP_LOCKED;
import static org.junit.jupiter.api.Assertions.fail;

/// Reproduces follow-on acquisition returning a row skipped because another
/// transaction holds its lock. The direct query is a control demonstrating
/// that the conflicting lock is held and H2 skips the row correctly.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = FollowOnSkipLockedTest.Item.class)
@SessionFactory
@RequiresDialect(H2Dialect.class)
@Tag("db-locking")
@Jira( "https://hibernate.atlassian.net/browse/HHH-21028" )
class FollowOnSkipLockedTest {

	@BeforeEach
	void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Item( 1, "available" ) );
			session.persist( new Item( 2, "locked elsewhere" ) );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void directAcquisitionSkipsLockedEntity(SessionFactoryScope scope) {
		scope.inTransaction( holder -> {
			holder.find( Item.class, 2, LockModeType.PESSIMISTIC_WRITE );
			scope.inTransaction( reader -> {
				final var items = reader.createQuery( "from SkipLockedItem order by id", Item.class )
						.setLockMode( LockModeType.PESSIMISTIC_WRITE )
						.setFollowOnStrategy( Locking.FollowOn.DISALLOW )
						.setLockTimeout( SKIP_LOCKED )
						.getResultList();
				assertThat( items ).extracting( item -> item.id ).containsExactly( 1 );
			} );
		} );
	}

	@Test
	@FailureExpected(reason = "Here's the rub")
	void unsupportedFollowOnSkipLockedIsRejectedBeforeExecution(SessionFactoryScope scope) {
		scope.inTransaction( holder -> {
			holder.find( Item.class, 2, LockModeType.PESSIMISTIC_WRITE );
			final List<String> statements = new ArrayList<>();
			final UnaryOperator<String> inspector = sql -> {
				statements.add( sql );
				return sql;
			};
			try ( var reader = scope.getSessionFactory().withOptions().statementInspector( inspector ).openSession() ) {
				final var transaction = reader.beginTransaction();
				try {
					try {
						final var items = reader.createQuery( "from SkipLockedItem order by id", Item.class )
								.setLockMode( LockModeType.PESSIMISTIC_WRITE )
								.setFollowOnStrategy( Locking.FollowOn.FORCE )
								.setLockTimeout( SKIP_LOCKED )
								.getResultList();
						assertThat( statements ).hasSize( 2 );
						assertThat( statements.get( 0 ) ).doesNotContain( "for update" );
						assertThat( statements.get( 1 ) ).contains( "for update skip locked" );
						fail( "Expected unsupported follow-on/SKIP_LOCKED acquisition to be rejected before SQL; "
								+ "instead returned " + items.stream()
								.map( item -> item.id + ":" + reader.getCurrentLockMode( item ) ).toList()
								+ " while another transaction still holds row 2" );
					}
					catch (IllegalQueryOperationException expected) {
						assertThat( statements ).as( "SQL executed before rejecting the unsupported combination" ).isEmpty();
					}
				}
				finally {
					if ( transaction.isActive() ) {
						transaction.rollback();
					}
				}
			}
		} );
	}

	@Entity(name = "SkipLockedItem")
	@Table(name = "follow_on_skip_locked_item")
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
