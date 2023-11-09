package org.hibernate.event.jfr.cache;

import java.util.List;

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

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jdk.jfr.consumer.RecordedEvent;
import org.moditect.jfrunit.EnableEvent;
import org.moditect.jfrunit.JfrEventTest;
import org.moditect.jfrunit.JfrEvents;

import static org.assertj.core.api.Assertions.assertThat;

@JfrEventTest
@DomainModel(annotatedClasses = {
		QueryCachePutEventTests.TestEntity.class,
})
@SessionFactory
@ServiceRegistry(
		settings =
				{
						@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
						@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true")
				}
)
public class QueryCachePutEventTests {
	public JfrEvents jfrEvents = new JfrEvents();

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for ( int i = 2; i < 10; i++ ) {
						TestEntity entity = new TestEntity( i, "name_" + i );
						session.persist( entity );
					}
				}
		);
	}

	@Test
	@EnableEvent(JdbcBatchExecutionEvent.NAME)
	public void testCachePutEvent(SessionFactoryScope scope) {
		jfrEvents.reset();
		scope.inTransaction(
				session -> {
					List<TestEntity> dogs = session.createQuery(
							"from TestEntity",
							TestEntity.class
					).setCacheable( true ).getResultList();

					final List<RecordedEvent> events = jfrEvents.events()
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
					assertThat( event.getBoolean( "cacheChanged" ) ).isTrue();
					assertThat( event.getString( "regionName" ) ).isNotNull();
					assertThat( event.getBoolean( "isNaturalId" ) ).isFalse();
					assertThat( event.getString( "description" ) ).isEqualTo( JfrEventManager.CacheActionDescription.QUERY_RESULT.getText() );
				}
		);

	}

	@Test
	@EnableEvent(JdbcBatchExecutionEvent.NAME)
	public void testJdbcBatchExecutionEventNoFired(SessionFactoryScope scope) {
		jfrEvents.reset();
		scope.inTransaction(
				session -> {
					session.createQuery(
							"from TestEntity",
							TestEntity.class
					).setCacheable( false ).getResultList();
				}
		);
		final List<RecordedEvent> events = jfrEvents.events()
				.filter(
						recordedEvent ->
						{
							String eventName = recordedEvent.getEventType().getName();
							return eventName.equals( CachePutEvent.NAME );
						}
				).toList();

		assertThat( events ).hasSize( 0 );
	}

	@Entity(name = "TestEntity")
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
	}

}
