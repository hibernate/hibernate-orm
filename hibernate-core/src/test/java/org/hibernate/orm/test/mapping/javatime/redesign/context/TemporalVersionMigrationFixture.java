package org.hibernate.orm.test.mapping.javatime.redesign.context;

import org.hibernate.dialect.OracleDialect;

import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.orm.test.mapping.javatime.redesign.TemporalSchemaCleanup;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.SourceType;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.BasicValue;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsOffsetDateTimeJdbcType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Compatibility-mode writers and final preserving readers use independently closed factories.
/// Explicit SQL applies only to these owned modern-date fixtures; it is not a migration generator.
///
/// @author Steve Ebersole
final class TemporalVersionMigrationFixture {
	static final String TABLE = "mh_version";
	static final String NEGATIVE = "mh_version_negative";
	static final ZoneId DENVER = ZoneId.of( "America/Denver" );
	final String profile;
	final String kind;
	final boolean database;
	final boolean metadata;

	TemporalVersionMigrationFixture(String profile, String kind, boolean database, boolean metadata) {
		this.profile = profile; this.kind = kind; this.database = database; this.metadata = metadata;
	}

	Class<? extends Row> type(boolean target) {
		return switch ( kind ) {
			case "Local" -> database ? LocalDb.class : LocalVm.class;
			case "Instant" -> target ? database ? RetainedInstantDb.class : RetainedInstantVm.class
					: database ? InstantDb.class : InstantVm.class;
			case "Offset" -> database ? OffsetDb.class : OffsetVm.class;
			case "Zoned" -> database ? ZonedDb.class : ZonedVm.class;
			default -> throw new IllegalArgumentException( kind );
		};
	}

	Map<String, Object> settings(boolean target, boolean negative) {
		var result = new HashMap<String, Object>();
		result.put( "hibernate.type.temporal_preservation", target );
		result.put( "hibernate.cache.use_second_level_cache", cacheControl() );
		if ( !target && ( profile.equals( "M06" ) || profile.equals( "M07" ) ) ) {
			result.put( "hibernate.timezone.default_storage", "NORMALIZE" );
		}
		if ( negative ) result.put( "hibernate.physical_naming_strategy", new PhysicalNamingStrategyStandardImpl() {
			@Override public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment environment) {
				return name.getText().equals( TABLE ) ? Identifier.toIdentifier( NEGATIVE ) : name;
			}
		} );
		return result;
	}

	boolean cacheControl() {
		return profile.equals( "M03" ) && ContextEnvironment.is( H2Dialect.class );
	}

	ContextScope open(boolean target, boolean negative) {
		var scope = ContextScope.boot( target ? metadata : true, false, settings( target, negative ), type( target ) );
		scope.provider.blocked = false;
		return scope;
	}

	Properties write() throws Exception {
		assertThat( ZoneId.systemDefault() ).isEqualTo( DENVER );
		var evidence = new Properties();
		try ( var source = open( false, false ) ) {
			source.factory.getSchemaManager().create( false );
			source.tx( session -> {
				for ( int id = 1; id <= 3; id++ ) session.persist( row( false, id, null ) );
			} );
			source.tx( session -> {
				session.setCacheMode( org.hibernate.CacheMode.IGNORE );
				for ( int id = 1; id <= 3; id++ ) {
					var loaded = session.find( type( false ), id );
					evidence.setProperty( "token." + id, token( loaded ).toString() );
				}
			} );
			try ( var connection = source.provider.getConnection() ) {
				catalog( connection, TABLE, kind.equals( "Instant" ) );
				for ( int id = 1; id <= 3; id++ ) {
					var raw = raw( connection, TABLE, id, kind.equals( "Instant" ) );
					evidence.setProperty( "raw." + id, raw.toString() );
					var expected = parse( evidence.getProperty( "token." + id ) );
					if ( kind.equals( "Local" ) ) {
						var local = (LocalDateTime) expected;
						assertThat( raw ).isEqualTo( profile.equals( "M03" )
								? local.atZone( DENVER ).withZoneSameInstant( ZoneOffset.UTC ).toLocalDateTime() : local );
					}
					else if ( kind.equals( "Instant" ) ) assertThat( ( (OffsetDateTime) raw ).toInstant() ).isEqualTo( expected );
					else assertThat( raw ).isEqualTo( expected instanceof ZonedDateTime z ? z.toLocalDateTime() : ( (OffsetDateTime) expected ).toLocalDateTime() );
				}
				if ( profile.equals( "M03" ) || profile.equals( "M06" ) ) {
					execute( connection, "create table " + NEGATIVE + " as select * from " + TABLE );
				}
				connection.commit();
				evidence.setProperty( "database", connection.getMetaData().getDatabaseProductVersion() );
				evidence.setProperty( "driver", connection.getMetaData().getDriverVersion() );
			}
			if ( cacheControl() ) {
				source.tx( session -> session.find( type( false ), 1 ) );
				source.factory.getStatistics().clear();
				source.tx( session -> session.find( type( false ), 1 ) );
				assertThat( source.factory.getStatistics().getSecondLevelCacheHitCount() ).isGreaterThan( 0 );
				source.factory.getCache().evictAllRegions();
				assertThat( source.factory.getCache().containsEntity( type( false ), 1 ) ).isFalse();
				evidence.setProperty( "cache.cutover", "source populated, observed hit, explicitly invalidated before source close" );
			}
			var value = (BasicValue) source.metadata.getEntityBinding( type( false ).getName() ).getVersion().getValue();
			evidence.setProperty( "source.descriptor", value.resolve().getJdbcType().getClass().getName() );
			evidence.setProperty( "source.java_type", value.resolve().getDomainJavaType().getJavaTypeClass().getName() );
			evidence.setProperty( "source.jdbc_code", Integer.toString( value.resolve().getJdbcType().getJdbcTypeCode() ) );
			evidence.setProperty( "source.ddl", value.getColumns().get( 0 ).getSqlType( source.metadata ) );
			evidence.setProperty( "source.column", "mh_version.vstamp precision=6 nullable=false (catalog checked)" );
			evidence.setProperty( "source.protocol", "current-runtime temporal_preservation=false; baseline=321b4ff81f217149f612878129a31cca599e6ab8" );
			evidence.setProperty( "profile", profile + ":" + kind + ":" + ( database ? "DB" : "VM" ) );
		}
		return evidence;
	}

	void read(Properties evidence) throws Exception {
		if ( profile.equals( "M07" ) ) {
			evidence.setProperty( "target.outcome", "unsupported pair rejected before mutation" );
			assertThatThrownBy( () -> { try ( var ignored = open( true, false ) ) {} } )
					.isInstanceOf( org.hibernate.MappingException.class );
			try ( var source = open( false, false ); var connection = source.provider.getConnection() ) {
				assertThat( raw( connection, TABLE, 1, false ).toString() ).isEqualTo( evidence.getProperty( "raw.1" ) );
			}
			return;
		}
		if ( profile.equals( "M03" ) ) {
			try ( var negative = open( true, true ) ) {
				negative.tx( session -> assertThat( token( session.find( type( true ), 1 ) ) ).isNotEqualTo( parse( evidence.getProperty( "token.1" ) ) ) );
			}
		}
		if ( profile.equals( "M06" ) ) {
			try ( var negative = open( true, true ); var connection = negative.provider.getConnection();
					var statement = connection.createStatement(); var columns = statement.executeQuery( "select vstamp from " + NEGATIVE + " where 1=0" ) ) {
				// The old plain column is structurally incompatible with the selected native target,
				// even if a particular driver happens to synthesize an offset for a sample value.
				assertThat( columns.getMetaData().getColumnTypeName( 1 ).toLowerCase() ).doesNotContain( "with time zone" );
				var mapping = (BasicValue) negative.metadata.getEntityBinding( type( true ).getName() ).getVersion().getValue();
				assertThat( mapping.getColumns().get( 0 ).getSqlType( negative.metadata ).toLowerCase() ).contains( "with time zone" );
				negative.noMutation();
			}
		}
		if ( profile.equals( "M03" ) || profile.equals( "M06" ) ) {
			try ( var source = open( false, false ); var connection = source.provider.getConnection() ) {
				assertThat( raw( connection, NEGATIVE, 1, false ).toString() ).isEqualTo( evidence.getProperty( "raw.1" ) );
				convert( connection );
				connection.commit();
			}
		}
		try ( var target = open( true, false ) ) {
			target.factory.getSchemaManager().validateMappedObjects();
			var mapping = (BasicValue) target.metadata.getEntityBinding( type( true ).getName() ).getVersion().getValue();
			evidence.setProperty( "target.descriptor", mapping.resolve().getJdbcType().getClass().getName() );
			evidence.setProperty( "target.ddl", mapping.getColumns().get( 0 ).getSqlType( target.metadata ) );
			evidence.setProperty( "target.mode", "temporal_preservation=true metadata=" + metadata );
			try ( var connection = target.provider.getConnection() ) {
				catalog( connection, TABLE, !kind.equals( "Local" ) );
				for ( int id = 1; id <= 3; id++ ) {
					Object old = parse( evidence.getProperty( "token." + id ) );
					try ( var query = connection.prepareStatement( "select count(*) from " + TABLE + " where id=? and vstamp=?" ) ) {
						query.setInt( 1, id );
						query.setObject( 2, old instanceof Instant instant ? instant.atOffset( ZoneOffset.UTC ) : old instanceof ZonedDateTime zoned ? zoned.toOffsetDateTime() : old );
						try ( var rs = query.executeQuery() ) { rs.next(); assertThat( rs.getInt( 1 ) ).isEqualTo( 1 ); }
					}
				} connection.commit();
			}
			target.measure();
			target.tx( session -> {
				for ( int id = 1; id <= 3; id++ ) {
					var loaded = session.find( type( true ), id );
					assertThat( token( loaded ) ).isEqualTo( parse( evidence.getProperty( "token." + id ) ) );
					assertThat( token( loaded ).toString() ).isEqualTo( evidence.getProperty( "token." + id ) );
				}
			} );
			assertThat( target.sql ).anyMatch( sql -> sql.toLowerCase().contains( "select" ) );
			var generated = new Object[1];
			target.tx( session -> {
				var detached = row( true, 1, parse( evidence.getProperty( "token.1" ) ) ); detached.payload = "updated";
				var merged = session.merge( detached ); session.flush(); generated[0] = token( merged );
			} );
			assertThat( generated[0] ).isNotEqualTo( parse( evidence.getProperty( "token.1" ) ) );
			target.tx( session -> assertThat( token( session.find( type( true ), 1 ) ) ).isEqualTo( generated[0] ) );
			assertThatThrownBy( () -> target.tx( session -> session.merge( row( true, 1, parse( evidence.getProperty( "token.1" ) ) ) ) ) )
					.isInstanceOf( jakarta.persistence.OptimisticLockException.class );
			assertThatThrownBy( () -> target.tx( session -> session.remove( row( true, 1, parse( evidence.getProperty( "token.1" ) ) ) ) ) )
					.isInstanceOf( jakarta.persistence.OptimisticLockException.class );
			target.tx( session -> session.remove( session.find( type( true ), 1 ) ) );
			target.tx( session -> session.remove( session.find( type( true ), 2 ) ) );
		}
	}

	void cleanup() throws Exception {
		try ( var source = open( false, false ); var connection = source.provider.getConnection() ) {
			if ( profile.equals( "M03" ) || profile.equals( "M06" ) ) TemporalSchemaCleanup.dropStatement( connection, "drop table " + NEGATIVE );
			source.drop(); connection.commit();
		}
	}

	void convert(Connection connection) throws Exception {
		String provider = connection.getMetaData().getDatabaseProductName();
		if ( profile.equals( "M06" ) ) {
			assertThat( provider ).contains( "Oracle" );
			for ( String sql : new String[] {
					"alter table mh_version add (vstamp_new timestamp(6) with time zone)",
					"update mh_version set vstamp_new=from_tz(cast(vstamp as timestamp), 'America/Denver')",
					"alter table mh_version modify (vstamp_new not null)",
					"alter table mh_version drop column vstamp",
					"alter table mh_version rename column vstamp_new to vstamp" } ) execute( connection, sql );
		}
		else {
			String expression = ContextEnvironment.is( PostgreSQLDialect.class ) ? "vstamp at time zone 'UTC' at time zone 'America/Denver'"
					: ContextEnvironment.is( OracleDialect.class ) ? "cast(from_tz(cast(vstamp as timestamp), 'UTC') at time zone 'America/Denver' as timestamp(6))"
					: "cast(formatdatetime(cast(cast(vstamp as varchar) || ' UTC' as timestamp with time zone),'yyyy-MM-dd HH:mm:ss.SSSSSS','en','America/Denver') as timestamp(6))";
			execute( connection, "update " + TABLE + " set vstamp=" + expression );
		}
	}

	static void execute(Connection connection, String sql) throws Exception {
		try ( var st = connection.createStatement() ) { st.execute( sql ); }
	}
	static Object raw(Connection connection, String table, int id, boolean zoned) throws Exception {
		try ( var st = connection.prepareStatement( "select vstamp from " + table + " where id=?" ) ) {
			st.setInt( 1, id ); try ( var rs = st.executeQuery() ) {
				assertThat( rs.next() ).isTrue(); return zoned ? rs.getObject( 1, OffsetDateTime.class ) : rs.getObject( 1, LocalDateTime.class );
			}
		}
	}
	static void catalog(Connection connection, String table, boolean zoned) throws Exception {
		try ( var st = connection.createStatement(); var rs = st.executeQuery( "select vstamp from " + table + " where 1=0" ) ) {
			var columns = rs.getMetaData();
			assertThat( columns.getScale( 1 ) ).isEqualTo( 6 );
			assertThat( columns.isNullable( 1 ) ).isEqualTo( java.sql.ResultSetMetaData.columnNoNulls );
			assertThat( ( columns.getColumnTypeName( 1 ).toLowerCase().contains( "with time zone" ) || columns.getColumnTypeName( 1 ).equalsIgnoreCase( "timestamptz" ) ) ).isEqualTo( zoned );
			System.out.println( "MIGRATION_COLUMN type=" + columns.getColumnTypeName( 1 ) + " precision=" + columns.getScale( 1 ) + " nullable=" + columns.isNullable( 1 ) );
		}
	}
	Object parse(String value) {
		return switch ( kind ) {
			case "Local" -> LocalDateTime.parse( value ); case "Instant" -> Instant.parse( value );
			case "Offset" -> OffsetDateTime.parse( value ); case "Zoned" -> ZonedDateTime.parse( value );
			default -> throw new IllegalArgumentException( kind );
		};
	}
	Row row(boolean target, int id, Object token) {
		try { var row = type( target ).getDeclaredConstructor().newInstance(); row.id = id; field( row ).set( row, token ); return row; }
		catch ( ReflectiveOperationException e ) { throw new AssertionError( e ); }
	}
	static Field field(Row row) throws NoSuchFieldException { return row.getClass().getDeclaredField( "stamp" ); }
	static Object token(Row row) {
		try { return field( row ).get( row ); } catch ( ReflectiveOperationException e ) { throw new AssertionError( e ); }
	}
	@MappedSuperclass static abstract class Row { @Id int id; String payload = "source"; }
	@Entity @jakarta.persistence.Cacheable @org.hibernate.annotations.Cache(usage=org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE) @Table(name = TABLE) static class LocalVm extends Row { @Version @Column(name="vstamp", secondPrecision=6, nullable=false) LocalDateTime stamp; }
	@Entity @jakarta.persistence.Cacheable @org.hibernate.annotations.Cache(usage=org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE) @Table(name = TABLE) static class LocalDb extends Row { @Version @CurrentTimestamp(source=SourceType.DB) @Column(name="vstamp", secondPrecision=6, nullable=false) LocalDateTime stamp; }
	@Entity @Table(name = TABLE) static class InstantVm extends Row { @Version @Column(name="vstamp", secondPrecision=6, nullable=false) Instant stamp; }
	@Entity @Table(name = TABLE) static class InstantDb extends Row { @Version @CurrentTimestamp(source=SourceType.DB) @Column(name="vstamp", secondPrecision=6, nullable=false) Instant stamp; }
	@Entity @Table(name = TABLE) static class RetainedInstantVm extends Row { @Version @JdbcType(TimestampUtcAsOffsetDateTimeJdbcType.class) @Column(name="vstamp", secondPrecision=6, nullable=false) Instant stamp; }
	@Entity @Table(name = TABLE) static class RetainedInstantDb extends Row { @Version @CurrentTimestamp(source=SourceType.DB) @JdbcType(TimestampUtcAsOffsetDateTimeJdbcType.class) @Column(name="vstamp", secondPrecision=6, nullable=false) Instant stamp; }
	@Entity @Table(name = TABLE) static class OffsetVm extends Row { @Version @Column(name="vstamp", secondPrecision=6, nullable=false) OffsetDateTime stamp; }
	@Entity @Table(name = TABLE) static class OffsetDb extends Row { @Version @CurrentTimestamp(source=SourceType.DB) @Column(name="vstamp", secondPrecision=6, nullable=false) OffsetDateTime stamp; }
	@Entity @Table(name = TABLE) static class ZonedVm extends Row { @Version @Column(name="vstamp", secondPrecision=6, nullable=false) ZonedDateTime stamp; }
	@Entity @Table(name = TABLE) static class ZonedDb extends Row { @Version @CurrentTimestamp(source=SourceType.DB) @Column(name="vstamp", secondPrecision=6, nullable=false) ZonedDateTime stamp; }
}
