package org.hibernate.orm.test.mapping.embeddable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.MappingSettings;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.type.internal.OracleJsonJdbcType;
import org.hibernate.dialect.type.internal.OracleLocalTimeJdbcType;
import org.hibernate.dialect.type.spi.OracleJdbcTypes;
import org.hibernate.orm.test.mapping.embeddable.OracleLocalTimeAggregateTestSupport.TimeConverter;
import org.hibernate.orm.test.mapping.embeddable.OracleLocalTimeAggregateTestSupport.TimeValue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.LocalTimeJavaType;
import org.hibernate.type.internal.BasicTypeImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Whole-value and member operations must agree for native OSON and its text fallback.
/// Explicit relational handlers keep the legacy control focused on representation.
///
/// @author Steve Ebersole
@RequiresDialect(value = OracleDialect.class, majorVersion = 21)
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"))
@DomainModel(
		annotatedClasses = OracleJsonTemporalRepresentationTests.JsonRow.class,
		typeContributors = OracleAggregateClockTypeContributor.class
)
@SessionFactory
class OracleJsonTemporalRepresentationTests {
	// Match the fixture contribution used for legacy parameter resolution.
	private static final BasicTypeImpl<LocalTime> TIME_PARAMETER =
			new BasicTypeImpl<>( LocalTimeJavaType.INSTANCE, OracleLocalTimeJdbcType.INSTANCE );
	boolean osonDisabled() {
		return false;
	}
	boolean preservationEnabled() {
		return true;
	}

	@Test
	void wholeAndMemberPathsAgree(SessionFactoryScope scope) {
		final var factory = scope.getSessionFactory();
		assertThat( factory.getSessionFactoryOptions().isTemporalPreservationEnabled() ).isEqualTo( preservationEnabled() );
		final boolean oson = !osonDisabled() && OracleJdbcTypes.isOsonAvailable( factory.getServiceRegistry() );
		final var jdbc = factory.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( SqlTypes.JSON );
		assertThat( jdbc ).isInstanceOf( OracleJsonJdbcType.class );
		assertThat( ((OracleJsonJdbcType) jdbc).usesOsonTemporalValues() ).isEqualTo( oson );
		assertThat( jdbc.getDdlTypeCode() ).isEqualTo( SqlTypes.JSON );
		final var data = new Payload();
		data.clock = new Clock();
		data.clock.time = LocalTime.of( 12, 34, 56, 123456789 );
		data.clock.converted = new TimeValue( LocalTime.of( 23, 59, 59, 987654321 ) );
		data.stamp = LocalDateTime.of( 2024, 2, 29, 12, 34, 56, 123456789 );
		data.day = LocalDate.of( 2024, 2, 29 );
		data.instant = Instant.parse( "2024-02-29T12:34:56.123456789Z" );
		final var row = new JsonRow();
		row.id = 1;
		row.payload = data;
		try {
			scope.inTransaction( session -> session.persist( row ) );
			check( scope, data, data.clock.time, data.clock.converted );
			final var next = LocalTime.of( 1, 2, 3, 456789123 );
			final var converted = new TimeValue( LocalTime.of( 4, 5, 6, 789123456 ) );
			scope.inTransaction( session -> assertThat( session.createMutationQuery(
					"update JsonTemporalRow e set e.payload.clock.time=:t,e.payload.clock.converted=:c where e.id=1" )
					.setParameter( "t", next, TIME_PARAMETER ).setParameter( "c", converted ).executeUpdate() ).isEqualTo( 1 ) );
			check( scope, data, next, converted );
			scope.inTransaction( session -> assertThat( session.createMutationQuery(
					"update JsonTemporalRow e set e.payload.clock.time=null,e.payload.clock.converted=null where e.id=1" )
					.executeUpdate() ).isEqualTo( 1 ) );
			check( scope, data, null, null );
		}
		finally {
			scope.dropData();
		}
	}

	private static void check(SessionFactoryScope scope, Payload expected, LocalTime time, TimeValue converted) {
		scope.inTransaction( session -> {
			final var loaded = session.find( JsonRow.class, 1 ).payload;
			assertThat( loaded.clock == null ? null : loaded.clock.time ).isEqualTo( time );
			assertThat( loaded.clock == null ? null : loaded.clock.converted ).isEqualTo( converted );
			assertThat( loaded.stamp ).isEqualTo( expected.stamp );
			assertThat( loaded.day ).isEqualTo( expected.day );
			assertThat( loaded.instant ).isEqualTo( expected.instant );
		} );
		scope.inTransaction( session -> assertThat( session.createSelectionQuery(
				"select e.payload.clock.time,e.payload.clock.converted,e.payload.stamp,e.payload.day,e.payload.instant from JsonTemporalRow e", Object[].class )
				.getSingleResult() ).containsExactly( time, converted, expected.stamp, expected.day, expected.instant ) );
		scope.inTransaction( session -> {
			final var clock = session.createSelectionQuery(
					"select e.payload.clock from JsonTemporalRow e", Clock.class ).getSingleResult();
			assertThat( clock == null ? null : clock.time ).isEqualTo( time );
			assertThat( clock == null ? null : clock.converted ).isEqualTo( converted );
		} );
		if ( time != null ) {
			scope.inTransaction( session -> assertThat( session.createSelectionQuery(
					"select e.id from JsonTemporalRow e where e.payload.clock.time=:t and e.payload.clock.converted=:c", Integer.class )
					.setParameter( "t", time, TIME_PARAMETER ).setParameter( "c", converted ).getResultList() ).containsExactly( 1 ) );
		}
	}

	@Entity(name = "JsonTemporalRow")
	static class JsonRow {
		@Id
		int id;
		@JdbcTypeCode(SqlTypes.JSON)
		Payload payload;
	}
	@Embeddable
	static class Payload {
		@JdbcTypeCode(SqlTypes.JSON)
		Clock clock;
		@Column(secondPrecision = 9)
		LocalDateTime stamp;
		LocalDate day;
		@Column(secondPrecision = 9)
		Instant instant;
	}
	@Embeddable
	static class Clock {
		@Column(secondPrecision = 9)
		@JdbcType(OracleLocalTimeJdbcType.class)
		LocalTime time;
		@Column(secondPrecision = 9)
		@JdbcType(OracleLocalTimeJdbcType.class)
		@Convert(converter = TimeConverter.class)
		TimeValue converted;
	}
}
