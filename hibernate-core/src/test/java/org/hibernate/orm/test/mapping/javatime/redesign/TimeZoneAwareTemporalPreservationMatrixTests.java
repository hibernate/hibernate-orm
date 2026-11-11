package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.*;
import java.util.stream.Stream;

import org.hibernate.MappingException;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation;
import org.hibernate.dialect.type.internal.PostgreSQLOffsetTimeJdbcType;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.testing.orm.junit.*;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.*;
import org.hibernate.type.descriptor.jdbc.*;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimeJdbcType;
import org.hibernate.type.internal.TemporalMappingValidator;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.internal.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Checks COLUMN decomposition/JDBC/reconstruction against independently expected
/// preservation facts. Direct-enabled fallback is selected explicitly; the legacy
/// direct-disabled compatibility path is not used to simulate fallback.
/// Unsupported cells must reject validation, never silently skip a round trip.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = @Setting(name = "hibernate.type.java_time_use_direct_jdbc", value = "true"))
@RequiresDialect(org.hibernate.dialect.H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
@DomainModel
@SessionFactory(exportSchema = false)
class TimeZoneAwareTemporalPreservationMatrixTests {
	enum Handling { DIRECT, FALLBACK }
	enum Form { PLAIN, WITH_TIME_ZONE }
	enum Domain { OFFSET_TIME, OFFSET_DATE_TIME, ZONED_DATE_TIME }

	record Combination(Domain domain, Handling handling, Form form, boolean adjusted) {}

	static Stream<Combination> combinations() {
		return Stream.of( Domain.values() ).flatMap( domain -> Stream.of( Handling.values() )
				.flatMap( handling -> Stream.of( Form.values() ).flatMap( form -> Stream.of( false, true )
						.map( adjusted -> new Combination( domain, handling, form, adjusted ) ) ) ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("combinations")
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void columnPreservation(Combination combination, SessionFactoryScope scope) {
		scope.inSession( TemporalMatrixEnvironment::assertSessionTimeZone );
		final var factory = scope.getSessionFactory();
		final var dialect = factory.getJdbcServices().getDialect();
		final boolean time = combination.domain == Domain.OFFSET_TIME;
		final JavaType primaryJava = time ? OffsetTimeJavaType.INSTANCE : InstantJavaType.INSTANCE;
		final JdbcType primary;
		final TemporalPreservation.Level expected;
		if ( time ) {
			if ( combination.handling == Handling.FALLBACK ) {
				primary = TimeUtcAsJdbcTimeJdbcType.INSTANCE;
				expected = TemporalPreservation.Level.UNKNOWN;
			}
			else if ( combination.form == Form.PLAIN && !(dialect instanceof org.hibernate.dialect.PostgreSQLDialect) ) {
				// A zoned typed extractor is not a matching handler for a plain
				// Oracle timestamp (ORA-18716). No supported direct UTC-time
				// descriptor is currently declared for this plain pair.
				primary = LocalTimeJdbcType.INSTANCE;
				expected = TemporalPreservation.Level.UNKNOWN;
			}
			else {
				final var delegate = factory.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( SqlTypes.TIME_UTC );
				primary = dialect instanceof PostgreSQLDialect && combination.form == Form.WITH_TIME_ZONE
						? PostgreSQLOffsetTimeJdbcType.INSTANCE
						: new UtcTimeJdbcType( delegate );
				expected = TemporalPreservation.Level.SUPPORTED;
			}
		}
		else if ( combination.handling == Handling.FALLBACK ) {
			primary = combination.form == Form.PLAIN ? TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE
					: dialect instanceof PostgreSQLDialect
							? org.hibernate.dialect.type.internal.PostgreSQLTimestampUtcAsJdbcTimestampJdbcType.INSTANCE
							: dialect instanceof org.hibernate.dialect.OracleDialect
							? new org.hibernate.dialect.type.internal.OracleTimestampUtcAsJdbcTimestampJdbcType( factory.getServiceRegistry().requireService( org.hibernate.boot.registry.classloading.spi.ClassLoaderService.class ) )
							: TimestampUtcAsZonedJdbcTimestampJdbcType.INSTANCE;
			expected = TemporalPreservation.Level.SUPPORTED;
		}
		else if ( combination.form == Form.PLAIN ) {
			primary = InstantJdbcType.INSTANCE;
			expected = TemporalPreservation.Level.UNKNOWN;
		}
		else {
			// Typed OffsetDateTime JDBC access carries the Instant primary without
			// TimestampUtcAsInstant's exception-triggered java.sql fallback.
			primary = TimestampUtcAsOffsetDateTimeJdbcType.INSTANCE;
			expected = TemporalPreservation.Level.SUPPORTED;
		}
		final var part = time ? TemporalPreservation.TemporalPart.TIME : TemporalPreservation.TemporalPart.INSTANT;
		final var irrelevant = TemporalPreservation.Level.IRRELEVANT;
		final var expectedOffset = !time ? irrelevant : expected == TemporalPreservation.Level.UNKNOWN
				? TemporalPreservation.Level.UNKNOWN : primary instanceof UtcTimeJdbcType
						? TemporalPreservation.Level.UNSUPPORTED : TemporalPreservation.Level.SUPPORTED;
		// The established UTC carriers explicitly report normalized clock preservation.
		// Other pairs retain the report's UNKNOWN default, even when TIME + OFFSET
		// can independently establish normalized preservation via preserves().
		final var expectedNormalizedTime = time && expected == TemporalPreservation.Level.SUPPORTED
				&& primary instanceof UtcTimeJdbcType
				? TemporalPreservation.Level.SUPPORTED : TemporalPreservation.Level.UNKNOWN;
		assertThat( dialect.getTemporalPreservation( primaryJava, primary ) ).as( "%s complete primary report",
						combination )
				.isEqualTo( new TemporalPreservation( irrelevant, time ? expected : irrelevant,
						time ? irrelevant : expected, expectedOffset, irrelevant, expectedNormalizedTime ) );
		assertThat( dialect.getTemporalPreservation( primaryJava, primary ).preserves( part ) )
				.as( "%s primary report", combination ).isEqualTo( expected );
		assertThat( dialect.getTemporalPreservation( ZoneOffsetJavaType.INSTANCE, IntegerJdbcType.INSTANCE ).offset() )
				.isEqualTo( TemporalPreservation.Level.SUPPORTED );
		if ( expected != TemporalPreservation.Level.SUPPORTED ) {
			assertThatThrownBy( () -> TemporalMappingValidator.validate( dialect, primaryJava, primary,
					ZoneOffset.UTC, combination.toString() ) ).isInstanceOf( MappingException.class ).hasMessageContaining( part.name() );
			return;
		}
		TemporalMappingValidator.validate( dialect, primaryJava, primary, time ? ZoneOffset.UTC : null,
				combination.toString(), time );
		final CompositeUserType delegate = switch ( combination.domain ) {
			case OFFSET_TIME -> new OffsetTimeCompositeUserType();
			case OFFSET_DATE_TIME -> new OffsetDateTimeCompositeUserType();
			case ZONED_DATE_TIME -> new ZonedDateTimeCompositeUserType();
		};
		final JavaType domainJava = factory.getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( delegate.returnedClass() );
		final ZoneId target = !combination.adjusted ? null : time ? ZoneOffset.ofHours( -3 ) : ZoneId.of( "America/New_York" );
		final var composite = new ContextualTemporalCompositeUserType( delegate, domainJava, target, combination.toString() );
		final String sqlType = (time && !(dialect instanceof OracleDialect) ? "time(6)" : "timestamp(6)")
				+ (combination.form == Form.WITH_TIME_ZONE ? " with time zone" : "");
		final String table = "preservation_" + combination.domain.ordinal() + combination.handling.ordinal() + combination.form.ordinal() + (
				combination.adjusted ? "a" : "n");
		final String sql = "insert into " + table + " (primary_value, offset_value) values (?, ?)";
		final Object[] inputs = time
				? new Object[] { OffsetTime.parse( "00:15:16.123456+05:30:45" ), OffsetTime.parse( "23:45:16.654321-03:00" ), null }
				: new Object[] { ZonedDateTime.parse( "2026-11-01T01:30:16.123456-04:00[America/New_York]" ),
						ZonedDateTime.parse( "2026-11-01T01:30:16.654321-05:00[America/New_York]" ),
						ZonedDateTime.parse( "2026-07-01T12:00:00.123456+05:30:45" ), null };
		scope.inSession( session -> session.doWork( connection -> {
			try ( var ddl = connection.createStatement() ) {
				ddl.execute( "create table " + table + " (primary_value " + sqlType + ", offset_value integer)" );
			}
			try ( var statement = connection.prepareStatement( sql ); var read = connection.createStatement() ) {
				for ( Object sample : inputs ) {
					final Object input = sample instanceof ZonedDateTime z && combination.domain == Domain.OFFSET_DATE_TIME ? z.toOffsetDateTime() : sample;
					primary.getBinder( primaryJava ).bind( statement, composite.getPropertyValue( input, 0 ), 1, session );
					IntegerJdbcType.INSTANCE.getBinder( ZoneOffsetJavaType.INSTANCE ).bind( statement,
							(ZoneOffset) composite.getPropertyValue( input, 1 ), 2, session );
					statement.executeUpdate();
					try ( var result = read.executeQuery( "select primary_value, offset_value from " + table ) ) {
						assertThat( result.next() ).isTrue();
						final Object[] tuple = { primary.getExtractor( primaryJava ).extract( result, 1, session ),
								IntegerJdbcType.INSTANCE.getExtractor( ZoneOffsetJavaType.INSTANCE ).extract( result, 2, session ) };
						final Object actual = composite.instantiate( (ValueAccess) () -> tuple );
						if ( input == null ) assertThat( actual ).isNull();
						else if ( input instanceof OffsetTime t ) {
							assertThat( actual ).isEqualTo( target == null ? t : t.withOffsetSameInstant( (ZoneOffset) target ) );
						}
						else if ( input instanceof OffsetDateTime t ) {
							final var value = (OffsetDateTime) actual;
							assertThat( value.toInstant() ).isEqualTo( t.toInstant() );
							assertThat( value.getOffset() ).isEqualTo( target == null ? t.getOffset() : t.atZoneSameInstant( target ).getOffset() );
						}
						else {
							final var t = (ZonedDateTime) input;
							final var value = (ZonedDateTime) actual;
							assertThat( value.toInstant() ).isEqualTo( t.toInstant() );
							assertThat( value.getZone() ).isEqualTo( target == null ? t.getOffset() : target );
							assertThat( value.getOffset() ).isEqualTo( target == null ? t.getOffset() : t.withZoneSameInstant( target ).getOffset() );
						}
					}
					read.executeUpdate( "delete from " + table );
				}
			}
			finally {
				try ( var ddl = connection.createStatement() ) {
					ddl.execute( "drop table " + table );
				}
			}
		} ) );
	}
}
