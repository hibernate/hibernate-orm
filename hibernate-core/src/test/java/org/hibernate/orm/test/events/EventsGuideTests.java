package org.hibernate.orm.test.events;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityAgent;
import jakarta.persistence.EntityListener;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.PostCreate;
import jakarta.persistence.PostDelete;
import jakarta.persistence.PostInsert;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PreDelete;
import jakarta.persistence.PreClose;
import jakarta.persistence.PreInsert;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.boot.ConflictingInterceptorSettingsException;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Executable callback and interceptor examples for the Interceptors and Events chapter.
///
/// @author Steve Ebersole
@BaseUnitTest
public class EventsGuideTests {

	static final List<String> events = new ArrayList<>();

	@BeforeEach
	void reset() {
		events.clear();
	}

	@Test
	void callbackTimingAndRollback() {
		try ( var factory = configuration( Record.class ).createEntityManagerFactory() ) {
			try ( var entityManager = factory.createEntityManager() ) {
				// tag::events-callback-timing[]
				var transaction = entityManager.getTransaction();
				transaction.begin();
				var record = new Record( 1, "  first  " );
				entityManager.persist( record );
				assertThat( record.name ).isEqualTo( "first" );
				assertThat( events ).containsExactly( "pre-persist" );

				entityManager.flush();
				assertThat( events ).containsExactly( "pre-persist", "pre-insert", "post-persist", "post-insert" );
				assertThat( transaction.isActive() ).isTrue();
				transaction.rollback();
				// end::events-callback-timing[]
			}
			try ( var entityManager = factory.createEntityManager() ) {
				assertThat( entityManager.find( Record.class, 1 ) ).isNull();
			}
			factory.inTransaction( session -> session.persist( new Record( 2, "second" ) ) );
			try ( var entityManager = factory.createEntityManager() ) {
				var transaction = entityManager.getTransaction();
				transaction.begin();
				events.clear();
				entityManager.remove( entityManager.find( Record.class, 2 ) );
				assertThat( events ).containsExactly( "pre-remove" );
				entityManager.flush();
				assertThat( events ).containsExactly( "pre-remove", "pre-delete", "post-remove", "post-delete" );
				transaction.rollback();
			}
			try ( var entityManager = factory.createEntityManager() ) {
				assertThat( entityManager.find( Record.class, 2 ) ).isNotNull();
			}
		}
	}

	@Test
	void statelessInsertAndDeleteCallbacks() {
		try ( var factory = configuration( Record.class ).createEntityManagerFactory();
				var session = factory.openStatelessSession() ) {
			var transaction = session.beginTransaction();
			var record = new Record( 1, "  stateless  " );
			session.insert( record );
			assertThat( record.name ).isEqualTo( "stateless" );
			assertThat( events ).containsExactly( "pre-persist", "pre-insert", "post-persist", "post-insert" );
			events.clear();
			session.delete( record );
			assertThat( events ).containsExactly( "pre-remove", "pre-delete", "post-remove", "post-delete" );
			transaction.commit();
		}
	}

	@Test
	void discoveredListener() {
		// tag::events-discovered-registration[]
		var configuration = new HibernatePersistenceConfiguration( "discovered-listener" )
				.managedClasses( PlainRecord.class, CreationListener.class );
		// end::events-discovered-registration[]
		configuration.properties( ServiceRegistryUtil.createBaseSettingsWithExport() );
		try ( var factory = configuration.createEntityManagerFactory() ) {
			factory.inTransaction( session -> session.persist( new PlainRecord( 1, "created" ) ) );
			assertThat( events ).containsExactly( "discovered:1" );
		}
	}

	@Test
	void runtimeListenerAndCancellation() {
		try ( var factory = configuration( PlainRecord.class ).createEntityManagerFactory() ) {
			EntityManagerFactory entityManagerFactory = factory;
			// tag::events-runtime-registration[]
			var creations = new AtomicInteger();
			var registration = entityManagerFactory.addListener(
					PlainRecord.class, PrePersist.class, record -> creations.incrementAndGet() );
			// end::events-runtime-registration[]
			factory.inTransaction( session -> session.persist( new PlainRecord( 1, "first" ) ) );
			assertThat( creations ).hasValue( 1 );
			// tag::events-runtime-cancellation[]
			registration.cancel();
			// end::events-runtime-cancellation[]
			factory.inTransaction( session -> session.persist( new PlainRecord( 2, "second" ) ) );
			assertThat( creations ).hasValue( 1 );
		}
	}

	@Test
	void runtimeCallbacksFollowDeclaredCallbacksInRegistrationOrder() {
		try ( var factory = configuration( OrderedRecord.class, OrderingDiscoveredListener.class ).createEntityManagerFactory() ) {
			factory.addListener( OrderedRecord.class, PrePersist.class, record -> events.add( "runtime-first" ) );
			factory.addListener( OrderedRecord.class, PrePersist.class, record -> events.add( "runtime-second" ) );
			factory.inTransaction( session -> session.persist( new OrderedRecord() ) );
			assertThat( events ).containsExactly( "discovered", "declared-listener", "entity-callback", "runtime-first", "runtime-second" );
		}
	}

	@Test
	void persistenceUnitLifecycleCallbacks() {
		try ( var factory = configuration( PlainRecord.class, UnitLifecycleListener.class ).createEntityManagerFactory() ) {
			assertThat( events ).containsExactly( "factory-created" );
			try ( var manager = factory.createEntityManager() ) {
				assertThat( manager.isOpen() ).isTrue();
			}
			try ( var agent = factory.createEntityAgent() ) {
				assertThat( agent.isOpen() ).isTrue();
			}
		}
		assertThat( events ).containsExactly( "factory-created", "manager-created", "manager-closing",
				"agent-created", "agent-closing", "factory-closing" );
	}

	@Test
	void modifyingInterceptorState() {
		try ( var factory = configuration( PlainRecord.class ).createEntityManagerFactory() ) {
			try ( var session = factory.withOptions().interceptor( new NormalizingInterceptor() ).openSession() ) {
				var transaction = session.beginTransaction();
				var record = new PlainRecord( 1, "  normalized  " );
				session.persist( record );
				session.flush();
				assertThat( record.name ).isEqualTo( "normalized" );
				transaction.commit();
			}
			try ( var session = factory.openSession() ) {
				assertThat( session.find( PlainRecord.class, 1 ).name ).isEqualTo( "normalized" );
			}
		}
	}

	@Test
	void automaticSessionInterceptorsAndOverrides() {
		var supplierCalls = new AtomicInteger();
		var defaults = new CountingInterceptor();
		var explicit = new CountingInterceptor();
		try ( var factory = configuration( PlainRecord.class )
				.property( "hibernate.session_factory.session_scoped_interceptor",
						(java.util.function.Supplier<Interceptor>) () -> {
							supplierCalls.incrementAndGet();
							return defaults;
						} ).createEntityManagerFactory() ) {
			factory.inTransaction( session -> session.persist( new PlainRecord( 1, "default" ) ) );
			assertThat( defaults.persistences ).hasValue( 1 );
			assertThat( supplierCalls ).hasValue( 1 );
			try ( var session = factory.withOptions().interceptor( explicit ).openSession() ) {
				persist( session, new PlainRecord( 2, "explicit" ) );
			}
			try ( var session = factory.withOptions().noSessionInterceptorCreation().openSession() ) {
				persist( session, new PlainRecord( 3, "suppressed creation" ) );
			}
			assertThat( supplierCalls ).hasValue( 1 );
			assertThat( explicit.persistences ).hasValue( 1 );
		}
		try ( var factory = configuration( PlainRecord.class )
				.property( "hibernate.session_factory.interceptor", defaults ).createEntityManagerFactory() ) {
			try ( var session = factory.withOptions().noSessionInterceptorCreation().openSession() ) {
				persist( session, new PlainRecord( 1, "shared retained" ) );
			}
			try ( var session = factory.withOptions().noInterceptor().openSession() ) {
				persist( session, new PlainRecord( 2, "disabled" ) );
			}
			assertThat( defaults.persistences ).hasValue( 2 );
			try ( var entityManager = factory.createEntityManager( explicit ) ) {
				var transaction = entityManager.getTransaction();
				transaction.begin();
				entityManager.persist( new PlainRecord( 3, "typed creation option" ) );
				transaction.commit();
			}
			assertThat( explicit.persistences ).hasValue( 2 );
		}
	}

	@Test
	void conflictingSettingsAreRejected() {
		assertThatThrownBy( () -> configuration( PlainRecord.class )
				.property( "hibernate.session_factory.interceptor", new CountingInterceptor() )
				.property( "hibernate.session_factory.session_scoped_interceptor", CountingInterceptor.class )
				.createEntityManagerFactory() ).isInstanceOf( ConflictingInterceptorSettingsException.class );
	}

	static HibernatePersistenceConfiguration configuration(Class<?>... entities) {
		return new HibernatePersistenceConfiguration( "events" )
				.managedClasses( entities )
				.properties( ServiceRegistryUtil.createBaseSettingsWithExport() );
	}

	private static void persist(Session session, Object entity) {
		var transaction = session.beginTransaction();
		session.persist( entity );
		transaction.commit();
	}

	// tag::events-jpa-callbacks-example[]
	@Entity(name = "EventRecord")
	@EntityListeners(RecordListener.class)
	public static class Record {
		@Id
		int id;
		String name;

		public Record() {}

		Record(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	public static class RecordListener {
		@PrePersist
		public void beforePersist(Record record) {
			record.name = record.name.strip();
			events.add( "pre-persist" );
		}

		@PreInsert
		public void beforeInsert(Record record) {
			events.add( "pre-insert" );
		}

		@PostPersist
		public void afterPersist(Record record) {
			events.add( "post-persist" );
		}

		@PostInsert
		public void afterInsert(Record record) {
			events.add( "post-insert" );
		}
		// end::events-jpa-callbacks-example[]

		@PreRemove
		public void beforeRemove(Record record) {
			events.add( "pre-remove" );
		}

		@PreDelete
		public void beforeDelete(Record record) {
			events.add( "pre-delete" );
		}

		@PostRemove
		public void afterRemove(Record record) {
			events.add( "post-remove" );
		}

		@PostDelete
		public void afterDelete(Record record) {
			events.add( "post-delete" );
		}
		// tag::events-jpa-callbacks-example[]
	}
	// end::events-jpa-callbacks-example[]

	// tag::events-discovered-listener[]
	@EntityListener
	public static class CreationListener {
		@PrePersist
		public void created(PlainRecord record) {
			events.add( "discovered:" + record.id );
		}
	}
	// end::events-discovered-listener[]

	@Entity(name = "EventPlainRecord")
	public static class PlainRecord {
		@Id
		int id;
		String name;

		public PlainRecord() {}

		public PlainRecord(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	// tag::events-interceptor-state[]
	public static class NormalizingInterceptor implements Interceptor {
		@Override
		public boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
			if ( entity instanceof PlainRecord ) {
				for ( int i = 0; i < propertyNames.length; i++ ) {
					if ( "name".equals( propertyNames[i] ) && state[i] instanceof String name ) {
						String normalized = name.strip();
						if ( !normalized.equals( name ) ) {
							state[i] = normalized;
							return true;
						}
					}
				}
			}
			return false;
		}
	}
	// end::events-interceptor-state[]

	@Entity(name = "EventOrderedRecord")
	@EntityListeners(OrderingDeclaredListener.class)
	public static class OrderedRecord {
		@Id
		int id = 1;

		@PrePersist
		void created() {
			events.add( "entity-callback" );
		}
	}

	public static class OrderingDeclaredListener {
		@PrePersist
		public void created(OrderedRecord record) {
			events.add( "declared-listener" );
		}
	}

	@EntityListener
	public static class OrderingDiscoveredListener {
		@PrePersist
		public void created(Object record) {
			events.add( "discovered" );
		}
	}

	// tag::events-unit-lifecycle[]
	@EntityListener
	public static class UnitLifecycleListener {
		@PostCreate
		public void created(EntityManagerFactory factory) {
			events.add( "factory-created" );
		}

		@PreClose
		public void closing(EntityManagerFactory factory) {
			events.add( "factory-closing" );
		}

		@PostCreate
		public void created(EntityManager manager) {
			events.add( "manager-created" );
		}

		@PreClose
		public void closing(EntityManager manager) {
			events.add( "manager-closing" );
		}

		@PostCreate
		public void created(EntityAgent agent) {
			events.add( "agent-created" );
		}

		@PreClose
		public void closing(EntityAgent agent) {
			events.add( "agent-closing" );
		}
	}
	// end::events-unit-lifecycle[]

	public static class CountingInterceptor implements Interceptor {
		final AtomicInteger persistences = new AtomicInteger();

		@Override
		public boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
			persistences.incrementAndGet();
			return false;
		}
	}
}
