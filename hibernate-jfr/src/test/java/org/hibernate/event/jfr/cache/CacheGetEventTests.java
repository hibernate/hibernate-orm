package org.hibernate.event.jfr.cache;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.event.jfr.internal.CacheGetEvent;
import org.hibernate.event.jfr.internal.JdbcBatchExecutionEvent;

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
		CacheGetEventTests.TestEntity.class,
})
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
		}
)
public class CacheGetEventTests {
	public JfrEvents jfrEvents = new JfrEvents();

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity entity = new TestEntity( 1, "name_1" );
					session.persist( entity );
				}
		);
	}

	@Test
	@EnableEvent(JdbcBatchExecutionEvent.NAME)
	public void testCacheGetEvent(SessionFactoryScope scope) {
		jfrEvents.reset();
		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, 1 );

					List<RecordedEvent> events = jfrEvents.events()
							.filter(
									recordedEvent ->
									{
										String eventName = recordedEvent.getEventType().getName();
										return eventName.equals( CacheGetEvent.NAME );
									}
							).toList();
					assertThat( events ).hasSize( 1 );

					RecordedEvent event = events.get( 0 );
					assertThat( event.getEventType().getName() )
							.isEqualTo( CacheGetEvent.NAME );
					assertThat( event.getLong( "executionTime" ) ).isGreaterThan( 0 );
					assertThat( event.getString( "sessionIdentifier" ) )
							.isEqualTo( session.getSessionIdentifier().toString() );
					assertThat( event.getString( "entityName" ) )
							.isEqualTo( TestEntity.class.getName() );
					assertThat( event.getBoolean( "isNaturalId" ) ).isFalse();
					assertThat( event.getBoolean( "hit" ) ).isTrue();
					assertThat( event.getString( "regionName" ) ).isNotNull();
				}
		);

	}

	@Test
	@EnableEvent(JdbcBatchExecutionEvent.NAME)
	public void testCacheGetEventNoFired(SessionFactoryScope scope) {
		jfrEvents.reset();
		scope.inTransaction(
				session -> {

				}
		);
		List<RecordedEvent> events = jfrEvents.events()
				.filter(
						recordedEvent ->
						{
							String eventName = recordedEvent.getEventType().getName();
							return eventName.equals( CacheGetEvent.NAME );
						}
				).toList();
		assertThat( events ).hasSize( 0 );
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

}
