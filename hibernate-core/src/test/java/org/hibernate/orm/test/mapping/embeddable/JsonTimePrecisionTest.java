package org.hibernate.orm.test.mapping.embeddable;

import java.time.LocalTime;
import jakarta.persistence.Column;
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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = JsonTimePrecisionTest.Payload.class)
@SessionFactory
@RequiresDialect(H2Dialect.class)
@JiraKey("HHH-21146")
class JsonTimePrecisionTest {
	@Test
	void wholeDocumentAndMemberProjection(SessionFactoryScope scope) {
		final var value = LocalTime.parse( "12:34:56.789" );
		scope.inTransaction( session -> {
			final var payload = new Payload();
			payload.id = 1;
			payload.data = new Times();
			payload.data.millis = value;
			payload.data.seconds = value;
			session.persist( payload );
		} );
		scope.inTransaction( session -> {
			final var times = session.find( Payload.class, 1L ).data;
			assertThat( times.millis ).isEqualTo( value );
			assertThat( times.seconds ).isEqualTo( LocalTime.of( 12, 34, 57 ) );
			final var row = session.createSelectionQuery(
					"select p.data.millis, p.data.seconds from TimePayload p where p.id = 1", Object[].class ).getSingleResult();
			assertThat( row ).containsExactly( times.millis, times.seconds );
		} );
	}

	@Test
	void existingDocumentAndPartialUpdate(SessionFactoryScope scope) {
		final var value = LocalTime.parse( "12:34:56.789" );
		scope.inTransaction( session -> session.createNativeMutationQuery( """
				insert into TimePayload(id,data) values
				(2, JSON '{"millis":"12:34:56.789","seconds":"12:34:56.789"}')
				""" ).executeUpdate() );
		scope.inTransaction( session -> {
			final var times = session.find( Payload.class, 2L ).data;
			assertThat( times.millis ).isEqualTo( value );
			assertThat( times.seconds ).isEqualTo( LocalTime.of( 12, 34, 57 ) );
			assertThat( session.createMutationQuery( """
					update TimePayload p set p.data.millis = :time where p.id = 2
					""" ).setParameter( "time", value.plusSeconds( 1 ) ).executeUpdate() ).isOne();
		} );
		scope.inTransaction( session -> assertThat( session.find( Payload.class, 2L ).data.millis )
				.isEqualTo( value.plusSeconds( 1 ) ) );
	}

	@Entity(name = "TimePayload")
	static class Payload {
		@Id long id;
		@JdbcTypeCode(SqlTypes.JSON) Times data;
	}

	@Embeddable
	static class Times {
		@Column(secondPrecision = 3)
		@JdbcTypeCode(SqlTypes.TIME)
		LocalTime millis;
		@JdbcTypeCode(SqlTypes.TIME)
		LocalTime seconds;
	}
}
