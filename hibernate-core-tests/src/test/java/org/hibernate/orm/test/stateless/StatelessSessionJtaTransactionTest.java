package org.hibernate.orm.test.stateless;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.transaction.Status;

import org.hibernate.StatelessSession;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.cfg.JdbcSettings.CONNECTION_HANDLING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ServiceRegistry(
		settings = @Setting(name = CONNECTION_HANDLING, value = "DELAYED_ACQUISITION_AND_RELEASE_BEFORE_TRANSACTION_COMPLETION"),
		settingConfigurations = @SettingConfiguration(configurer = TestingJtaBootstrap.class)
)
@DomainModel(annotatedClasses = StatelessSessionJtaTransactionTest.Item.class)
@SessionFactory
@JiraKey("HHH-21079")
class StatelessSessionJtaTransactionTest {
	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void insertShouldJoinEachJtaTransaction(SessionFactoryScope scope) throws Exception {
		final var transactionManager = TestingJtaPlatformImpl.transactionManager();
		// Open the session before any JTA transaction, as a generated repository does.
		try (StatelessSession session = scope.getSessionFactory().openStatelessSession()) {
			transactionManager.begin();
			session.insert( new Item( 1 ) );
			assertTrue( session.isJoinedToTransaction(), "The first insert must join the JTA transaction" );
			transactionManager.commit();

			// Start a new JTA transaction; the same session must join it when inserting.
			transactionManager.begin();
			session.insert( new Item( 2 ) );
			assertTrue( session.isJoinedToTransaction(), "The second insert must join the new JTA transaction" );
			transactionManager.commit();
		}
		finally {
			if ( transactionManager.getStatus() != Status.STATUS_NO_TRANSACTION ) {
				transactionManager.rollback();
			}
		}
		scope.inTransaction( session -> {
			final var items = session.createSelectionQuery( "from Item", Item.class ).getResultList();
			assertEquals( 2, items.size() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "insert", "update", "delete", "upsert" })
	void multipleMutationsShouldJoinEachJtaTransaction(String mutation, SessionFactoryScope scope) throws Exception {
		if ( mutation.equals( "update" ) || mutation.equals( "delete" ) ) {
			scope.inTransaction( session -> {
				for ( int id = 1; id <= 4; id++ ) {
					session.persist( new Item( id ) );
				}
			} );
		}

		final var transactionManager = TestingJtaPlatformImpl.transactionManager();
		try (StatelessSession session = scope.getSessionFactory().openStatelessSession()) {
			for ( int id = 1; id <= 4; id += 2 ) {
				transactionManager.begin();
				assertFalse( session.isJoinedToTransaction() );
				final var items = List.of( new Item( id ), new Item( id + 1 ) );
				items.forEach( item -> item.name = "changed" );
				switch ( mutation ) {
					case "insert" -> session.insertMultiple( items );
					case "update" -> session.updateMultiple( items );
					case "delete" -> session.deleteMultiple( items );
					case "upsert" -> session.upsertMultiple( items );
				}
				assertTrue( session.isJoinedToTransaction(), mutation + "Multiple must join each JTA transaction" );
				transactionManager.commit();
				assertFalse( session.isJoinedToTransaction() );
			}
		}
		finally {
			if ( transactionManager.getStatus() != Status.STATUS_NO_TRANSACTION ) {
				transactionManager.rollback();
			}
		}
		scope.inTransaction( session -> {
			final var items = session.createSelectionQuery( "from Item order by id", Item.class ).getResultList();
			assertEquals( mutation.equals( "delete" ) ? 0 : 4, items.size() );
			for ( int i = 0; i < items.size(); i++ ) {
				assertEquals( i + 1, items.get( i ).id );
				assertEquals( "changed", items.get( i ).name );
			}
		} );
	}

	@Entity(name = "Item")
	static class Item {
		@Id
		int id;
		String name = "item";

		Item() {
		}

		Item(int id) {
			this.id = id;
		}
	}
}
