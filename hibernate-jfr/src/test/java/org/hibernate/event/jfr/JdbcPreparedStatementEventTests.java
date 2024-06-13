package org.hibernate.event.jfr;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.event.jfr.internal.JdbcPreparedStatementCreationEvent;
import org.hibernate.event.jfr.internal.JdbcPreparedStatementExecutionEvent;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ParameterMode;
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
		jfrEvents.reset();
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
					assertThat( preparedStatementCreationEvent.getDuration() ).isPositive();
					assertThat( preparedStatementCreationEvent.getString( "sql" ).toLowerCase( Locale.ROOT ) )
							.contains( "select " );

					RecordedEvent preparedStatementExecutionEvent = events.get( 1 );
					assertThat( preparedStatementExecutionEvent.getEventType().getName() )
							.isEqualTo( JdbcPreparedStatementExecutionEvent.NAME );
					assertThat( preparedStatementExecutionEvent.getDuration() ).isPositive();
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
					// No-op
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

	@Test
	@RequiresDialect(H2Dialect.class)
	@EnableEvent(JdbcPreparedStatementCreationEvent.NAME)
	@EnableEvent(JdbcPreparedStatementExecutionEvent.NAME)
	public void testJdbcPreparedStatementEventStoredProcedure(SessionFactoryScope scope) {
		jfrEvents.reset();
		scope.inTransaction(
				session -> {
					ProcedureCall call = session.createStoredProcedureCall("DB_OBJECT_SQL", String.class)
							.registerStoredProcedureParameter(1, String.class, ParameterMode.IN)
							.registerStoredProcedureParameter(2, String.class, ParameterMode.IN)
							.setParameter(1, "USER")  
							.setParameter(2, "SA");
					boolean hasResult = call.execute();
					assertThat( hasResult ).isTrue();
					Object createSa = call.getSingleResult();
					assertThat( createSa ).isInstanceOf( String.class );
					assertThat( ((String) createSa).toLowerCase( Locale.ROOT ) ).contains( "create user if not exists " );
					
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
					assertThat( preparedStatementCreationEvent.getDuration() ).isGreaterThan( Duration.ZERO );
					assertThat( preparedStatementCreationEvent.getString( "sql" ).toLowerCase( Locale.ROOT ) )
							.contains( "{call " );

					RecordedEvent preparedStatementExecutionEvent = events.get( 1 );
					assertThat( preparedStatementExecutionEvent.getEventType().getName() )
							.isEqualTo( JdbcPreparedStatementExecutionEvent.NAME );
					assertThat( preparedStatementExecutionEvent.getDuration() ).isGreaterThan( Duration.ZERO );
					assertThat( preparedStatementExecutionEvent.getString( "sql" ) )
							.isEqualTo( preparedStatementCreationEvent.getString( "sql" ) );
				}
		);

	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Long id;

		private String name;
	}
}
