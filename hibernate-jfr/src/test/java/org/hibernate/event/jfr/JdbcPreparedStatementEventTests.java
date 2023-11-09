package org.hibernate.event.jfr;

import java.util.List;
import java.util.Locale;

import org.hibernate.event.jfr.internal.JdbcPreparedStatementCreationEvent;
import org.hibernate.event.jfr.internal.JdbcPreparedStatementExecutionEvent;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jdk.jfr.consumer.RecordedEvent;
import org.moditect.jfrunit.EnableEvent;
import org.moditect.jfrunit.JfrEventTest;
import org.moditect.jfrunit.JfrEvents;

import static org.assertj.core.api.Assertions.assertThat;

@JfrEventTest
@DomainModel(annotatedClasses = JdbcPreparedStatementEventTests.TestEntity.class)
@SessionFactory
public class JdbcPreparedStatementEventTests {
	public JfrEvents jfrEvents = new JfrEvents();

	@Test
	@EnableEvent(JdbcPreparedStatementCreationEvent.NAME)
	@EnableEvent(JdbcPreparedStatementExecutionEvent.NAME)
	public void testJdbcPreparedStatementEvent(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select t from TestEntity t" ).list();
					final List<RecordedEvent> events = jfrEvents.events()
							.filter(
									recordedEvent ->
									{
										String eventName = recordedEvent.getEventType().getName();
										return eventName.equals( JdbcPreparedStatementCreationEvent.NAME )
												|| eventName.equals( JdbcPreparedStatementExecutionEvent.NAME );
									}
							).toList();
					assertThat( events ).hasSize( 2 );

					RecordedEvent preparedStatementCreationEvent = events.get( 0 );
					assertThat( preparedStatementCreationEvent.getEventType().getName() )
							.isEqualTo( JdbcPreparedStatementCreationEvent.NAME );
					assertThat( preparedStatementCreationEvent.getLong( "executionTime" ) ).isGreaterThan( 0 );
					assertThat( preparedStatementCreationEvent.getString( "sql" ).toLowerCase( Locale.ROOT ) )
							.contains( "select " );

					RecordedEvent preparedStatementExecutionEvent = events.get( 1 );
					assertThat( preparedStatementExecutionEvent.getEventType().getName() )
							.isEqualTo( JdbcPreparedStatementExecutionEvent.NAME );
					assertThat( preparedStatementExecutionEvent.getLong( "executionTime" ) ).isGreaterThan( 0 );
					assertThat( preparedStatementExecutionEvent.getString( "sql" ) )
							.isEqualTo( preparedStatementCreationEvent.getString( "sql" ) );
				}
		);

	}

	@Test
	@EnableEvent(JdbcPreparedStatementCreationEvent.NAME)
	@EnableEvent(JdbcPreparedStatementExecutionEvent.NAME)
	public void testJdbcPreparedStatementEventNoFired(SessionFactoryScope scope) {
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
							return eventName.equals( JdbcPreparedStatementCreationEvent.NAME )
									|| eventName.equals( JdbcPreparedStatementExecutionEvent.NAME );
						}
				).toList();

		assertThat( events ).hasSize( 0 );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Long id;

		private String name;
	}
}
