package org.hibernate.event.jfr;

import java.util.List;
import java.util.Locale;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.event.jfr.internal.JdbcBatchExecutionEvent;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
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
		JdbcBatchExecutionEventTests.TestEntity.class
})
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "5")
)
public class JdbcBatchExecutionEventTests {
	public JfrEvents jfrEvents = new JfrEvents();


	@Test
	@EnableEvent(JdbcBatchExecutionEvent.NAME)
	public void testJdbcBatchExecutionEvent(SessionFactoryScope scope) {
		jfrEvents.reset();
		scope.inTransaction(
				session -> {
					for ( int i = 2; i < 10; i++ ) {
						TestEntity entity = new TestEntity( i, "name_" + i );
						session.persist( entity );
					}
					session.flush();
					final List<RecordedEvent> events = jfrEvents.events()
							.filter(
									recordedEvent ->
									{
										String eventName = recordedEvent.getEventType().getName();
										return eventName.equals( JdbcBatchExecutionEvent.NAME );
									}
							).toList();
					assertThat( events ).hasSize( 2 );

					RecordedEvent jdbcBatchExecutionEvent = events.get( 0 );
					assertThat( jdbcBatchExecutionEvent.getEventType().getName() )
							.isEqualTo( JdbcBatchExecutionEvent.NAME );
					assertThat( jdbcBatchExecutionEvent.getLong( "executionTime" ) ).isGreaterThan( 0 );
					assertThat( jdbcBatchExecutionEvent.getString( "sql" ).toLowerCase( Locale.ROOT ) )
							.contains( "insert into " );

					jdbcBatchExecutionEvent = events.get( 1 );
					assertThat( jdbcBatchExecutionEvent.getEventType().getName() )
							.isEqualTo( JdbcBatchExecutionEvent.NAME );
					assertThat( jdbcBatchExecutionEvent.getLong( "executionTime" ) ).isGreaterThan( 0 );
					assertThat( jdbcBatchExecutionEvent.getString( "sql" ).toLowerCase( Locale.ROOT ) )
							.contains( "insert into " );
				}
		);

	}

	@Test
	@EnableEvent(JdbcBatchExecutionEvent.NAME)
	public void testJdbcBatchExecutionEventNoFired(SessionFactoryScope scope) {
		jfrEvents.reset();
		scope.inTransaction(
				session -> {
				}
		);
		final List<RecordedEvent> events = jfrEvents.events()
				.filter(
						recordedEvent ->
						{
							String eventName = recordedEvent.getEventType().getName();
							return eventName.equals( JdbcBatchExecutionEvent.NAME );
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
