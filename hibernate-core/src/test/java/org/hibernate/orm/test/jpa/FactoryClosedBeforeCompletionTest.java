package org.hibernate.orm.test.jpa;

import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.Session;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.RequiresDialect;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Transaction completion must not silently discard pending state after factory closure.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class FactoryClosedBeforeCompletionTest {
	enum Scenario {
		EMPTY, MIXED, QUEUED, QUEUED_ONLY, COLLECTION, COLLECTION_ONLY, CLEAN, FLUSHED, OPEN,
		MANUAL, READ_ONLY, CLOSED, ROLLBACK_ONLY, SHARED, MANUAL_PARENT, MANUAL_CHILD, CLOSED_CHILD, SHARED_OPEN, SHARED_AUTO_CLOSE
	}

	static Stream<Arguments> scenarios() {
		return Stream.of( Scenario.values() )
				.flatMap( scenario -> Stream.of( Arguments.of( false, scenario ), Arguments.of( true, scenario ) ) );
	}

	@ParameterizedTest
	@MethodSource("scenarios")
	void completion(boolean jta, Scenario scenario) throws Exception {
		final String url = "jdbc:h2:mem:factory_close_" + jta + "_" + scenario + ";DB_CLOSE_DELAY=-1";
		final var builder = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.URL, url )
				.applySetting( AvailableSettings.USER, "sa" )
				.applySetting( AvailableSettings.PASS, "" )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create" );
		if ( jta ) {
			TestingJtaBootstrap.prepare( builder );
		}
		final var registry = builder.build();
		final var tm = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		try (var factory = new MetadataSources( registry ).addAnnotatedClass( Item.class )
				.addAnnotatedClass( QueuedItem.class ).buildMetadata().buildSessionFactory();
				var verification = DriverManager.getConnection( url, "sa", "" )) {
			if ( scenario == Scenario.READ_ONLY ) {
				factory.inTransaction( session -> session.persist( new Item() ) );
			}
			if ( jta ) {
				tm.begin();
			}
			try (var parent = factory.withOptions().readOnly( scenario == Scenario.READ_ONLY ).openSession()) {
				final var tx = parent.beginTransaction();
				if ( scenario == Scenario.READ_ONLY ) {
					parent.find( Item.class, 1L );
				}
				final boolean shared = scenario == Scenario.SHARED || scenario == Scenario.MANUAL_PARENT
						|| scenario == Scenario.MANUAL_CHILD || scenario == Scenario.CLOSED_CHILD || scenario == Scenario.SHARED_OPEN
						|| scenario == Scenario.SHARED_AUTO_CLOSE;
				try (Session child = shared ? parent.sessionWithOptions().connection()
						.autoClose( scenario == Scenario.SHARED_AUTO_CLOSE ).openSession() : null) {
					final Session session = shared ? child : parent;
					if ( scenario == Scenario.MANUAL_PARENT ) {
						parent.setHibernateFlushMode( FlushMode.MANUAL );
					}
					if ( scenario == Scenario.MANUAL || scenario == Scenario.MANUAL_CHILD ) {
						session.setHibernateFlushMode( FlushMode.MANUAL );
					}
					if ( scenario != Scenario.EMPTY && scenario != Scenario.READ_ONLY ) {
						final var item = new Item();
						item.name = "before";
						session.persist( item ); // IDENTITY executes before completion
						if ( scenario == Scenario.COLLECTION || scenario == Scenario.COLLECTION_ONLY ) {
							session.flush();
							item.tags.add( "pending" );
							if ( scenario == Scenario.COLLECTION_ONLY ) {
								// Isolate retained collection state without managed entity entries.
								final var context = ((SessionImplementor) session).getPersistenceContextInternal();
								context.removeEntry( item );
								assertEquals( 0, context.getNumberOfManagedEntities() );
							}
						}
						else if ( scenario == Scenario.QUEUED || scenario == Scenario.QUEUED_ONLY ) {
							final var queued = new QueuedItem();
							session.persist( queued );
							if ( scenario == Scenario.QUEUED_ONLY ) {
								// Isolate the action queue from persistence-context counts.
								final var context = ((SessionImplementor) session).getPersistenceContextInternal();
								context.removeEntry( item );
								context.removeEntry( queued );
								context.getCollectionEntries().clear();
								assertEquals( 0, context.getNumberOfManagedEntities() );
							}
						}
						else if ( scenario == Scenario.CLEAN ) {
							session.flush();
							session.clear();
							session.find( Item.class, item.id );
						}
						else {
							item.name = "after";
						}
						if ( scenario == Scenario.FLUSHED ) {
							session.flush();
						}
					}
					if ( scenario == Scenario.CLOSED || scenario == Scenario.CLOSED_CHILD ) {
						session.close();
					}
					if ( scenario == Scenario.ROLLBACK_ONLY ) {
						tx.markRollbackOnly();
					}
					if ( scenario != Scenario.OPEN && scenario != Scenario.SHARED_OPEN && scenario != Scenario.SHARED_AUTO_CLOSE ) {
						factory.close();
					}
					final boolean rejected = switch ( scenario ) {
						case MIXED, QUEUED, QUEUED_ONLY, COLLECTION, COLLECTION_ONLY, CLEAN, FLUSHED, SHARED, MANUAL_PARENT -> true;
						default -> false;
					};
					if ( rejected ) {
						final var failure = assertThrows( Exception.class, () -> {
							if ( jta ) {
								tm.commit();
							}
							else {
								tx.commit();
							}
						} );
						if ( !jta ) {
							assertInstanceOf( IllegalStateException.class, failure );
							assertTrue( failure.getMessage().contains( "may contain unflushed changes" ) );
						}
					}
					else if ( !jta && scenario == Scenario.CLOSED ) {
						final var failure = assertThrows( IllegalStateException.class, tx::commit );
						assertTrue( failure.getMessage().contains( "is closed" ) );
					}
					else if ( jta && scenario == Scenario.ROLLBACK_ONLY ) {
						assertThrows( jakarta.transaction.RollbackException.class, tm::commit );
					}
					else {
						if ( jta ) {
							tm.commit();
						}
						else {
							tx.commit();
						}
					}
					if ( scenario == Scenario.SHARED_AUTO_CLOSE ) {
						assertTrue( !child.isOpen() );
					}
					try (var statement = verification.createStatement();
							var rows = statement.executeQuery( "select count(*), max(name) from shutdown_item" )) {
						assertTrue( rows.next() );
						if ( scenario == Scenario.OPEN || scenario == Scenario.SHARED_OPEN
								|| scenario == Scenario.SHARED_AUTO_CLOSE ) {
							assertEquals( "after", rows.getString( 2 ) );
						}

						assertEquals( rejected || scenario == Scenario.EMPTY || scenario == Scenario.ROLLBACK_ONLY ? 0 : 1,
								rows.getInt( 1 ) );
					}
				}
			}
		}
		finally {
			if ( jta && tm.getStatus() != jakarta.transaction.Status.STATUS_NO_TRANSACTION ) {
				tm.rollback();
			}
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Entity(name = "ShutdownItem")
	@Table(name = "shutdown_item")
	static class Item {
		@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
		Long id;
		String name;
		@ElementCollection
		List<String> tags = new ArrayList<>();
	}

	@Entity(name = "ShutdownQueuedItem")
	@Table(name = "shutdown_queued_item")
	static class QueuedItem {
		@Id
		Long id = 1L;
	}
}
