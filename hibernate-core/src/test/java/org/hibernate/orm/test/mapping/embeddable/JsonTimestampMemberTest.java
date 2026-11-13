package org.hibernate.orm.test.mapping.embeddable;

import java.time.LocalDateTime;
import java.util.TimeZone;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@RequiresDialect(H2Dialect.class)
@JiraKey("HHH-21146")
@ResourceLock(Resources.TIME_ZONE)
class JsonTimestampMemberTest {
	private static TimeZone previous;

	@BeforeAll
	static void setTimeZone() {
		previous = TimeZone.getDefault();
		TimeZone.setDefault( TimeZone.getTimeZone( "Europe/Madrid" ) );
	}

	@AfterAll
	static void restoreTimeZone() {
		TimeZone.setDefault( previous );
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"UTC", "Asia/Tokyo"})
	@DomainModel(annotatedClasses = Payload.class)
	@SessionFactory
	void timestampMemberAccess(String zone, SessionFactoryScope scope) {
		final var expected = LocalDateTime.parse( "2024-05-17T10:15:30.123456" );
		scope.inTransaction( session -> session.createNativeMutationQuery( """
				insert into Payload(id,data) values (1, JSON '{"stamp":"2024-05-17T08:15:30.123456"}')
				""" ).executeUpdate() );
		final var builder = scope.getSessionFactory().withOptions();
		if ( zone != null ) {
			builder.jdbcTimeZone( TimeZone.getTimeZone( zone ) );
		}
		try ( var session = builder.openSession() ) {
			session.inTransaction( tx -> {
				assertThat( session.find( Payload.class, 1L ).data.stamp ).isEqualTo( expected );
				final var row = session.createSelectionQuery(
						"select p.data.stamp, hour(p.data.stamp) from Payload p where p.data.stamp = :stamp",
						Object[].class ).setParameter( "stamp", expected ).getSingleResult();
				assertThat( row ).containsExactly( expected, 10 );
				assertThat( session.createMutationQuery( """
						update Payload p set p.data.stamp = :stamp
						where p.data.stamp = datetime 2024-05-17 10:15:30.123456
						""" ).setParameter( "stamp", expected.plusDays( 1 ) ).executeUpdate() ).isOne();
				session.clear();
				assertThat( session.find( Payload.class, 1L ).data.stamp ).isEqualTo( expected.plusDays( 1 ) );
				assertThat( session.createMutationQuery( """
						update Payload p set p.data.stamp = datetime 2024-12-17 10:15:30.123456
						where p.data.stamp = :stamp
						""" ).setParameter( "stamp", expected.plusDays( 1 ) ).executeUpdate() ).isOne();
			} );
		}
		scope.inTransaction( session -> {
			assertThat( session.find( Payload.class, 1L ).data.stamp ).isEqualTo( expected.withMonth( 12 ) );
			session.createMutationQuery( "delete from Payload" ).executeUpdate();
		} );
	}

	@Entity(name = "Payload")
	static class Payload {
		@Id long id;
		@JdbcTypeCode(SqlTypes.JSON) Stamp data;
	}

	@Embeddable
	static class Stamp {
		@JdbcTypeCode(SqlTypes.TIMESTAMP) LocalDateTime stamp;
	}
}
