package org.hibernate.orm.test.type.temporal.preservation;


import org.hibernate.orm.test.mapping.javatime.redesign.TemporalSchemaCleanup;
import org.hibernate.SessionFactory;
import org.hibernate.TemporalValueException;
import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation;
import org.hibernate.dialect.type.internal.OracleLocalTimeJdbcType;
import org.hibernate.dialect.type.internal.OracleOffsetDateTimeJdbcType;
import org.hibernate.dialect.type.internal.OracleOffsetTimeJdbcType;
import org.hibernate.dialect.type.internal.OracleZonedDateTimeJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLOffsetTimeJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLTimestampUtcAsJdbcTimestampJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLUtcTimeJdbcType;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.InstantJdbcType;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.OffsetDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.OffsetTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsJdbcTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsOffsetTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeWithTimeZoneJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsOffsetDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsZonedJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimestampJdbcType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Shared typed lifecycle, resolution assertions and zero-connection bootstrap.
/// Expected support is an explicit platform matrix, independent of reported facts.
///
/// @author Steve Ebersole
public abstract class AbstractPreservationTests<T> {
	final Class<T> domain;
	private boolean hideOracleDriver;

	AbstractPreservationTests(Class<T> domain) { this.domain = domain; }

	abstract List<Mapping<T>> mappings();

	@Test
	public void preservationMatrix() throws Exception {
		final var environment = PreservationEnvironment.current();
		environment.assertJvm();
		final String mode = System.getProperty( "hibernate.test.temporal.matrix_metadata", "both" );
		for ( boolean metadata : new boolean[] {true, false} ) {
			if ( !mode.equals( "both" ) && metadata != Boolean.parseBoolean( mode ) ) continue;
			for ( var handling : Handling.values() ) run( environment, metadata, handling );
		}
	}

	/// Whether the domain type under test is `OffsetTime`
	private boolean time() { return domain == OffsetTime.class; }

	/// Whether the domain type under test is `ZonedDateTime`
	private boolean zoned() { return domain == ZonedDateTime.class; }

	/// Whether the domain type under test is zone-aware - i.e. `OffsetTime`, `OffsetDateTime`, or `ZonedDateTime`
	private boolean aware() { return time() || zoned() || domain == OffsetDateTime.class; }

	private boolean applicable(Mapping<T> mapping, Handling handling) {
		if ( handling == Handling.NATURAL ) {
			return true;
		}
		if ( mapping.unknown() || mapping.explicitFallback() || !aware() && domain != Instant.class ) {
			return false;
		}
		if ( aware() && !mapping.column() && !mapping.adjusted() ) {
			return handling == Handling.FALLBACK_PLAIN || handling == Handling.FALLBACK_ZONED;
		}
		return true;
	}

	private boolean supported(Mapping<T> mapping, Handling handling, PreservationEnvironment env) {
		if ( domain == LocalTime.class && mapping.id().equals( "RawDirect" ) && (env.pg() || env.oracle()) ) return false;
		if ( mapping.unknown() ) {
			return false;
		}
		if ( mapping.adjusted() && !aware() || time() && "America/New_York".equals( mapping.adjustment() ) ) {
			return false;
		}
		if ( mapping.explicitFallback() ) {
			return domain == Instant.class;
		}
		if ( !aware() ) {
			return domain != Instant.class || handling != Handling.DIRECT_PLAIN;
		}
		if ( !mapping.column() && !mapping.adjusted() ) {
			return handling == Handling.NATURAL && (time() || zoned() && env.oracle() || !zoned() && !env.pg());
		}
		if ( time() ) {
			return switch ( handling ) {
				case NATURAL -> true;
				case DIRECT_PLAIN -> env.pg();
				case DIRECT_ZONED -> !env.pg();
				default -> false;
			};
		}
		return handling != Handling.DIRECT_PLAIN;
	}

	private void run(PreservationEnvironment env, boolean metadataEnabled, Handling handling) throws Exception {
		final var candidates = mappings().stream().filter( m -> applicable( m, handling ) ).toList();
		if ( env.oracle() && env.full() && !metadataEnabled && handling == Handling.NATURAL
				&& (domain == LocalDate.class || domain == ZonedDateTime.class) ) {
			hideOracleDriver = true;
			final var isolatedProvider = new GuardedConnectionProvider( true );
			try {
				final var isolated = List.of( mappings().get( 0 ) );
				if ( domain == LocalDate.class ) {
					try ( var ignored = boot( isolated, handling, false, isolatedProvider, "none", env ) ) {}
				}
				else assertThatThrownBy( () -> {
					try ( var ignored = boot( isolated, handling, false, isolatedProvider, "none", env ) ) {}
				} ).isInstanceOf( MappingException.class ).hasMessageContaining( "preservation" );
				assertThat( isolatedProvider.blockedAttempts ).isZero();
				System.out.println( "TP_OPTIONAL_DRIVER\t" + domain.getSimpleName() + "\tbootAttempts=0" );
			}
			finally { hideOracleDriver = false; }
		}

		if ( env.full() ) {
			for ( var mapping : candidates ) if ( !supported( mapping, handling, env ) ) {
				final var provider = new GuardedConnectionProvider( !metadataEnabled );
				try {
					assertThatThrownBy( () -> {
						try ( var ignored = boot( List.of( mapping ), handling, metadataEnabled, provider, "none", env ) ) {}
					} ).as( "%s / %s / %s / metadata=%s", domain, mapping, handling, metadataEnabled )
							.isInstanceOf( MappingException.class )
							.satisfies( ex -> assertThat( ex.getMessage() ).containsAnyOf( "value", "utcTime", "instant" ) );
				}
				finally { if ( !metadataEnabled ) assertThat( provider.blockedAttempts ).isZero(); }
				inventory( env, mapping, handling, metadataEnabled, "BOOT_REJECTION", "-", "-" );
			}
		}
		final var accepted = candidates.stream().filter( m -> supported( m, handling, env ) ).toList();
		if ( accepted.isEmpty() ) return;
		if ( env.writer() ) {
			// Independent setup completes before the guarded registry exists.
			final var setupProvider = new GuardedConnectionProvider( false );
			try ( var setup = boot( accepted, handling, metadataEnabled, setupProvider, "none", env );
					var connection = setupProvider.getConnection() ) {
				TemporalSchemaCleanup.drop( setup.metadata, setup.registry, connection );
				setup.factory.getSchemaManager().create( false );
			}
		}
		final var provider = new GuardedConnectionProvider( !metadataEnabled );
		try ( var runtime = boot( accepted, handling, metadataEnabled, provider, "none", env ) ) {
			if ( !metadataEnabled ) assertThat( provider.blockedAttempts ).isZero();
			System.out.println( "TP_BOOT\t" + domain.getSimpleName() + "\t" + handling + "\tmetadata=" + metadataEnabled
					+ "\tguardedAttempts=" + provider.blockedAttempts + "\tguarded=" + provider.blocked );
			provider.blocked = false;
			if ( !env.full() ) {
				final String expected = "America/Los_Angeles";
				if ( !env.profile().equals( "E1" ) && !env.profile().equals( "E2" ) ) {
					assertThat( runtime.factory.unwrap( org.hibernate.engine.spi.SessionFactoryImplementor.class )
							.getSessionFactoryOptions().getJdbcTimeZone().getID() ).isEqualTo( expected );
				}
			}
			if ( env.phase().equals( "read" ) ) {
				final var jdbcEnvironment = runtime.registry.requireService( org.hibernate.engine.jdbc.env.spi.JdbcEnvironment.class );
				System.out.println( "TP_NAMESPACE catalog=" + jdbcEnvironment.getCurrentCatalog() + " schema=" + jdbcEnvironment.getCurrentSchema()
						+ " tableFilter=" + jdbcEnvironment.getIdentifierHelper().toMetaDataObjectName( Identifier.toIdentifier( "tp_ldate_0_0_0" ) ) );
				runtime.factory.getSchemaManager().validate();
			}

			try ( Connection writer = provider.getConnection(); Connection reader = provider.getConnection() ) {
				final String writerId = env.identity( writer ), readerId = env.identity( reader );
				assertThat( writerId ).isNotEqualTo( readerId );
				System.out.println( "TP_ENV\t" + env + "\tmetadata=" + metadataEnabled + "\thandling=" + handling
						+ "\twriterConnection=" + writerId + "\treaderConnection=" + readerId + "\tpid=" + ProcessHandle.current().pid()
						+ "\tdriver=" + writer.getMetaData().getDriverVersion() + "\tdatabase=" + writer.getMetaData().getDatabaseProductVersion()
						+ "\tjava=" + System.getProperty( "java.version" ) + "\ttzdb=" + java.time.zone.ZoneRulesProvider.getVersions( "America/New_York" ).lastKey() );
				for ( var mapping : accepted ) {
					final var storage = resolve( runtime, mapping, handling, metadataEnabled, env );
					try { lifecycle( runtime.factory, mapping, storage, handling, metadataEnabled, env, writer, reader, provider ); }
					catch (Exception | AssertionError failure) {
						throw new AssertionError( "Temporal cell " + domain.getSimpleName() + "/" + mapping + "/" + handling
								+ "/metadata=" + metadataEnabled + "/" + env, failure );
					}

				}
			}
			if ( env.reader() ) runtime.factory.getSchemaManager().drop( false );
			assertThat( provider.runtimeAcquisitions ).isPositive();
		}
	}

	private Boot boot(List<Mapping<T>> mappings, Handling handling, boolean metadataEnabled,
			GuardedConnectionProvider provider, String schema, PreservationEnvironment env) {
		final var loaders = new ClassLoaderServiceImpl() {
			@Override public <X> Class<X> classForName(String name) {
				if ( hideOracleDriver && (name.equals( "oracle.jdbc.OracleConnection" ) || name.equals( "oracle.sql.ZONEIDMAP" )) ) {
					throw new org.hibernate.boot.registry.classloading.spi.ClassLoadingException( "Optional Oracle driver hidden by fixture" );
				}
				return super.classForName( name );
			}

			@Override public <S> Collection<S> loadJavaServices(Class<S> contract) {
				if ( contract != TypeContributor.class ) return super.loadJavaServices( contract );
				final var contributors = new ArrayList<S>( super.loadJavaServices( contract ) );
				contributors.add( contract.cast( (TypeContributor) (contributions, services) -> {
					if ( handling == Handling.NATURAL ) return;
					final var dialect = services.requireService( JdbcServices.class ).getDialect();
					final var types = contributions.getTypeConfiguration();
					final var jdbc = types.getJdbcTypeRegistry();
					final boolean plain = handling == Handling.DIRECT_PLAIN || handling == Handling.FALLBACK_PLAIN;
					final boolean fallback = handling == Handling.FALLBACK_PLAIN || handling == Handling.FALLBACK_ZONED;
					final org.hibernate.type.descriptor.jdbc.JdbcType timestamp = fallback
							? plain ? TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE
									: env.pg() ? PostgreSQLTimestampUtcAsJdbcTimestampJdbcType.INSTANCE
											: env.oracle() ? new org.hibernate.dialect.type.internal.OracleTimestampUtcAsJdbcTimestampJdbcType(
													services.requireService( org.hibernate.boot.registry.classloading.spi.ClassLoaderService.class ) )
													: TimestampUtcAsZonedJdbcTimestampJdbcType.INSTANCE
							: plain ? InstantJdbcType.INSTANCE : TimestampUtcAsOffsetDateTimeJdbcType.INSTANCE;
					jdbc.addDescriptor( SqlTypes.TIMESTAMP_UTC, timestamp );
					jdbc.addDescriptor( SqlTypes.TIME_UTC, fallback ? TimeUtcAsJdbcTimeJdbcType.INSTANCE
							: plain ? env.pg() ? PostgreSQLUtcTimeJdbcType.INSTANCE : LocalTimeJdbcType.INSTANCE
									: env.pg() ? TimeWithTimeZoneJdbcType.INSTANCE : TimeUtcAsOffsetTimeJdbcType.INSTANCE );
					if ( fallback ) {
						jdbc.addDescriptor( SqlTypes.OFFSET_DATE_TIME, timestamp );
						jdbc.addDescriptor( SqlTypes.ZONED_DATE_TIME, timestamp );
						jdbc.addDescriptor( SqlTypes.TIMESTAMP_WITH_TIMEZONE, timestamp );
						jdbc.addDescriptor( SqlTypes.OFFSET_TIME, TimeUtcAsJdbcTimeJdbcType.INSTANCE );
					}
					types.getDdlTypeRegistry().addDescriptor( new DdlTypeImpl( SqlTypes.TIMESTAMP_UTC,
							"timestamp($p)" + (plain ? "" : " with time zone"), dialect ) );
					types.getDdlTypeRegistry().addDescriptor( new DdlTypeImpl( SqlTypes.TIME_UTC,
							(env.oracle() ? "timestamp($p)" : "time($p)") + (plain ? "" : " with time zone"), dialect ) );
				} ) );
				return contributors;
			}
		};
		final var bootstrap = new BootstrapServiceRegistryBuilder().applyClassLoaderService( loaders ).build();
		final var registry = new StandardServiceRegistryBuilder( bootstrap )
				.applySetting( "hibernate.connection.provider_class", provider )
				.applySetting( "hibernate.boot.allow_jdbc_metadata_access", metadataEnabled )
				.applySetting( "hibernate.type.java_time_use_direct_jdbc", true )
				.applySetting( "hibernate.cache.use_second_level_cache", false )
				.applySetting( "hibernate.cache.use_query_cache", false )
				.applySetting( "hibernate.show_sql", false )
				.applySetting( "hibernate.jpa.static_metamodel.population", "disabled" )
				.applySetting( "hibernate.hbm2ddl.auto", schema )
				.applySetting( "hibernate.hbm2ddl.halt_on_error", true )
				.applySetting( "jakarta.persistence.sql-load-script-source", "" )
				.applySetting( "jakarta.persistence.database-product-version", env.version() )
				.build();
		try {
			final var sources = new MetadataSources( registry );
			mappings.forEach( mapping -> sources.addAnnotatedClass( mapping.entity() ) );
			final var metadata = sources.getMetadataBuilder().applyPhysicalNamingStrategy(
					new PhysicalNamingStrategyStandardImpl() {
						@Override public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
							return Identifier.toIdentifier( name.getText() + "_" + handling.ordinal() + "_" + (metadataEnabled ? "1" : "0") );
						}
					} ).build();
			if ( !metadataEnabled ) {
				assertThat( provider.blockedAttempts ).isZero();
			}
			final var factory = metadata.buildSessionFactory();
			if ( !metadataEnabled ) {
				assertThat( provider.blockedAttempts ).isZero();
			}
			return new Boot( registry, metadata, factory );
		}
		catch (Throwable ex) { StandardServiceRegistryBuilder.destroy( registry ); throw ex; }
	}

	private record Boot(StandardServiceRegistry registry, Metadata metadata, SessionFactory factory) implements AutoCloseable {
		@Override public void close() { factory.close(); StandardServiceRegistryBuilder.destroy( registry ); }
	}

	private PreservationJdbcOracle.Storage resolve(Boot boot, Mapping<T> mapping, Handling handling, boolean metadataEnabled, PreservationEnvironment env) {
		final var dialect = boot.registry.requireService( JdbcServices.class ).getDialect();
		final var binding = boot.metadata.getEntityBinding( mapping.entity().getName() );
		final String expectedTable = mapping.entity().getAnnotation( jakarta.persistence.Table.class ).name() + "_" + handling.ordinal() + "_" + (metadataEnabled ? "1" : "0");
		assertThat( binding.getTable().getName() ).isEqualTo( expectedTable );
		assertThat( boot.registry.requireService( JdbcEnvironment.class ).getIdentifierHelper()
				.toMetaDataObjectName( Identifier.toIdentifier( expectedTable ) ) )
				.isEqualTo( env.pg() ? expectedTable.toLowerCase( java.util.Locale.ROOT ) : expectedTable.toUpperCase( java.util.Locale.ROOT ) );

		final var value = binding.getProperty( "value" ).getValue();
		final BasicValue primary;
		if ( value instanceof Component component ) {
			assertThat( mapping.column() ).isTrue();
			assertThat( component.getColumnSpan() ).isEqualTo( 2 );
			primary = (BasicValue) component.getProperties().stream().filter( p -> !p.getName().equals( "zoneOffset" ) )
					.findFirst().orElseThrow().getValue();
			final var offset = (BasicValue) component.getProperty( "zoneOffset" ).getValue();
			assertThat( offset.resolve().getJdbcType().getClass() ).isEqualTo( IntegerJdbcType.class );
			assertThat( dialect.getTemporalPreservation( offset.resolve().getDomainJavaType(), offset.resolve().getJdbcType() ).offset() )
					.isEqualTo( TemporalPreservation.Level.SUPPORTED );
		}
		else {
			assertThat( mapping.column() ).isFalse();
			primary = (BasicValue) value;
		}
		var jdbc = primary.resolve().getJdbcType();
		final var report = dialect.getTemporalPreservation( primary.resolve().getDomainJavaType(), jdbc );
		if ( jdbc instanceof UtcTimestampJdbcType utc ) {
			jdbc = utc.getDelegate();
		}
		if ( jdbc instanceof UtcTimeJdbcType utc ) {
			jdbc = utc.getDelegate();
		}
		final Class<?> expectedJdbc = expectedDescriptor( mapping, handling, env );
		assertThat( jdbc.getClass() ).as( "%s / %s / %s", domain, mapping, handling ).isEqualTo( expectedJdbc );
		final var required = new ArrayList<TemporalPreservation.TemporalPart>();
		if ( domain == LocalDate.class || domain == LocalDateTime.class ) {
			required.add( TemporalPreservation.TemporalPart.DATE );
		}
		if ( domain == LocalTime.class || domain == LocalDateTime.class || time() ) {
			required.add( TemporalPreservation.TemporalPart.TIME );
		}
		if ( !time() && (aware() || domain == Instant.class) ) {
			required.add( TemporalPreservation.TemporalPart.INSTANT );
		}
		if ( aware() && !mapping.column() && !mapping.adjusted() ) {
			required.add( TemporalPreservation.TemporalPart.OFFSET );
			if ( zoned() ) {
				required.add( TemporalPreservation.TemporalPart.ZONE_ID );
			}
		}
		for ( var part : required ) {
			assertThat( report.preserves( part ) ).as( "%s / %s", mapping, part )
					.isEqualTo( TemporalPreservation.Level.SUPPORTED );
		}
		final String sql = ((Column) primary.getColumn()).getSqlType( boot.metadata );
		if ( domain != LocalDate.class ) assertThat( ((Column) primary.getColumn()).getColumnSize( dialect, boot.metadata ).getPrecision() ).isEqualTo( mapping.precision() );
		final boolean withZone = expectedWithZone( mapping, handling, env );
		assertThat( sql.contains( "with time zone" ) ).as( "%s physical SQL: %s", mapping, sql ).isEqualTo( withZone );
		final boolean sqlTimestamp = expectedJdbc == TimestampUtcAsPlainJdbcTimestampJdbcType.class
				|| expectedJdbc == TimestampUtcAsZonedJdbcTimestampJdbcType.class || expectedJdbc == PostgreSQLTimestampUtcAsJdbcTimestampJdbcType.class
				|| expectedJdbc == org.hibernate.dialect.type.internal.OracleTimestampUtcAsJdbcTimestampJdbcType.class;
		final var storage = new PreservationJdbcOracle.Storage( expectedTable, sql, mapping.column(), withZone, env.oracle(), env.pg(), sqlTimestamp, domain );
		inventory( env, mapping, handling, metadataEnabled, "RESOLVED", jdbc.getClass().getSimpleName(), sql );
		return storage;
	}

	private Class<?> expectedDescriptor(Mapping<T> mapping, Handling handling, PreservationEnvironment env) {
		if ( domain == LocalDate.class ) {
			return LocalDateJdbcType.class;
		}
		if ( domain == LocalTime.class ) {
			return env.oracle() ? OracleLocalTimeJdbcType.class : env.pg() ? org.hibernate.dialect.type.internal.PostgreSQLLocalTimeJdbcType.class : LocalTimeJdbcType.class;
		}
		if ( domain == LocalDateTime.class ) {
			return LocalDateTimeJdbcType.class;
		}
		if ( mapping.explicitFallback() ) {
			return TimestampUtcAsPlainJdbcTimestampJdbcType.class;
		}
		if ( aware() && !mapping.column() && !mapping.adjusted() ) {
			if ( time() ) {
				return env.pg() ? PostgreSQLOffsetTimeJdbcType.class : env.oracle() ? OracleOffsetTimeJdbcType.class : OffsetTimeJdbcType.class;
			}
			if ( zoned() ) {
				return OracleZonedDateTimeJdbcType.class;
			}
			return env.oracle() ? OracleOffsetDateTimeJdbcType.class : OffsetDateTimeJdbcType.class;
		}
		if ( time() ) {
			return env.pg() ? PostgreSQLUtcTimeJdbcType.class : TimeUtcAsOffsetTimeJdbcType.class;
		}
		return switch ( handling ) {
			case FALLBACK_PLAIN -> TimestampUtcAsPlainJdbcTimestampJdbcType.class;
			case FALLBACK_ZONED -> env.pg() ? PostgreSQLTimestampUtcAsJdbcTimestampJdbcType.class
					: env.oracle() ? org.hibernate.dialect.type.internal.OracleTimestampUtcAsJdbcTimestampJdbcType.class : TimestampUtcAsZonedJdbcTimestampJdbcType.class;
			case NATURAL -> env.pg() ? TimestampUtcAsPlainJdbcTimestampJdbcType.class : TimestampUtcAsOffsetDateTimeJdbcType.class;
			default -> TimestampUtcAsOffsetDateTimeJdbcType.class;
		};
	}
	private boolean expectedWithZone(Mapping<T> mapping, Handling handling, PreservationEnvironment env) {
		if ( !aware() && domain != Instant.class ) {
			return false;
		}
		if ( mapping.explicitFallback() ) {
			return false;
		}
		if ( aware() && !mapping.column() && !mapping.adjusted() ) {
			return true;
		}
		return handling == Handling.DIRECT_ZONED || handling == Handling.FALLBACK_ZONED || handling == Handling.NATURAL && !env.pg();
	}

	private void lifecycle(
			SessionFactory factory,
			Mapping<T> mapping,
			PreservationJdbcOracle.Storage storage,
			Handling handling,
			boolean metadata,
			PreservationEnvironment env,
			Connection writer,
			Connection reader, GuardedConnectionProvider provider) throws Exception {
		final var catalog = new ArrayList<>( PreservationSamples.samples( domain, env.full() || env.profile().equals( "E3" ) || env.profile().equals( "E4" ) ) );
		if ( env.full() ) {
			final Class<?> descriptor = expectedDescriptor( mapping, handling, env );
			final boolean sqlTimestamp = descriptor == TimestampUtcAsPlainJdbcTimestampJdbcType.class
					|| descriptor == TimestampUtcAsZonedJdbcTimestampJdbcType.class || descriptor == PostgreSQLTimestampUtcAsJdbcTimestampJdbcType.class
					|| descriptor == org.hibernate.dialect.type.internal.OracleTimestampUtcAsJdbcTimestampJdbcType.class;
			catalog.addAll( PreservationRangeSamples.samples( domain, mapping, env, sqlTimestamp ) );
		}

		int id = 0;
		for ( var data : catalog ) {
			id++;
			if ( !env.full()
				&& id > (domain == Instant.class || domain == OffsetDateTime.class || zoned() ? 5 : 1)
				&& !data.rounding() ) {
				continue;
			}
			final T input = data.value();
			final boolean reject = data.rejected() || runtimeRejected( input, mapping, env );
			if ( reject ) {
				if ( env.full() || mapping.adjusted() ) {
					final int rowId = id;
					final int boundBefore = provider.temporalBindings;
					assertThatThrownBy( () -> writeHibernate( factory, writer, mapping, rowId, input, env ) )
							.as( "%s / %s", mapping, data ).isInstanceOf( TemporalValueException.class );
					assertThat( provider.temporalBindings ).as( "Reject the complete value before any temporal JDBC parameter binding" ).isEqualTo( boundBefore );
					inventory( env, mapping, handling, metadata, "VALUE_REJECTION:" + data.id(), storage.table(), input.toString() );
				}
				continue;
			}
			final Object expected = PreservationSamples.expected( input, mapping, env );
			if ( env.writer() ) {
				writeHibernate( factory, writer, mapping, id, input, env );
			}
			if ( env.reader() ) {
				env.prepare( reader, false );
				try ( var session = factory.withOptions().connection( reader ).openSession() ) {
					final var tx = session.beginTransaction();
					final var row = session.find( mapping.entity(), id );
					assertThat( row ).isNotNull();
					if ( env.cross() ) {
						assertThat( row.writerProcess ).isNotEqualTo( ProcessHandle.current().pid() );
						assertThat( row.writerJvm ).isEqualTo( env.writerJvm() ).isNotEqualTo( ZoneId.systemDefault().getId() );
					}
					assertValue( row.value(), expected );
					PreservationJdbcOracle.inspect( reader, storage, id, input, mapping, expected );
					row.value( null );
					tx.commit();
				}
				try ( var session = factory.withOptions().connection( reader ).openSession() ) {
					final var tx = session.beginTransaction();
					final var row = session.get( mapping.entity(), id );
					assertValue( row.value(), null );
					session.remove( row );
					tx.commit();
				}
				// Independent JDBC writer must encode the complete tuple, without production converters.
				env.prepare( writer, true );
				PreservationJdbcOracle.insert( writer, storage, id, input, mapping ); writer.commit();
				try ( var session = factory.withOptions().connection( reader ).openSession() ) {
					final var tx = session.beginTransaction();
					final var row = session.get( mapping.entity(), id );
					assertValue( row.value(), expected ); session.remove( row ); tx.commit();
				}
			}
			inventory( env, mapping, handling, metadata, env.reader() ? "ORM_AND_JDBC:" + data.id() : "WRITTEN:" + data.id(), storage.table(), input.toString() );
		}
		if ( env.reader() ) {
			writeHibernate( factory, writer, mapping, 1000, null, env );
			try ( var session = factory.withOptions().connection( reader ).openSession() ) {
				final var tx = session.beginTransaction();
				final var row = session.get( mapping.entity(), 1000 );
				assertValue( row.value(), null ); session.remove( row ); tx.commit();
			}
			inventory( env, mapping, handling, metadata, "NULL_LIFECYCLE", storage.table(), "null" );
			if ( env.full() && mapping.column() ) {
				invalidTuples( factory, mapping, storage, writer, reader, handling, metadata, env );
			}
			if ( env.full() && env.pg() && domain != LocalTime.class && domain != OffsetTime.class ) {
				for ( String endpoint : List.of( "infinity", "-infinity" ) ) {
					PreservationJdbcOracle.insert( writer, storage, 2001, catalog.get( 0 ).value(), mapping );
					try ( var statement = writer.createStatement() ) {
						statement.executeUpdate( "update " + storage.table() + " set temporal_value='" + endpoint + "' where id=2001" );
						writer.commit();
					}
					try ( var session = factory.withOptions().connection( reader ).openSession() ) {
						final var tx = session.beginTransaction();
						try {
							assertThatThrownBy( () -> session.get( mapping.entity(), 2001 ) )
									.isInstanceOf( TemporalValueException.class ).hasMessageContaining( "finite" );
						}
						finally { tx.rollback(); }
					}
					try ( var statement = writer.createStatement() ) {
						statement.executeUpdate( "delete from " + storage.table() + " where id=2001" ); writer.commit();
					}
					inventory( env, mapping, handling, metadata, "STORED_INFINITY_REJECTION:" + endpoint, storage.table(), endpoint );
				}
			}
		}
	}

	private boolean runtimeRejected(T input, Mapping<T> mapping, PreservationEnvironment env) {
		if ( !aware() || mapping.column() || mapping.adjusted() ) {
			return false;
		}
		final Object adjusted = PreservationSamples.adjust( input, mapping.adjustment() );
		if ( env.oracle() && adjusted instanceof ZonedDateTime time && time.getZone().getId().equals( "GMT+05:30" ) ) return true;
		final ZoneOffset offset = adjusted instanceof OffsetTime t ? t.getOffset() : adjusted instanceof OffsetDateTime t ? t.getOffset() : ((ZonedDateTime) adjusted).getOffset();
		return env.oracle() && offset.getTotalSeconds() % 60 != 0 || env.pg() && time() && Math.abs( offset.getTotalSeconds() ) >= 16 * 3600;
	}

	private void writeHibernate(SessionFactory factory, Connection connection, Mapping<T> mapping, int id, T input, PreservationEnvironment env) throws Exception {
		env.prepare( connection, true );
		try ( var session = factory.withOptions().connection( connection ).openSession() ) {
			final var tx = session.beginTransaction();
			try {
				final var row = mapping.entity().getDeclaredConstructor().newInstance();
				row.id = id; row.writerProcess = ProcessHandle.current().pid(); row.writerJvm = ZoneId.systemDefault().getId(); row.value( input );
				session.persist( row ); tx.commit();
				assertValue( row.value(), input );
			}
			catch (Throwable ex) { if ( tx.isActive() ) tx.rollback(); throw ex; }
		}
	}

	private void invalidTuples(SessionFactory factory, Mapping<T> mapping, PreservationJdbcOracle.Storage storage, Connection writer, Connection reader,
			Handling handling, boolean metadata, PreservationEnvironment env) throws Exception {
		final Object input = PreservationSamples.samples( domain, false ).get( 0 ).value();
		for ( String corruption : List.of( "temporal_value=null", "offset_seconds=null", "offset_seconds=64801", "offset_seconds=-64801" ) ) {
			PreservationJdbcOracle.insert( writer, storage, 2000, input, mapping );
			try ( var statement = writer.createStatement() ) {
				statement.executeUpdate( "update " + storage.table() + " set " + corruption + " where id=2000" ); writer.commit();
			}
			try ( var session = factory.withOptions().connection( reader ).openSession() ) {
				final var tx = session.beginTransaction();
				try {
					final var failure = assertThatThrownBy( () -> session.find( mapping.entity(), 2000 ) );
					if ( corruption.endsWith( "=null" ) ) failure.isInstanceOf( TemporalValueException.class ).hasMessageContaining( "COLUMN tuple" );
					else failure.isInstanceOf( RuntimeException.class );
				}
				finally { tx.rollback(); }
			}
			try ( var statement = writer.createStatement() ) { statement.executeUpdate( "delete from " + storage.table() + " where id=2000" ); writer.commit(); }
			inventory( env, mapping, handling, metadata, "STORED_TUPLE_REJECTION:" + corruption, storage.table(), corruption );
		}
		try ( var statement = writer.createStatement() ) {
			statement.executeUpdate( "insert into " + storage.table() + " (id, writerProcess) values (2000, 0)" ); writer.commit();
		}
		try ( var session = factory.withOptions().connection( reader ).openSession() ) {
			final var tx = session.beginTransaction(); final var row = session.find( mapping.entity(), 2000 );
			assertValue( row.value(), null ); session.remove( row ); tx.commit();
		}
	}

	static void assertValue(Object actual, Object expected) {
		// Object.equals is exact for java.time values, unlike timeline-oriented assertion overloads.
		assertThat( Objects.equals( actual, expected ) ).withFailMessage( "Expected exact value %s but got %s", expected, actual ).isTrue();
	}

	private void inventory(PreservationEnvironment env, Mapping<T> mapping, Handling handling, boolean metadata, String disposition, String descriptor, String sql) {
		System.out.println( "TP_CASE\t" + env.platform() + "\t" + domain.getSimpleName() + "\t" + mapping.id() + "\t" + handling
				+ "\tprecision=" + (domain == LocalDate.class ? "n/a" : mapping.precision()) + "\tadjustment=" + mapping.adjustment() + "\tmetadata=" + metadata + "\t" + env.profile()
				+ "\t" + disposition + "\t" + descriptor + "\t" + sql );
	}
}
