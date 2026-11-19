package org.hibernate.orm.test.type.temporal.preservation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcType;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsLocalTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsInstantJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsOffsetDateTimeJdbcType;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.SybaseDialect;

import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// TEMPORARY probe, not to be merged. Exercises each carrier through Hibernate
/// on the databases whose Dialects have not been assessed for temporal
/// preservation (CockroachDB, HANA, Spanner in both of its dialects, and
/// Sybase ASE), without preservation guards, under varied
/// JVM time zones, once with `java.sql` conversions and once with direct
/// `java.time` access. It asserts nothing and never fails. Each result is
/// printed on a line starting with `TP-PROBE|`, which the build copies to its
/// own log.
class ZzDialectCarrierProbe {
	private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern( "uuuu-MM-dd HH:mm:ss" );
	private static final String[] WRITER_ZONES = { "UTC", "America/New_York", "Asia/Tokyo", "America/Sao_Paulo" };

	private String variant;
	private String dialectName;

	private final List<String> lines = new ArrayList<>();

	private void out(String line) {
		lines.add( variant + "|" + line );
		System.out.println( "TP-PROBE|carriers|" + dialectName + "|" + variant + "|" + line );
	}

	/// The results in brief, as the names of tests, since a build server
	/// may truncate what a test prints but keeps the names of its tests.
	@TestFactory
	List<DynamicTest> probe() throws Exception {
		run();
		final List<DynamicTest> tests = new ArrayList<>();
		for ( String name : digest() ) {
			tests.add( DynamicTest.dynamicTest( name.length() > 400 ? name.substring( 0, 400 ) : name, () -> {
			} ) );
		}
		return tests;
	}

	private List<String> digest() {
		final Map<String, Map<String, Integer>> counts = new LinkedHashMap<>();
		final Map<String, Set<String>> anomalies = new LinkedHashMap<>();
		final List<String> digest = new ArrayList<>();
		for ( String line : lines ) {
			final String[] parts = line.split( "\\|", 5 );
			if ( parts.length < 5 ) {
				digest.add( dialectName + " " + line );
				continue;
			}
			final String key = parts[0] + " " + parts[1];
			final String value = parts[3];
			final String rest = parts[4];
			final var count = counts.computeIfAbsent( key, k -> new LinkedHashMap<>() );
			final var anomaly = anomalies.computeIfAbsent( key, k -> new LinkedHashSet<>() );
			final boolean written = !value.startsWith( "native" );
			if ( value.equals( "*" ) ) {
				count.merge( rest, 1, Integer::sum );
			}
			else if ( rest.startsWith( "write !!" ) ) {
				count.merge( written ? "writeFailed" : "nativeWriteFailed", 1, Integer::sum );
				anomaly.add( value + " " + rest );
			}
			else {
				final int read = rest.indexOf( "|read=" );
				final int other = rest.indexOf( "|readJvmChatham=" );
				if ( read < 0 || other < 0 ) {
					count.merge( "unparsed", 1, Integer::sum );
					continue;
				}
				final String here = rest.substring( read + 6, other );
				final String there = rest.substring( other + 16 );
				count.merge( written ? "rows" : "native", 1, Integer::sum );
				if ( written && here.equals( value ) ) {
					count.merge( "exact", 1, Integer::sum );
				}
				else {
					anomaly.add( value + " text=" + rest.substring( 5, Math.min( read, 45 ) ) + " read=" + here );
				}
				if ( here.equals( there ) ) {
					count.merge( written ? "stable" : "nativeStable", 1, Integer::sum );
				}
				else {
					anomaly.add( value + " depends on the JVM zone: " + here + " / " + there );
				}
			}
		}
		for ( var entry : counts.entrySet() ) {
			digest.add( dialectName + " " + entry.getKey() + " " + entry.getValue() );
			int n = 0;
			for ( String anomaly : anomalies.get( entry.getKey() ) ) {
				digest.add( dialectName + " " + entry.getKey() + " #" + ( ++n ) + " " + anomaly );
				if ( n == 40 ) {
					break;
				}
			}
		}
		return digest;
	}

	private void run() throws Exception {
		final var dialect = DialectContext.getDialect();
		assumeTrue( dialect instanceof CockroachDialect
				|| dialect instanceof HANADialect
				|| dialect instanceof SpannerDialect
				|| dialect instanceof SpannerPostgreSQLDialect
				|| dialect instanceof SybaseDialect
				|| System.getProperty( "hibernate.test.temporal_preservation.probe" ) != null );
		dialectName = dialect.getClass().getSimpleName();
		try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder().applySetting( "hibernate.hbm2ddl.auto", "none" ).build();
				var factory = (SessionFactoryImplementor) new MetadataSources( registry ).buildMetadata().buildSessionFactory() ) {
			variant = "server";
			factory.inSession( session -> session.doWork( connection -> out( "version|"
					+ connection.getMetaData().getDatabaseProductName() + " "
					+ connection.getMetaData().getDatabaseProductVersion().replace( '\n', ' ' ) + "|driver "
					+ connection.getMetaData().getDriverName() + " " + connection.getMetaData().getDriverVersion() ) ) );
		}
		catch (Throwable e) {
			out( "version|!! " + brief( e ) );
		}
		final Map<Class<?>, List<Object>> cases = new LinkedHashMap<>();
		final List<LocalDate> dates = List.of(
				LocalDate.of( 1, 1, 1 ), LocalDate.of( 999, 12, 31 ), LocalDate.of( 1000, 1, 1 ),
				LocalDate.of( 1582, 10, 4 ), LocalDate.of( 1582, 10, 10 ), LocalDate.of( 1582, 10, 15 ),
				LocalDate.of( 1899, 12, 31 ), LocalDate.of( 1970, 1, 1 ), LocalDate.of( 2018, 11, 4 ), LocalDate.of( 2024, 7, 1 ),
				LocalDate.of( 9999, 12, 31 )
		);
		cases.put( PLocalDate.class, new ArrayList<>( dates ) );
		cases.put( PLocalTime.class, List.of( LocalTime.of( 0, 0 ), LocalTime.of( 12, 34, 56 ), LocalTime.of( 23, 59, 59 ) ) );
		final List<Object> dateTimes = new ArrayList<>();
		for ( var date : dates ) {
			dateTimes.add( date.atTime( 12, 34, 56 ) );
		}
		dateTimes.add( LocalDateTime.of( 1, 1, 1, 0, 0 ) );
		dateTimes.add( LocalDateTime.of( 2018, 11, 4, 0, 30 ) );
		dateTimes.add( LocalDateTime.of( 2024, 3, 10, 2, 30 ) );
		dateTimes.add( LocalDateTime.of( 2024, 11, 3, 1, 30 ) );
		dateTimes.add( LocalDateTime.of( 9999, 12, 31, 23, 59, 59 ) );
		cases.put( PLocalDateTime.class, dateTimes );
		final List<Instant> instants = List.of(
				Instant.parse( "0001-01-01T00:00:00Z" ), Instant.parse( "1000-01-01T00:00:00Z" ),
				Instant.parse( "1582-10-04T12:00:00Z" ), Instant.parse( "1582-10-14T23:59:59Z" ),
				Instant.parse( "1582-10-15T00:00:00Z" ), Instant.parse( "1582-12-31T23:00:00Z" ),
				Instant.parse( "1583-01-01T00:00:00Z" ), Instant.parse( "1899-12-31T23:59:59Z" ),
				Instant.parse( "1970-01-01T00:00:00Z" ), Instant.parse( "2024-03-10T07:30:00Z" ),
				Instant.parse( "2024-11-03T05:30:00Z" ), Instant.parse( "2024-11-03T06:30:00Z" ),
				Instant.parse( "2038-01-19T03:14:08Z" ), Instant.parse( "9999-12-31T23:59:59Z" )
		);
		for ( var type : List.of( PInstant.class, PInstantAsJdbcTimestamp.class, PInstantAsInstant.class, PInstantAsOffsetDateTime.class ) ) {
			cases.put( type, new ArrayList<>( instants ) );
		}
		final List<Object> offsets = new ArrayList<>();
		final List<Object> zoned = new ArrayList<>();
		for ( var instant : instants ) {
			offsets.add( instant.atOffset( ZoneOffset.ofHoursMinutes( 5, 45 ) ) );
			zoned.add( instant.atZone( ZoneId.of( "Europe/Paris" ) ) );
		}
		cases.put( POffsetDateTime.class, offsets );
		cases.put( PZonedDateTime.class, zoned );
		final List<Object> times = List.of(
				OffsetTime.parse( "10:15:30+02:00" ), OffsetTime.parse( "00:00:00Z" ), OffsetTime.parse( "01:30:00+05:00" ),
				OffsetTime.parse( "22:30:00-05:00" ), OffsetTime.parse( "00:00:00+18:00" ), OffsetTime.parse( "23:59:59-18:00" )
		);
		cases.put( POffsetTime.class, times );
		cases.put( POffsetTimeAsLocalTime.class, times );

		final TimeZone original = TimeZone.getDefault();
		try {
			for ( boolean direct : new boolean[] { false, true } ) {
				variant = direct ? "direct" : "java.sql";
				for ( var entry : cases.entrySet() ) {
					for ( String writerZone : WRITER_ZONES ) {
						TimeZone.setDefault( TimeZone.getTimeZone( writerZone ) );
						try {
							probe( entry.getKey(), entry.getValue(), writerZone, direct );
						}
						catch (Throwable e) {
							out( entry.getKey().getSimpleName() + "|" + writerZone + "|*|factory !! " + brief( e ) );
						}
					}
				}
			}
		}
		finally {
			TimeZone.setDefault( original );
		}
	}

	private void probe(Class<?> entityClass, List<Object> values, String writerZone, boolean direct) throws Exception {
		final String name = entityClass.getSimpleName();
		final var builder = ServiceRegistryUtil.serviceRegistryBuilder();
		builder.applySetting( "hibernate.hbm2ddl.auto", "create-drop" );
		builder.applySetting( MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, direct );
		builder.applySetting( MappingSettings.TEMPORAL_PRESERVATION, false );
		try ( var registry = builder.build();
				var factory = (SessionFactoryImplementor) new MetadataSources( registry ).addAnnotatedClass( entityClass ).buildMetadata().buildSessionFactory() ) {
			final var basicValue = factory.getMappingMetamodel().getEntityDescriptor( entityClass ).findAttributeMapping( "theValue" ).getSingleJdbcMapping();
			out( name + "|" + writerZone + "|*|carrier=" + basicValue.getJdbcType().getClass().getSimpleName() );
			int id = 0;
			for ( Object value : values ) {
				final int rowId = ++id;
				final String prefix = name + "|" + writerZone + "|" + value + "|";
				try {
					final Object entity = entityClass.getDeclaredConstructor().newInstance();
					entityClass.getField( "id" ).set( entity, rowId );
					entityClass.getField( "theValue" ).set( entity, value );
					factory.inTransaction( session -> session.persist( entity ) );
				}
				catch (Throwable e) {
					out( prefix + "write !! " + brief( e ) );
					continue;
				}
				final String table = "tp_probe_" + name.toLowerCase();
				final StringBuilder line = new StringBuilder( prefix );
				line.append( "text=" ).append( read( factory, entityClass, table, rowId, null, true ) );
				line.append( "|read=" ).append( read( factory, entityClass, table, rowId, null, false ) );
				TimeZone.setDefault( TimeZone.getTimeZone( "Pacific/Chatham" ) );
				line.append( "|readJvmChatham=" ).append( read( factory, entityClass, table, rowId, null, false ) );
				TimeZone.setDefault( TimeZone.getTimeZone( writerZone ) );
				out( line.toString() );
			}
			// rows written by plain SQL, which the driver must read as they are
			for ( String literal : NATIVE.getOrDefault( entityClass, List.of() ) ) {
				final int rowId = ++id;
				final String table = "tp_probe_" + name.toLowerCase();
				final String prefix = name + "|" + writerZone + "|native " + literal + "|";
				try {
					factory.inTransaction( session -> session.createNativeMutationQuery(
							"insert into " + table + " (id, theValue) values (" + rowId + ", '" + literal + "')" ).executeUpdate() );
				}
				catch (Throwable e) {
					out( prefix + "write !! " + brief( e ) );
					continue;
				}
				final StringBuilder line = new StringBuilder( prefix );
				line.append( "text=" ).append( read( factory, entityClass, table, rowId, null, true ) );
				line.append( "|read=" ).append( read( factory, entityClass, table, rowId, null, false ) );
				TimeZone.setDefault( TimeZone.getTimeZone( "Pacific/Chatham" ) );
				line.append( "|readJvmChatham=" ).append( read( factory, entityClass, table, rowId, null, false ) );
				TimeZone.setDefault( TimeZone.getTimeZone( writerZone ) );
				out( line.toString() );
			}
		}
	}

	private static final List<String> NATIVE_UTC_FIELDS = List.of(
			"0001-01-01 00:00:00", "1000-01-01 00:00:00", "1582-10-04 12:00:00",
			"1582-10-14 23:59:59", "1582-10-15 00:00:00", "1582-12-31 23:00:00",
			"1900-01-01 00:00:00", "2024-07-01 06:15:00", "2024-07-01 24:00:00"
	);
	private static final Map<Class<?>, List<String>> NATIVE = Map.of(
			PLocalDate.class, List.of( "0001-01-01", "1582-10-04", "1582-10-10", "1582-10-15", "1900-01-01" ),
			PLocalDateTime.class, List.of( "0001-01-01 00:00:00", "1582-10-04 12:00:00", "1582-10-10 12:00:00", "1582-10-15 00:00:00", "1900-01-01 00:00:00", "2024-03-10 02:30:00", "2024-07-01 24:00:00" ),
			PInstant.class, NATIVE_UTC_FIELDS,
			PInstantAsJdbcTimestamp.class, NATIVE_UTC_FIELDS,
			PInstantAsOffsetDateTime.class, NATIVE_UTC_FIELDS,
			PLocalTime.class, List.of( "00:00:00", "23:59:59", "24:00:00" ),
			POffsetDateTime.class, NATIVE_UTC_FIELDS,
			PZonedDateTime.class, NATIVE_UTC_FIELDS
	);

	private Object read(SessionFactoryImplementor factory, Class<?> entityClass, String table, int id, String sessionZone, boolean text) {
		try {
			return factory.fromTransaction( session -> {
				try {
					if ( text ) {
						return session.createNativeQuery( "select cast(theValue as varchar(60)) from " + table + " where id = " + id, String.class ).getSingleResult();
					}
					final Object entity = session.find( entityClass, id );
					return normalize( entityClass.getField( "theValue" ).get( entity ) );
				}
				catch (ReflectiveOperationException e) {
					throw new IllegalStateException( e );
				}
			} );
		}
		catch (Throwable e) {
			return "!! " + brief( e );
		}
	}

	/// Values with an offset or zone are compared by what a single column can keep.
	private static Object normalize(Object value) {
		if ( value instanceof OffsetDateTime offsetDateTime ) {
			return offsetDateTime.toInstant() + "/" + offsetDateTime.getOffset();
		}
		if ( value instanceof ZonedDateTime zonedDateTime ) {
			return zonedDateTime.toInstant();
		}
		if ( value instanceof OffsetTime offsetTime ) {
			return offsetTime.withOffsetSameInstant( ZoneOffset.UTC ).toLocalTime() + "@UTC/" + offsetTime.getOffset();
		}
		return value;
	}

	private static String brief(Throwable e) {
		Throwable root = e;
		while ( root.getCause() != null ) {
			root = root.getCause();
		}
		final String message = String.valueOf( root.getMessage() ).replace( '\n', ' ' );
		return root.getClass().getSimpleName() + ": " + ( message.length() > 140 ? message.substring( 0, 140 ) : message );
	}

	@Entity(name = "PLocalDate") @Table(name = "tp_probe_plocaldate")
	public static class PLocalDate { @Id public Integer id; public LocalDate theValue; }

	@Entity(name = "PLocalTime") @Table(name = "tp_probe_plocaltime")
	public static class PLocalTime { @Id public Integer id; public LocalTime theValue; }

	@Entity(name = "PLocalDateTime") @Table(name = "tp_probe_plocaldatetime")
	public static class PLocalDateTime { @Id public Integer id; public LocalDateTime theValue; }

	@Entity(name = "PInstant") @Table(name = "tp_probe_pinstant")
	public static class PInstant { @Id public Integer id; public Instant theValue; }

	@Entity(name = "PInstantAsJdbcTimestamp") @Table(name = "tp_probe_pinstantasjdbctimestamp")
	public static class PInstantAsJdbcTimestamp { @Id public Integer id; @JdbcType(TimestampUtcAsJdbcTimestampJdbcType.class) public Instant theValue; }

	@Entity(name = "PInstantAsInstant") @Table(name = "tp_probe_pinstantasinstant")
	public static class PInstantAsInstant { @Id public Integer id; @JdbcType(TimestampUtcAsInstantJdbcType.class) public Instant theValue; }

	@Entity(name = "PInstantAsOffsetDateTime") @Table(name = "tp_probe_pinstantasoffsetdatetime")
	public static class PInstantAsOffsetDateTime { @Id public Integer id; @JdbcType(TimestampUtcAsOffsetDateTimeJdbcType.class) public Instant theValue; }

	@Entity(name = "POffsetDateTime") @Table(name = "tp_probe_poffsetdatetime")
	public static class POffsetDateTime { @Id public Integer id; public OffsetDateTime theValue; }

	@Entity(name = "PZonedDateTime") @Table(name = "tp_probe_pzoneddatetime")
	public static class PZonedDateTime { @Id public Integer id; public ZonedDateTime theValue; }

	@Entity(name = "POffsetTime") @Table(name = "tp_probe_poffsettime")
	public static class POffsetTime { @Id public Integer id; public OffsetTime theValue; }

	@Entity(name = "POffsetTimeAsLocalTime") @Table(name = "tp_probe_poffsettimeaslocaltime")
	public static class POffsetTimeAsLocalTime { @Id public Integer id; @JdbcType(TimeUtcAsLocalTimeJdbcType.class) public OffsetTime theValue; }
}
