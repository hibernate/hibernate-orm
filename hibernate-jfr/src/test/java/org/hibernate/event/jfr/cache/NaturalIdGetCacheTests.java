package org.hibernate.event.jfr.cache;

import java.util.List;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.event.jfr.internal.CacheGetEvent;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
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
		NaturalIdGetCacheTests.TestEntity.class
})
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
		}
)
public class NaturalIdGetCacheTests {

	public JfrEvents jfrEvents = new JfrEvents();

	@Test
	@EnableEvent(CacheGetEvent.NAME)
	public void testCacheGetEvent(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity testEntity = new TestEntity( 2, 4, 5 );
					session.persist( testEntity );
				}
		);

		scope.inTransaction(
				session -> {
					jfrEvents.reset();
					TestEntity load = session.byNaturalId( TestEntity.class )
							.using( "code", 4 )
							.using( "item", 5 )
							.load();
					List<RecordedEvent> events = jfrEvents.events()
							.filter(
									recordedEvent ->
									{
										String eventName = recordedEvent.getEventType().getName();
										return eventName.equals( CacheGetEvent.NAME );
									}
							).toList();

					assertThat( events ).hasSize( 2 );

					RecordedEvent event = events.get( 0 );
					assertThat( event.getEventType().getName() )
							.isEqualTo( CacheGetEvent.NAME );
					assertThat( event.getLong( "executionTime" ) ).isGreaterThan( 0 );
					assertThat( event.getString( "sessionIdentifier" ) )
							.isEqualTo( session.getSessionIdentifier().toString() );
					assertThat( event.getString( "entityName" ) )
							.isEqualTo( TestEntity.class.getName() );
					assertThat( event.getBoolean( "isNaturalId" ) ).isTrue();
					assertThat( event.getBoolean( "hit" ) ).isTrue();
					assertThat( event.getString( "regionName" ) ).isNotNull();

					event = events.get( 1 );
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


	@Entity(name = "TestEntity")
	@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
	@Cacheable
	@NaturalIdCache
	public static class TestEntity {

		@Id
		private Integer id;

		@NaturalId
		private Integer code;

		@NaturalId
		private Integer item;

		private String description = "A description ...";

		protected TestEntity() {
		}

		public TestEntity(Integer id, Integer code, Integer item) {
			this.id = id;
			this.code = code;
			this.item = item;
		}
	}
}
