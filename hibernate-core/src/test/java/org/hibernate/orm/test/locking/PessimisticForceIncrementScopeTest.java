package org.hibernate.orm.test.locking;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

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
import org.junit.jupiter.api.Test;

import static jakarta.persistence.PessimisticLockScope.EXTENDED;
import static jakarta.persistence.PessimisticLockScope.NORMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.LockMode.PESSIMISTIC_WRITE;
import static org.hibernate.LockMode.PESSIMISTIC_FORCE_INCREMENT;

/// Reproduces managed pessimistic force increment omitting owned collection-row
/// protection requested by EXTENDED scope. Competing transactions verify physical
/// locks independently of the immediate version increment and reported mode.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = PessimisticForceIncrementScopeTest.Item.class)
@SessionFactory
@RequiresDialect(H2Dialect.class)
@Tag("db-locking")
@Jira( "https://hibernate.atlassian.net/browse/HHH-21031" )
class PessimisticForceIncrementScopeTest {

	@BeforeEach
	void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Item( 1, "initial" ) ) );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void normalForceIncrementProtectsRootButNotCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			assertThat( item.version ).isZero();
			session.lock( item, PESSIMISTIC_FORCE_INCREMENT, NORMAL );
			assertThat( item.version ).as( "immediate version increment" ).isEqualTo( 1 );
			assertThat( concurrentUpdateTimesOut( scope, false ) ).as( "root row protection" ).isTrue();
			assertThat( concurrentUpdateTimesOut( scope, true ) ).as( "NORMAL excludes collection rows" ).isFalse();
		} );
	}

	@Test
	void extendedWriteLockProtectsCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			session.lock( item, PESSIMISTIC_WRITE, EXTENDED );
			assertThat( item.version ).as( "ordinary write lock does not force an increment" ).isZero();
			assertThat( concurrentUpdateTimesOut( scope, true ) ).as( "EXTENDED collection protection" ).isTrue();
		} );
	}

	@Test
	@FailureExpected(reason = "Here's the rub")
	void extendedForceIncrementProtectsCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// Start without a pessimistic lock to isolate the force-increment path
			// from the separate equal/stronger-mode scope-expansion issue.
			final var item = session.find( Item.class, 1 );
			assertThat( item.version ).isZero();
			session.lock( item, PESSIMISTIC_FORCE_INCREMENT, EXTENDED );
			assertThat( item.version ).as( "immediate version increment" ).isEqualTo( 1 );
			assertThat( session.getCurrentLockMode( item ) ).isEqualTo( PESSIMISTIC_FORCE_INCREMENT );
			assertThat( concurrentUpdateTimesOut( scope, false ) ).as( "root row protection" ).isTrue();
			assertThat( concurrentUpdateTimesOut( scope, true ) )
					.as( "EXTENDED must protect owned collection rows as well as incrementing the root version" )
					.isTrue();
		} );
	}

	private boolean concurrentUpdateTimesOut(SessionFactoryScope scope, boolean collection) {
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
						try {
							try {
								final int updated = statement.executeUpdate( collection
										? "update force_increment_scope_tags set tag_value = 'changed' where item_id = 1"
										: "update force_increment_scope_item set title = 'changed' where id = 1" );
								assertThat( updated ).isEqualTo( 1 );
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

	@Entity(name = "ForceIncrementScopeItem")
	@Table(name = "force_increment_scope_item")
	static class Item {
		@Id
		int id;
		@Version
		int version;
		String title;
		@ElementCollection
		@CollectionTable(name = "force_increment_scope_tags", joinColumns = @JoinColumn(name = "item_id"))
		@Column(name = "tag_value")
		Set<String> tags = new HashSet<>();

		Item() {
		}

		Item(int id, String title) {
			this.id = id;
			this.title = title;
			tags.add( "initial tag" );
		}
	}
}
