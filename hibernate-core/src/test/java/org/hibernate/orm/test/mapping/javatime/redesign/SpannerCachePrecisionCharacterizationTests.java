package org.hibernate.orm.test.mapping.javatime.redesign;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Independent storage observations for the exact COLUMN primaries used by the cache
/// regression. These probes do not establish a new precision expectation: that requires
/// evaluating server comparisons and the recorded driver/emulator profile together.
/// The original cache lifecycle assertions remain in {@link TemporalCacheTests}.
///
/// @author Steve Ebersole
@RequiresDialect(SpannerDialect.class)
@RequiresDialect(SpannerPostgreSQLDialect.class)
@ServiceRegistry(settings = {
		@Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"),
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "false")
})
@DomainModel(annotatedClasses = TemporalCacheTests.Event.class)
@SessionFactory
class SpannerCachePrecisionCharacterizationTests {
	static Stream<String> inputs() {
		return Stream.of( null, "2026-03-08T06:59:59.123456789Z", "2026-03-08T06:59:59.123456Z",
				"2026-03-08T06:59:59.123456499Z", "2026-03-08T06:59:59.123456500Z",
				"2026-03-08T06:59:59.123456501Z", "2026-03-08T06:59:59.999999900Z",
				"2026-03-08T23:59:59.999999900Z" );
	}

	@ParameterizedTest
	@MethodSource("inputs")
	void ormStorage(String text, DomainModelScope model, SessionFactoryScope scope) {
		characterize( text, model, scope, false );
	}

	@ParameterizedTest
	@MethodSource("inputs")
	void jdbcStorage(String text, DomainModelScope model, SessionFactoryScope scope) {
		characterize( text, model, scope, true );
	}

	private void characterize(String text, DomainModelScope model, SessionFactoryScope scope, boolean rawJdbc) {
		assertThat( scope.getSessionFactory().getSessionFactoryOptions().isTemporalPreservationEnabled() ).isTrue();
		final var value = text == null ? null : Instant.parse( text );
		final var row = new TemporalCacheTests.Event(); row.id = 1L;
		// Seed valid zone/offset companions; raw JDBC subsequently replaces only the primaries.
		final var seed = value == null ? null : rawJdbc ? value.truncatedTo( ChronoUnit.SECONDS ).minusSeconds( 1 ) : value;
		row.adjusted = row.unadjusted = seed == null ? null : seed.atZone( ZoneId.of( "Europe/Paris" ) );
		final var metadata = model.getDomainModel();
		final var entity = metadata.getEntityBinding( TemporalCacheTests.Event.class.getName() );
		final var dialect = metadata.getDatabase().getDialect();
		final String table = entity.getTable().getQuotedName( dialect );
		final String sqlType = dialect instanceof SpannerPostgreSQLDialect ? "timestamp with time zone" : "timestamp";
		try {
			scope.inTransaction( session -> session.persist( row ) );
			scope.inTransaction( session -> session.doWork( connection -> {
				final var info = connection.getMetaData();
				final String url = info.getURL();
				System.out.println( "SPANNER_CACHE_PROFILE dialect=" + dialect.getClass().getName()
						+ " driver=" + info.getDriverName() + "/" + info.getDriverVersion()
						+ " database=" + info.getDatabaseProductVersion()
						+ " autoConfigEmulator=" + (url != null && url.contains( "autoConfigEmulator=true" ))
						+ " emulatorHostConfigured=" + (System.getenv( "SPANNER_EMULATOR_HOST" ) != null)
						+ " jvmZone=" + ZoneId.systemDefault() + " jdbcZone="
						+ scope.getSessionFactory().getSessionFactoryOptions().getJdbcTimeZone()
						+ " sessionZone=not independently queried input=" + text + " rawJdbc=" + rawJdbc );
				for ( String property : List.of( "adjusted", "unadjusted" ) ) {
					final var component = (Component) entity.getProperty( property ).getValue();
					final var primary = (BasicValue) component.getProperty( "instant" ).getValue();
					final var mapping = primary.resolve().getJdbcMapping();
					final var column = primary.getColumns().get( 0 );
					final String name = column.getQuotedName( dialect );
					System.out.println( "SPANNER_CACHE_MAPPING property=" + property + " java="
							+ mapping.getJdbcJavaType().getClass().getName() + " jdbc=" + mapping.getJdbcType().getClass().getName()
							+ " converter=" + mapping.getValueConverter() + " sqlCode=" + mapping.getJdbcType().getDefaultSqlTypeCode()
							+ " ddl=" + column.getSqlType( metadata ) + " precision=" + column.getColumnSize( dialect, metadata ).getPrecision() );
					if ( rawJdbc ) {
						try ( var statement = connection.prepareStatement( "update " + table + " set " + name + "=? where id=1" ) ) {
							mapping.getJdbcValueBinder().bind( statement, value, 1, (SharedSessionContractImplementor) session );
							assertThat( statement.executeUpdate() ).isEqualTo( 1 );
						}
					}
					observe( connection, table, name, sqlType, mapping, (SharedSessionContractImplementor) session, value );
				}
			} ) );
			scope.inTransaction( session -> {
				final var loaded = session.find( TemporalCacheTests.Event.class, 1L );
				System.out.println( "SPANNER_CACHE_ORM rawJdbc=" + rawJdbc + " input=" + text
						+ " adjusted=" + loaded.adjusted + " unadjusted=" + loaded.unadjusted );
				if ( value == null ) {
					assertThat( loaded.adjusted ).isNull();
					assertThat( loaded.unadjusted ).isNull();
				}
			} );
		}
		finally { scope.dropData(); }
	}

	private static void observe(Connection connection, String table, String column, String sqlType,
			JdbcMapping mapping, SharedSessionContractImplementor session, Instant input) throws SQLException {
		try ( var statement = connection.createStatement(); var rows = statement.executeQuery(
				"select " + column + " from " + table + " where id=1" ) ) {
			assertThat( rows.next() ).isTrue();
			final var timestamp = rows.getTimestamp( 1, Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ) );
			final var extracted = mapping.getJdbcValueExtractor().extract( rows, 1, session );
			System.out.println( "SPANNER_CACHE_JDBC column=" + column + " text=" + rows.getString( 1 )
					+ " timestamp=" + (timestamp == null ? null : timestamp.toInstant()) + " extracted=" + extracted
					+ " physicalType=" + rows.getMetaData().getColumnTypeName( 1 )
					+ " precision=" + rows.getMetaData().getPrecision( 1 ) + " scale=" + rows.getMetaData().getScale( 1 ) );
			assertThat( extracted ).isEqualTo( timestamp == null ? null : timestamp.toInstant() );
			if ( input == null ) assertThat( timestamp ).isNull();
		}
		if ( input != null ) {
			for ( Instant candidate : List.of( input, input.truncatedTo( ChronoUnit.MICROS ),
					input.plusNanos( 500 ).truncatedTo( ChronoUnit.MICROS ) ) ) {
				// Text parameters exercise server conversion independently of Hibernate's timestamp binder.
				try ( var statement = connection.prepareStatement( "select " + column + "=cast(? as " + sqlType
						+ "), cast(? as " + sqlType + ") from " + table + " where id=1" ) ) {
					statement.setString( 1, candidate.toString() );
					statement.setString( 2, candidate.toString() );
					try ( var rows = statement.executeQuery() ) {
						assertThat( rows.next() ).isTrue();
						final Timestamp converted = rows.getTimestamp( 2, Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ) );
						System.out.println( "SPANNER_CACHE_SERVER column=" + column + " candidate=" + candidate
								+ " equal=" + rows.getBoolean( 1 ) + " converted=" + converted.toInstant() );
					}
				}
			}
		}
	}
}
