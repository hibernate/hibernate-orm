package org.hibernate.orm.test.temporal;

import java.sql.Time;
import java.time.LocalTime;
import java.util.Map;
import java.util.TreeMap;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Table;
import org.hibernate.annotations.FractionalSeconds;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/// SQL Server CI gate: catalog scale and independent SQL equality verify physical
/// precision separately from ORM reads. Local H2 execution does not certify this path.
///
/// @author Steve Ebersole
@RequiresDialect(SQLServerDialect.class)
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"))
@DomainModel(annotatedClasses = SQLServerTimePrecisionTests.TimeRow.class)
@SessionFactory
class SQLServerTimePrecisionTests {
	boolean preservationEnabled() { return true; }

	@Test
	void physicalScaleAndRetainedLayout(SessionFactoryScope scope) {
		assertMode( scope );
		scope.inTransaction( session -> session.doWork( connection -> {
			final Map<String, Integer> scales = new TreeMap<>();
			try ( var statement = connection.createStatement(); var rows = statement.executeQuery(
					"select name,scale from sys.columns where object_id=object_id('TimePrecisionRow') and name in ('omitted','p0','p3','p7','retained')" ) ) {
				while ( rows.next() ) scales.put( rows.getString( 1 ), rows.getInt( 2 ) );
			}
			assertThat( scales ).isEqualTo( Map.of( "omitted", 7, "p0", 0, "p3", 3, "p7", 7, "retained", 7 ) );
		} ) );
	}

	@ParameterizedTest
	@ValueSource(strings = { "12:34:56.123456789", "12:34:56.1230000", "12:34:56.1234999",
			"12:34:56.1235000", "12:34:56.1235001", "12:34:56.4999999", "12:34:56.5000000", "12:34:56.5000001",
			"12:34:56.123456749", "12:34:56.123456750", "12:34:56.123456751", "12:34:56.9995000", "23:59:59.9995000", "00:00:00" })
	void declaredPrecisionControlsStorage(String text, SessionFactoryScope scope) {
		assertMode( scope );
		final var input = LocalTime.parse( text );
		final var row = new TimeRow();
		row.id = 1;
		row.omitted = row.p0 = row.p3 = row.p7 = row.retained = input;
		row.legacy = Time.valueOf( "12:34:56" );
		try {
			scope.inTransaction( session -> session.persist( row ) );
			scope.inTransaction( session -> {
				final var loaded = session.find( TimeRow.class, 1 );
				assertThat( loaded.omitted ).isEqualTo( round( input, 7 ) );
				assertThat( loaded.retained ).isEqualTo( round( input, 7 ) );
				assertThat( loaded.p7 ).isEqualTo( round( input, 7 ) );
				assertThat( loaded.p3 ).isEqualTo( round( input, 3 ) );
				assertThat( loaded.p0 ).isEqualTo( round( input, 0 ) );
				assertThat( loaded.legacy.toLocalTime() ).isEqualTo( LocalTime.of( 12, 34, 56 ) );
			} );
			// Independent server comparisons, not expectations learned from ORM extraction.
			scope.inTransaction( session -> session.doWork( connection -> {
				try ( var statement = connection.createStatement(); var rows = statement.executeQuery(
						"select case when p0=cast('" + round( input, 0 ) + "' as time(0))"
								+ " and p3=cast('" + round( input, 3 ) + "' as time(3))"
								+ " and p7=cast('" + round( input, 7 ) + "' as time(7))"
								+ " and omitted=p7 and retained=p7 and cast(omitted as time)=p7 then 1 else 0 end from TimePrecisionRow where id=1" ) ) {
					assertThat( rows.next() ).isTrue();
					assertThat( rows.getInt( 1 ) ).isEqualTo( 1 );
				}
				// Seven-digit text isolates server conversion from Java/JDBC nine-digit conversion.
				if ( input.getNano() % 100 == 0 ) {
					for ( int precision : new int[] { 0, 3, 7 } ) {
						try ( var statement = connection.createStatement(); var rows = statement.executeQuery(
								"select case when cast(cast('" + input + "' as time(7)) as time(" + precision
										+ "))=cast('" + round( input, precision ) + "' as time(" + precision + ")) then 1 else 0 end" ) ) {
							assertThat( rows.next() ).isTrue();
							assertThat( rows.getInt( 1 ) ).as( "server-only precision " + precision ).isEqualTo( 1 );
						}
					}
				}
			} ) );
			// An unsized HQL cast must not narrow the retained seven-digit value.
			if ( preservationEnabled() ) {
				scope.inTransaction( session -> assertThat( session.createSelectionQuery(
						"select cast(e.omitted as LocalTime) from TimePrecisionRow e", LocalTime.class ).getSingleResult() )
						.isEqualTo( round( input, 7 ) ) );
			}
		}
		finally { scope.dropData(); }
	}

	@Test
	void nulls(SessionFactoryScope scope) {
		final var row = new TimeRow(); row.id = 1;
		try {
			scope.inTransaction( session -> session.persist( row ) );
			scope.inTransaction( session -> {
				final var loaded = session.find( TimeRow.class, 1 );
				assertThat( loaded.omitted ).isNull();
				assertThat( loaded.p0 ).isNull();
				assertThat( loaded.p3 ).isNull();
				assertThat( loaded.p7 ).isNull();
				assertThat( loaded.retained ).isNull();
				assertThat( loaded.legacy ).isNull();
			} );
		}
		finally { scope.dropData(); }
	}

	@Test
	void callablePrecisionAndNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.doWork( connection -> {
			try ( var statement = connection.createStatement() ) {
				statement.execute( "create procedure time_precision_echo @input time(3), @output time(3) output as begin set nocount on; set @output=@input; end" );
			}
		} ) );
		try {
			// Legacy default LocalTime uses its established carrier; do not demand new precision from it.
			final var input = LocalTime.parse( preservationEnabled() ? "12:34:56.1234567" : "12:34:56" );
			for ( LocalTime value : new LocalTime[] { input, null } ) {
				scope.inTransaction( session -> {
					try ( var call = session.createStoredProcedureCall( "time_precision_echo" ) ) {
						final var in = call.registerParameter( 1, LocalTime.class, ParameterMode.IN );
						final var out = call.registerParameter( 2, LocalTime.class, ParameterMode.OUT );
						call.setParameter( in, value );
						assertThat( call.getOutputs().getOutputParameterValue( out ) ).isEqualTo( value == null ? null : round( value, 3 ) );
					}
				} );
			}
		}
		finally {
			scope.inTransaction( session -> session.doWork( connection -> {
				try ( var statement = connection.createStatement() ) { statement.execute( "drop procedure time_precision_echo" ); }
			} ) );
		}
	}

	private void assertMode(SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getSessionFactoryOptions().isTemporalPreservationEnabled() ).isEqualTo( preservationEnabled() );
	}

	private static LocalTime round(LocalTime value, int precision) {
		final long unit = (long) Math.pow( 10, 9 - precision );
		return value.plusNanos( unit / 2 ).withNano( (int) ((value.getNano() + unit / 2) % 1_000_000_000 / unit * unit) );
	}

	@Entity(name = "TimePrecisionRow")
	@Table(name = "TimePrecisionRow")
	static class TimeRow {
		@Id int id;
		@JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime omitted;
		@Column(secondPrecision = 0) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime p0;
		@FractionalSeconds(3) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime p3;
		@Column(secondPrecision = 7) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime p7;
		@Column(columnDefinition = "time") @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime retained;
		Time legacy;
	}
}
