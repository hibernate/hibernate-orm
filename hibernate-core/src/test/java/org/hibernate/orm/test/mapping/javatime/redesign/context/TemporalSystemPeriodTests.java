package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Temporal;
import org.hibernate.mapping.BasicValue;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.dialect.H2Dialect;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Explicit compatibility mode retains the legacy period clock protocol. Enabled
/// controls still validate precision and application mappings without boot metadata.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalSystemPeriodTests {
	private static final Instant FIRST = Instant.parse( "2026-01-15T12:00:00Z" );
	private static final Instant SECOND = Instant.parse( "2026-07-15T12:00:00Z" );

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void boundHistoryRetainsLegacyClockFields(boolean metadata) {
		// The explicit compatibility mode overrides either deprecated preference.
		for ( boolean direct : List.of( true, false ) ) {
			for ( String strategy : List.of( "SINGLE_TABLE", "HISTORY_TABLE" ) ) {
				final var supplier = new Fixed();
				try ( var scope = ContextScope.open( metadata, direct, Map.of(
						"hibernate.type.temporal_preservation", false,
						"hibernate.temporal.table_strategy", strategy,
						"hibernate.temporal.changeset_id_supplier", supplier ), List.of(), Period.class ) ) {
					final var model = scope.metadata.getEntityBinding( Period.class.getName() );
					final String table = model.getAuxiliaryTable().getName();
					final var start = (BasicValue) model.getAuxiliaryTable().getColumn( new org.hibernate.mapping.Column( "valid_from" ) ).getValue();
					assertThat( start.resolve().getJdbcType() ).isExactlyInstanceOf( TimestampJdbcType.class );
					assertThat( start.copy().resolve().getJdbcType() ).isExactlyInstanceOf( TimestampJdbcType.class );
					scope.tx( s -> {
						final var row = new Period(); row.id = 1; row.payload = "v1"; row.tags.add( "first" ); s.persist( row );
					} );
					supplier.value = SECOND;
					scope.tx( s -> { final var row = s.find( Period.class, 1 ); row.payload = "v2"; row.tags.add( "second" ); } );
					try ( var s = scope.factory.withOptions().asOf( FIRST ).open() ) {
						s.inTransaction( tx -> {
							assertThat( s.find( Period.class, 1 ).payload ).isEqualTo( "v1" );
							assertThat( s.find( Period.class, 1 ).tags ).containsExactly( "first" );
						} );
					}
					scope.tx( s -> s.doWork( connection -> {
						try ( var statement = connection.createStatement(); var rows = statement.executeQuery(
								"select valid_from from " + table + " order by valid_from" ) ) {
							for ( var expected : List.of( FIRST, SECOND ) ) {
								assertThat( rows.next() ).isTrue();
								assertThat( rows.getTimestamp( 1 ).toInstant() ).isEqualTo( expected );
								assertThat( rows.getObject( 1, LocalDateTime.class ) )
										.isEqualTo( LocalDateTime.ofInstant( expected, ZoneId.systemDefault() ) );
							}
							assertThat( rows.next() ).isFalse();
						}
					} ) );
					supplier.value = SECOND.plusSeconds( 1 );
					scope.tx( s -> s.remove( s.find( Period.class, 1 ) ) );
					scope.tx( s -> assertThat( s.find( Period.class, 1 ) ).isNull() );
				}
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void serverGeneratedHistoryWithoutMetadata(boolean metadata) {
		for ( String strategy : List.of( "SINGLE_TABLE", "HISTORY_TABLE" ) ) {
			try ( var scope = ContextScope.open( metadata, Map.of(
					"hibernate.type.temporal_preservation", false,
						"hibernate.temporal.table_strategy", strategy,
					"hibernate.temporal.use_server_transaction_timestamps", true ), Period.class ) ) {
				final var table = scope.metadata.getEntityBinding( Period.class.getName() ).getAuxiliaryTable();
				final var before = Instant.now().minusMillis( 1 );
				scope.tx( s -> { final var row = new Period(); row.id = 1; row.payload = "v1"; s.persist( row ); } );
				final var after = Instant.now().plusMillis( 1 );
				final var start = scope.factory.fromTransaction( s -> s.doReturningWork( connection -> {
					try ( var statement = connection.createStatement(); var rows = statement.executeQuery(
							"select valid_from from " + table.getName() ) ) {
						assertThat( rows.next() ).isTrue();
						return rows.getTimestamp( 1 ).toInstant();
					}
				} ) );
				assertThat( start ).isBetween( before, after );
				scope.tx( s -> s.find( Period.class, 1 ).payload = "v2" );
				try ( var session = scope.factory.withOptions().asOf( start ).open() ) {
					session.inTransaction( tx -> assertThat( session.find( Period.class, 1 ).payload ).isEqualTo( "v1" ) );
				}
				assertThat( scope.sql ).anyMatch( sql -> sql.contains( "localtimestamp" ) );
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void copiedPeriodRetainsPrecisionValidationAndDiagnostics(boolean metadata) {
		for ( String strategy : List.of( "SINGLE_TABLE", "HISTORY_TABLE" ) ) {
			try ( var scope = ContextScope.boot( metadata, true,
					Map.of( "hibernate.temporal.table_strategy", strategy ), Period.class ) ) {
				final var table = scope.metadata.getEntityBinding( Period.class.getName() ).getAuxiliaryTable();
				for ( String column : List.of( "valid_from", "valid_to" ) ) {
					final var original = (BasicValue) table.getColumn( new org.hibernate.mapping.Column( column ) ).getValue();
					final var copy = original.copy();
					copy.setExplicitJdbcTypeAccess( types -> org.hibernate.type.descriptor.jdbc.TimeJdbcType.INSTANCE );
					assertThatThrownBy( copy::resolve ).hasMessageContaining( Period.class.getName() )
							.hasMessageContaining( column.equals( "valid_from" ) ? "rowStart" : "rowEnd" )
							.hasMessageContaining( "table=" + table.getName() )
							.hasMessageContaining( "column=" + column ).hasMessageContaining( "strategy=" + strategy )
							.hasMessageContaining( "bound supplier" ).hasMessageContaining( "JavaType=java.time.Instant" )
							.hasMessageContaining( "TimeJdbcType" ).hasMessageContaining( "millisecond precision" )
							.hasMessageNotContaining( "null.null" );
				}
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void ordinaryInstantRemainsValidatedAlongsideSyntheticPeriods(boolean metadata) {
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true, Invalid.class ) ) { }
		} ).hasMessageContaining( "stamp" ).hasMessageContaining( "INSTANT" )
				.hasMessageContaining( "UNKNOWN" );
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void legacyUtcDescriptorDoesNotReplaceApplicationOrCustomHandling(boolean metadata) {
		for ( Class<?> entity : List.of( Period.class, CustomPeriod.class ) ) {
			try ( var scope = ContextScope.boot( metadata, true,
					Map.of( "hibernate.dialect", UtcPeriodDialect.class,
						"hibernate.type.temporal_preservation", false ), entity ) ) {
				final var expected = entity == Period.class
						? org.hibernate.type.descriptor.jdbc.TimestampUtcAsJdbcTimestampJdbcType.class : CustomUtc.class;
				final var model = scope.metadata.getEntityBinding( entity.getName() );
				final var start = (BasicValue) model.getAuxiliaryColumn( "rowStart" ).getValue();
				assertThat( start.resolve().getJdbcType() ).isExactlyInstanceOf( expected );
				assertThat( scope.factory.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( SqlTypes.TIMESTAMP_UTC ) )
						.isExactlyInstanceOf( entity == Period.class
								? org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType.class : CustomUtc.class );
			}
		}
	}

	public static class UtcPeriodDialect extends H2Dialect {
		@Override
		public org.hibernate.dialect.temporal.spi.TemporalTableSupport getTemporalTableSupport() {
			return new org.hibernate.dialect.temporal.spi.DelegatingTemporalTableSupport( super.getTemporalTableSupport() ) {
				@Override public int getTemporalColumnType() { return SqlTypes.TIMESTAMP_UTC; }
			};
		}
	}

	public static class CustomUtc extends org.hibernate.type.descriptor.jdbc.TimestampUtcAsJdbcTimestampJdbcType {
	}

	@Entity(name = "CustomPeriodCompatibility")
	@Temporal
	@org.hibernate.annotations.JdbcTypeRegistration(value = CustomUtc.class, registrationCode = SqlTypes.TIMESTAMP_UTC)
	static class CustomPeriod {
		@Id int id;
	}

	public static class Fixed implements ChangesetIdentifierSupplier<Instant> {
		Instant value = FIRST;
		@Override
		public Instant generateIdentifier(SharedSessionContract session) { return value; }
	}

	@Entity(name = "PeriodCompatibility")
	@Table(name = "period_compatibility")
	@Temporal(rowStart = "valid_from", rowEnd = "valid_to", secondPrecision = 6)
	static class Period {
		@Id int id;
		String payload;
		@Temporal
		@ElementCollection
		@OrderColumn
		List<String> tags = new ArrayList<>();
	}

	@Entity(name = "InvalidPeriodCompatibility")
	@Temporal
	static class Invalid {
		@Id int id;
		@JdbcTypeCode(SqlTypes.TIMESTAMP)
		Instant stamp;
	}
}
