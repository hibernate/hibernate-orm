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

import org.hibernate.LockMode;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static jakarta.persistence.PessimisticLockScope.EXTENDED;
import static jakarta.persistence.PessimisticLockScope.NORMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.LockMode.PESSIMISTIC_WRITE;

/// Verifies that widening lock scope acquires protection for owned collection
/// rows even when the root already has an equal or stronger lock mode.
/// Separate transactions probe physical protection with bounded H2 lock waits.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = PessimisticLockScopeExpansionTest.Item.class)
@SessionFactory
@RequiresDialect(H2Dialect.class)
@Tag("db-locking")
@Jira( "https://hibernate.atlassian.net/browse/HHH-21030" )
class PessimisticLockScopeExpansionTest {

	@BeforeEach
	void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Item( 1, "initial" ) ) );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void normalScopeProtectsRootButNotCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.find( Item.class, 1, PESSIMISTIC_WRITE, NORMAL );
			assertThat( concurrentUpdateTimesOut( scope, false ) ).as( "root row protection" ).isTrue();
			assertThat( concurrentUpdateTimesOut( scope, true ) ).as( "NORMAL excludes collection rows" ).isFalse();
		} );
	}

	@Test
	void initialExtendedLockProtectsCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			session.lock( item, PESSIMISTIC_WRITE, EXTENDED );
			assertThat( concurrentUpdateTimesOut( scope, true ) ).as( "initial EXTENDED collection protection" ).isTrue();
		} );
	}

	@ParameterizedTest(name = "existing WRITE, requested {0} with EXTENDED scope")
	@EnumSource(value = LockMode.class, names = { "PESSIMISTIC_READ", "PESSIMISTIC_WRITE" })
	@FailureExpected(reason = "Here's the rub")
	void widerScopeProtectsCollectionDespiteExistingMode(LockMode requestedMode, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1, PESSIMISTIC_WRITE, NORMAL );
			item.title = "local change";
			session.lock( item, requestedMode, EXTENDED );
			assertThat( session.getCurrentLockMode( item ) ).as( "retain the stronger root mode" ).isEqualTo( PESSIMISTIC_WRITE );
			assertThat( item.title ).as( "preserve local managed state" ).isEqualTo( "local change" );
			assertThat( concurrentUpdateTimesOut( scope, true ) )
					.as( "EXTENDED must protect collection rows even when %s does not upgrade the root mode", requestedMode )
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
										? "update scope_expansion_tags set tag_value = 'changed' where item_id = 1"
										: "update scope_expansion_item set title = 'changed' where id = 1" );
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

	@Entity(name = "ScopeExpansionItem")
	@Table(name = "scope_expansion_item")
	static class Item {
		@Id
		int id;
		String title;
		@ElementCollection
		@CollectionTable(name = "scope_expansion_tags", joinColumns = @JoinColumn(name = "item_id"))
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
