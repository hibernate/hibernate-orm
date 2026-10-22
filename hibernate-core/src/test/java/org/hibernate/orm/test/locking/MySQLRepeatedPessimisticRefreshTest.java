package org.hibernate.orm.test.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQLDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey("HHH-21010")
@RequiresDialect(value = MySQLDialect.class, matchSubTypes = false)
@ServiceRegistry(settings = @Setting(name = AvailableSettings.ISOLATION, value = "4"))
@DomainModel(annotatedClasses = MySQLRepeatedPessimisticRefreshTest.Item.class)
@SessionFactory
public class MySQLRepeatedPessimisticRefreshTest {

	@AfterEach
	void dropData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void repeatedPessimisticRefreshDoesNotRestoreStaleState(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Item( 1L, "initial" ) ) );

		scope.inTransaction( transactionASession -> {
			// Establish Transaction A's consistent-read snapshot.
			final Item item = transactionASession.find( Item.class, 1L );
			assertEquals( "initial", item.description );

			// Transaction B commits a value that is newer than Transaction A's snapshot.
			scope.inTransaction( transactionBSession ->
					transactionBSession.find( Item.class, 1L ).description = "updated" );

			// A pessimistic locking read is a current read on MySQL and sees Transaction B's update.
			transactionASession.refresh( item, LockModeType.PESSIMISTIC_WRITE );
			assertEquals( "updated", item.description );

			// A repeated pessimistic refresh must remain a current read even though the lock is already held.
			transactionASession.refresh( item, LockModeType.PESSIMISTIC_WRITE );
			assertEquals( "updated", item.description );
		} );
	}

	@Entity(name = "Item")
	@Table(name = "test_item")
	static class Item {
		@Id
		Long id;

		String description;

		Item() {
		}

		Item(Long id, String description) {
			this.id = id;
			this.description = description;
		}
	}
}
