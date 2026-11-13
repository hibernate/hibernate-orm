package org.hibernate.orm.test.mapping.embeddable;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.TimeZone;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = JsonTemporalValuesTest.Payload.class)
@SessionFactory
@RequiresDialect(H2Dialect.class)
@ResourceLock(Resources.TIME_ZONE)
class JsonTemporalValuesTest {
	@Test
	void fractionalTime(SessionFactoryScope scope) {
		final var time = LocalTime.parse( "12:34:56.789" );
		scope.inTransaction( session -> {
			session.createNativeMutationQuery( """
					insert into Payload(id,data) values (1, JSON '{"tod":"12:34:56.789"}')
					""" ).executeUpdate();
			final var payload = new Payload();
			payload.id = 2;
			payload.data = new Stamp();
			payload.data.tod = time;
			session.persist( payload );
		} );
		scope.inTransaction( session -> {
			assertThat( session.find( Payload.class, 1L ).data.tod ).isEqualTo( time );
			assertThat( session.find( Payload.class, 2L ).data.tod ).isEqualTo( time );
		} );
	}

	@Test
	void timestampMemberAccess(SessionFactoryScope scope) {
		final var previous = TimeZone.getDefault();
		TimeZone.setDefault( TimeZone.getTimeZone( "Europe/Madrid" ) );
		try {
			final var stamp = LocalDateTime.parse( "2024-05-17T10:15:30.123456" );
			scope.inTransaction( session -> session.createNativeMutationQuery( """
					insert into Payload(id,data) values (3, JSON '{"stamp":"2024-05-17T08:15:30.123456"}')
					""" ).executeUpdate() );
			scope.inTransaction( session -> {
				assertThat( session.find( Payload.class, 3L ).data.stamp ).isEqualTo( stamp );
				assertThat( session.createSelectionQuery(
						"select p.data.stamp from Payload p where p.id = 3", LocalDateTime.class )
						.getSingleResult() ).isEqualTo( stamp );
				session.createMutationQuery( "update Payload p set p.data.stamp = :stamp where p.id = 3" )
						.setParameter( "stamp", stamp.plusDays( 1 ) ).executeUpdate();
			} );
			scope.inTransaction( session -> assertThat( session.find( Payload.class, 3L ).data.stamp )
					.isEqualTo( stamp.plusDays( 1 ) ) );
		}
		finally {
			TimeZone.setDefault( previous );
		}
	}

	@Entity(name = "Payload")
	static class Payload {
		@Id long id;
		@JdbcTypeCode(SqlTypes.JSON) Stamp data;
	}

	@Embeddable
	static class Stamp {
		@JdbcTypeCode(SqlTypes.TIME) LocalTime tod;
		@JdbcTypeCode(SqlTypes.TIMESTAMP) LocalDateTime stamp;
	}
}
