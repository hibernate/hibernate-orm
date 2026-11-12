package org.hibernate.orm.test.mapping.javatime.redesign.context;

import org.hibernate.orm.test.mapping.javatime.redesign.TemporalSchemaCleanup;

import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.temporal.internal.OracleTemporalTableSupport;
import org.hibernate.dialect.temporal.spi.TemporalTableSupport;
import org.hibernate.dialect.temporal.spi.TemporalPeriodPrecisionContext;
import org.hibernate.dialect.temporal.spi.TemporalPeriodPrecisionSupport;
import org.hibernate.type.descriptor.DateTimeUtils;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Explicit owned history-table cutovers retain the compatibility reader's interpretation.
/// Plain-field conversion is limited to modern dates with an established source zone.
///
/// @author Steve Ebersole
final class TemporalPeriodMigrationFixture {
	final String profile;
	final boolean server;
	final boolean metadata;
	TemporalPeriodMigrationFixture(String profile, boolean server, boolean metadata) {
		this.profile = profile; this.server = server; this.metadata = metadata;
	}
	Map<String, Object> settings(boolean target) {
		var settings = TemporalPeriodFixture.settings( target, "HISTORY_TABLE", server, new TemporalPeriodFixture.Fixed() );
		settings.putAll( retainedPrecision() );
		return settings;
	}
	void provision() {
		try ( var scope = ContextScope.boot( true, false, settings( false ), TemporalPeriodFixture.Period.class ) ) {
			scope.factory.getSchemaManager().create( false );
		}
	}
	// This fixture isolates timezone encoding migration on the established p6 schema.
	// Oracle's stock p9 adoption is a separate precision migration, not a reason to
	// silently change the historical fixture or its verified source interpretation.
	Map<String, Object> retainedPrecision() {
		return ContextEnvironment.is( OracleDialect.class )
				? Map.of( "hibernate.dialect", RetainedMicrosOracleDialect.class ) : Map.of();
	}

	TemporalPeriodFixture fixture(boolean target, boolean negative) throws Exception {
		var extra = new HashMap<String, Object>( retainedPrecision() );
		if ( negative ) {
			extra.put( "hibernate.physical_naming_strategy", new PhysicalNamingStrategyStandardImpl() {
				@Override public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment environment) {
					return Identifier.toIdentifier( "neg_" + name.getText() );
				}
			} );
		}
		return new TemporalPeriodFixture( target ? metadata : true, "HISTORY_TABLE", server,
				TemporalPeriodFixture.Period.class, target, false, extra );
	}

	public static class RetainedMicrosOracleDialect extends OracleDialect {
		@Override public TemporalTableSupport getTemporalTableSupport() {
			return new OracleTemporalTableSupport( this ) {
				@Override public int getTemporalColumnPrecision() { return 6; }
				@Override
				public TemporalPeriodPrecisionSupport getTemporalPeriodPrecisionSupport(TemporalPeriodPrecisionContext context) {
					if ( context.precision() == 6 && context.ddlType().equals( "timestamp(6)" )
							&& context.jdbcType().getClass() == TimestampUtcAsPlainJdbcTimestampJdbcType.class
							&& (context.source() == TemporalPeriodPrecisionContext.Source.JAVA
								|| context.source() == TemporalPeriodPrecisionContext.Source.FETCHED) ) {
						return TemporalPeriodPrecisionSupport.supported( "retained-oracle-utc-micros-" + context.source(),
								value -> DateTimeUtils.roundToSecondPrecision( value, 6 ), null );
					}
					return TemporalPeriodPrecisionSupport.unsupported( "Unassessed retained Oracle period route" );
				}
			};
		}
	}

	Properties write() throws Exception {
		provision();
		var evidence = new Properties();
		try ( var f = fixture( false, false ) ) {
			for ( int phase = 0; phase < 2; phase++ ) {
				if ( server && phase > 0 ) f.advancePast( Instant.parse( evidence.getProperty( "a" ) ) );
				f.supplier.value = TemporalPeriodFixture.SEASONAL[phase];
				Instant low = f.clock();
				if ( phase == 0 ) f.insert( 1 ); else f.update( 1 );
				Instant high = f.clock();
				var rows = sourceIntervals( f, false );
				Instant point = rows.get( rows.size() - 1 ).start();
				if ( server ) assertThat( point ).isBetween( low.minusNanos( f.tick ), high.plusNanos( f.tick ) );
				else assertThat( point ).isEqualTo( TemporalPeriodFixture.SEASONAL[phase] );
				evidence.setProperty( phase == 0 ? "a" : "b", point.toString() );
				evidence.setProperty( "bracket." + phase, low + ".." + high );
			}
			Instant a = Instant.parse( evidence.getProperty( "a" ) ), b = Instant.parse( evidence.getProperty( "b" ) );
			assertThat( sourceIntervals( f, false ) ).containsExactly(
					new TemporalPeriodFixture.Interval( a, b, "v1", 0 ), new TemporalPeriodFixture.Interval( b, null, "v2", 0 ) );
			f.at( 1, a.minusNanos( f.tick ), null, List.of() );
			f.at( 1, a, "v1", List.of( "one" ) ); f.at( 1, b, "v2", List.of( "one", "two" ) );
			snapshot( evidence, "entity", sourceIntervals( f, false ) );
			snapshot( evidence, "collection", sourceIntervals( f, true ) );
			for ( String table : List.of( f.entityTable, f.collectionTable ) ) {
				shape( f.reader, table, f.zoned );
				try ( var st = f.reader.createStatement(); var rs = st.executeQuery( "select valid_from from " + table + " order by valid_from" ) ) {
					assertThat( rs.next() ).isTrue();
					if ( !f.zoned ) {
						LocalDateTime raw = rs.getObject( 1, LocalDateTime.class );
						assertThat( raw ).isEqualTo( LocalDateTime.ofInstant( a, ZoneId.systemDefault() ) );
						evidence.setProperty( "raw." + table, raw.toString() );
					}
				}
			}
			if ( profile.equals( "M04" ) ) {
				for ( String table : tables( f ) ) TemporalVersionMigrationFixture.execute( f.reader,
						"create table neg_" + table + " as select * from " + table );
			}
			f.reader.commit();
			evidence.setProperty( "database", f.writer.getMetaData().getDatabaseProductVersion() );
			evidence.setProperty( "driver", f.writer.getMetaData().getDriverVersion() );
			var column = f.scope.metadata.getEntityBinding( TemporalPeriodFixture.Period.class.getName() ).getAuxiliaryColumn( "rowStart" );
			var mapping = (org.hibernate.mapping.BasicValue) column.getValue();
			evidence.setProperty( "source.descriptor", mapping.resolve().getJdbcType().getClass().getName() );
			evidence.setProperty( "source.ddl", column.getSqlType( f.scope.metadata ) );
			evidence.setProperty( "source.producer", "server=" + server + " inline=" + f.scope.factory.getChangesetCoordinator().useServerTimestamp( f.scope.factory.getJdbcServices().getDialect() ) );
			evidence.setProperty( "source.columns", f.entityTable + "," + f.collectionTable + " valid_from/valid_to precision=6, nullability=false/true (catalog checked)" );
			evidence.setProperty( "source.protocol", "current test binary with temporal_preservation=false; repaired coordination, not historical-binary evidence" );
		}
		return evidence;
	}

	void read(Properties evidence) throws Exception {
		// Validate with the source interpretation before any conversion or target mutation.
		try ( var source = fixture( false, false ) ) {
			validateSourceHistory( sourceIntervals( source, false ), sourceIntervals( source, true ) );
		}
		Instant a = Instant.parse( evidence.getProperty( "a" ) ), b = Instant.parse( evidence.getProperty( "b" ) );
		if ( profile.equals( "M04" ) ) {
			try ( var negative = fixture( true, true ) ) {
				assertThat( negative.intervals( false, 1 ).get( 0 ).start() ).isNotEqualTo( a );
				assertThatThrownBy( () -> negative.at( 1, a.minusNanos( negative.tick ), null, List.of() ) ).isInstanceOf( AssertionError.class );
			}
			try ( var source = fixture( false, false ) ) {
				for ( String table : List.of( source.entityTable, source.collectionTable ) ) {
					shape( source.reader, table, false );
					String start = expression( "valid_from", source.oracle() );
					String end = expression( "valid_to", source.oracle() );
					TemporalVersionMigrationFixture.execute( source.reader, "update " + table + " set valid_from=" + start
							+ ",valid_to=case when valid_to is null then null else " + end + " end" );
				} source.reader.commit();
			}
		}
		try ( var f = fixture( true, false ) ) {
			f.scope.factory.getSchemaManager().validateMappedObjects();
			evidence.setProperty( "target.mode", "temporal_preservation=true metadata=" + metadata );
			var column = f.scope.metadata.getEntityBinding( TemporalPeriodFixture.Period.class.getName() ).getAuxiliaryColumn( "rowStart" );
			evidence.setProperty( "target.ddl", column.getSqlType( f.scope.metadata ) );
			evidence.setProperty( "target.descriptor", ( (org.hibernate.mapping.BasicValue) column.getValue() ).resolve().getJdbcType().getClass().getName() );
			for ( String table : List.of( f.entityTable, f.collectionTable ) ) shape( f.reader, table, f.zoned );
			assertSnapshot( evidence, "entity", f.intervals( false, 1 ) );
			assertSnapshot( evidence, "collection", f.intervals( true, 1 ) );
			f.at( 1, a.minusNanos( f.tick ), null, List.of() );
			f.at( 1, a, "v1", List.of( "one" ) ); f.at( 1, b.minusNanos( f.tick ), "v1", List.of( "one" ) );
			f.at( 1, b, "v2", List.of( "one", "two" ) );
			Instant c = mutate( f, b, false );
			Instant d = mutate( f, c, true );
			assertThat( f.intervals( false, 1 ) ).containsExactly(
					new TemporalPeriodFixture.Interval( a, b, "v1", 0 ), new TemporalPeriodFixture.Interval( b, c, "v2", 0 ),
					new TemporalPeriodFixture.Interval( c, d, "v3", 0 ) );
			f.at( 1, c.minusNanos( f.tick ), "v2", List.of( "one", "two" ) );
			f.at( 1, c, "v3", List.of( "one", "two", "three" ) );
			f.at( 1, d.minusNanos( f.tick ), "v3", List.of( "one", "two", "three" ) );
			f.at( 1, d, null, List.of() );
			assertThat( f.intervals( true, 1 ) ).allSatisfy( interval -> {
				assertThat( interval.end() ).isEqualTo( d ); assertThat( interval.start() ).isIn( a, b, c );
			} );
		}
	}

	static void validateSourceHistory(List<TemporalPeriodFixture.Interval> entity,
			List<TemporalPeriodFixture.Interval> collection) {
		assertThat( entity ).as( "source entity history" ).isNotEmpty();
		var boundaries = new HashSet<Instant>();
		for ( int i = 0; i < entity.size(); i++ ) {
			var interval = entity.get( i );
			boundaries.add( interval.start() );
			if ( interval.end() != null ) {
				assertThat( interval.end() ).as( "source entity interval %s", interval ).isAfter( interval.start() );
				boundaries.add( interval.end() );
			}
			if ( i + 1 < entity.size() ) {
				assertThat( interval.end() ).as( "source entity continuity between %s and %s", interval, entity.get( i + 1 ) )
						.isEqualTo( entity.get( i + 1 ).start() );
			}
		}
		for ( var interval : collection ) {
			assertThat( interval.start() ).as( "source collection interval %s", interval ).isIn( boundaries );
			if ( interval.end() != null ) {
				assertThat( interval.end() ).as( "source collection interval %s", interval )
						.isAfter( interval.start() ).isIn( boundaries );
			}
		}
	}

	Instant mutate(TemporalPeriodFixture f, Instant previous, boolean delete) throws Exception {
		if ( server ) f.advancePast( previous );
		f.supplier.value = previous.plusSeconds( 86400 );
		Instant low = f.clock();
		if ( delete ) f.delete( 1 );
		else f.tx( session -> { var row = session.find( TemporalPeriodFixture.Period.class, 1 ); row.payload = "v3"; row.tags.add( "three" ); } );
		Instant high = f.clock();
		var rows = f.intervals( false, 1 );
		Instant result = delete ? rows.get( rows.size() - 1 ).end() : rows.get( rows.size() - 1 ).start();
		if ( server ) assertThat( result ).isBetween( low.minusNanos( f.tick ), high.plusNanos( f.tick ) );
		else assertThat( result ).isEqualTo( f.supplier.value );
		System.out.println( "MIGRATION_PERIOD_BOUNDARY low=" + low + " value=" + result + " high=" + high );
		return result;
	}

	void cleanup() throws Exception {
		try ( var scope = ContextScope.boot( true, false, settings( false ), TemporalPeriodFixture.Period.class ) ) {
			if ( profile.equals( "M04" ) ) {
				try ( var c = scope.provider.getConnection() ) {
					for ( String table : List.of( "pp_tags_history", "pp_tags", "pp_period_history", "pp_period" ) ) {
						TemporalSchemaCleanup.dropStatement( c, "drop table neg_" + table );
					} c.commit();
				}
			}
			scope.drop();
		}
	}
	static List<String> tables(TemporalPeriodFixture f) { return List.of( "pp_period", f.entityTable, "pp_tags", f.collectionTable ); }
	static String expression(String column, boolean oracle) {
		return oracle ? "sys_extract_utc(from_tz(cast(" + column + " as timestamp), 'America/Denver'))"
				: "cast(formatdatetime(cast(cast(" + column + " as varchar) || ' America/Denver' as timestamp with time zone),'yyyy-MM-dd HH:mm:ss.SSSSSS','en','UTC') as timestamp(6))";
	}
	static void shape(Connection c, String table, boolean zoned) throws Exception {
		try ( var st = c.createStatement(); var rs = st.executeQuery( "select valid_from,valid_to from " + table + " where 1=0" ) ) {
			var columns = rs.getMetaData();
			for ( int i = 1; i <= 2; i++ ) {
				assertThat( columns.getScale( i ) ).isEqualTo( 6 );
				assertThat( columns.isNullable( i ) ).isEqualTo( i == 1 ? java.sql.ResultSetMetaData.columnNoNulls : java.sql.ResultSetMetaData.columnNullable );
				String type = columns.getColumnTypeName( i ).toLowerCase();
				assertThat( type.contains( "with time zone" ) || type.equals( "timestamptz" ) ).isEqualTo( zoned );
				System.out.println( "MIGRATION_PERIOD_COLUMN " + table + " " + columns.getColumnName( i ) + " " + type + " precision=6 nullable=" + columns.isNullable( i ) );
			}
		}
	}
	static List<TemporalPeriodFixture.Interval> sourceIntervals(TemporalPeriodFixture f, boolean collection) throws Exception {
		var result = new ArrayList<TemporalPeriodFixture.Interval>();
		try ( var st = f.reader.createStatement(); var rs = st.executeQuery( "select valid_from,valid_to,"
				+ ( collection ? "tag_value,item_order" : "payload,0" ) + " from " + ( collection ? f.collectionTable : f.entityTable )
				+ " where " + ( collection ? "owner_id" : "id" ) + "=1 order by valid_from" ) ) {
			while ( rs.next() ) {
				// The established source protocol uses JVM-local plain timestamps (no JDBC zone).
				var start = rs.getTimestamp( 1 ); var end = rs.getTimestamp( 2 );
				result.add( new TemporalPeriodFixture.Interval( start.toInstant(), end == null ? null : end.toInstant(), rs.getString( 3 ), rs.getInt( 4 ) ) );
			}
		} f.reader.commit(); return result;
	}
	static void snapshot(Properties evidence, String prefix, List<TemporalPeriodFixture.Interval> rows) {
		evidence.setProperty( prefix + ".size", Integer.toString( rows.size() ) );
		for ( int i = 0; i < rows.size(); i++ ) evidence.setProperty( prefix + "." + i, rows.get( i ).toString() );
	}
	static void assertSnapshot(Properties evidence, String prefix, List<TemporalPeriodFixture.Interval> rows) {
		assertThat( rows ).hasSize( Integer.parseInt( evidence.getProperty( prefix + ".size" ) ) );
		for ( int i = 0; i < rows.size(); i++ ) assertThat( rows.get( i ).toString() ).isEqualTo( evidence.getProperty( prefix + "." + i ) );
	}
}
