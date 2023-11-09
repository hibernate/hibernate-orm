package org.hibernate.event.jfr.cache;

import java.util.List;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.event.jfr.internal.CachePutEvent;
import org.hibernate.event.jfr.internal.JdbcBatchExecutionEvent;
import org.hibernate.event.jfr.internal.JfrEventManager;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jdk.jfr.consumer.RecordedEvent;
import org.moditect.jfrunit.EnableEvent;
import org.moditect.jfrunit.JfrEventTest;
import org.moditect.jfrunit.JfrEvents;

import static org.assertj.core.api.Assertions.assertThat;

@JfrEventTest
@DomainModel(annotatedClasses = {
		EntityUpdateCachePutEventTests.TestEntity.class,
		EntityUpdateCachePutEventTests.AnotherTestEntity.class,
})
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
		}
)
public class EntityUpdateCachePutEventTests {
	public JfrEvents jfrEvents = new JfrEvents();

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity entity = new TestEntity( 1, "name_1" );
					session.persist( entity );
					AnotherTestEntity anotherTestEntity = new AnotherTestEntity( 1, "name_1" );
					session.persist( anotherTestEntity );
				}
		);
	}

	@Test
	@EnableEvent(JdbcBatchExecutionEvent.NAME)
	public void testCachePutEvent(SessionFactoryScope scope) {
		jfrEvents.reset();
		scope.inTransaction(
				session -> {

					TestEntity testEntity = session.find( TestEntity.class, 1 );

					testEntity.setName( "Another name" );
					session.flush();
					List<RecordedEvent> events = jfrEvents.events()
							.filter(
									recordedEvent ->
									{
										String eventName = recordedEvent.getEventType().getName();
										return eventName.equals( CachePutEvent.NAME );
									}
							).toList();
					assertThat( events ).hasSize( 1 );

					RecordedEvent event = events.get( 0 );
					assertThat( event.getEventType().getName() )
							.isEqualTo( CachePutEvent.NAME );
					assertThat( event.getLong( "executionTime" ) ).isGreaterThan( 0 );
					assertThat( event.getString( "sessionIdentifier" ) )
							.isEqualTo( session.getSessionIdentifier().toString() );
					assertThat( event.getString( "entityName" ) ).isEqualTo( TestEntity.class.getName() );
					// cache strategy is READ_WRITE so no cache insert happened
					assertThat( event.getBoolean( "cacheChanged" ) ).isFalse();
					assertThat( event.getBoolean( "isNaturalId" ) ).isFalse();
					assertThat( event.getString( "regionName" ) ).isNotNull();
					assertThat( event.getString( "description" ) ).isEqualTo( JfrEventManager.CacheActionDescription.ENTITY_UPDATE.getText() );

					jfrEvents.reset();

					AnotherTestEntity anotherTestEntity = session.find( AnotherTestEntity.class, 1 );

					anotherTestEntity.setName( "Another name" );
					session.flush();

					events = jfrEvents.events()
							.filter(
									recordedEvent ->
									{
										String eventName = recordedEvent.getEventType().getName();
										return eventName.equals( CachePutEvent.NAME );
									}
							).toList();
					assertThat( events ).hasSize( 1 );

					event = events.get( 0 );
					assertThat( event.getEventType().getName() )
							.isEqualTo( CachePutEvent.NAME );
					assertThat( event.getLong( "executionTime" ) ).isGreaterThan( 0 );
					assertThat( event.getString( "sessionIdentifier" ) )
							.isEqualTo( session.getSessionIdentifier().toString() );
					assertThat( event.getString( "entityName" ) ).isEqualTo( AnotherTestEntity.class.getName() );
					// cache strategy is TRANSACTIONAL so cache insert should happen
					assertThat( event.getBoolean( "cacheChanged" ) ).isTrue();
					assertThat( event.getString( "regionName" ) ).isNotNull();
					assertThat( event.getBoolean( "isNaturalId" ) ).isFalse();
					assertThat( event.getString( "description" ) ).isEqualTo( JfrEventManager.CacheActionDescription.ENTITY_UPDATE.getText() );
				}
		);

	}

	@Entity(name = "TestEntity")
	@Cacheable
	public static class TestEntity {
		@Id
		private Integer id;

		private String name;

		public TestEntity() {
		}

		public TestEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "AnotherTestEntity")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
	public static class AnotherTestEntity {
		@Id
		private Integer id;

		private String name;

		public AnotherTestEntity() {
		}

		public AnotherTestEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
