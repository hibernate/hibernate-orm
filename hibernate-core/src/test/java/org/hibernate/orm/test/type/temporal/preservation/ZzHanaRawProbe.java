package org.hibernate.orm.test.type.temporal.preservation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.hibernate.boot.MetadataSources;
import org.hibernate.dialect.HANADialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// TEMPORARY probe, not to be merged. Exchanges a `LocalDate` and a
/// `LocalDateTime` with HANA over plain JDBC, by every route which might
/// preserve them: passed to and requested from the driver directly, and as
/// text which the database converts. Each route is tried under several
/// default time zones of the JVM, each with connections of its own.
///
/// What the database holds is judged by the database itself, by comparing
/// the column with a literal, so no read through the driver is trusted
/// before it has been probed. Reads are judged against rows which the
/// database wrote from literals.
///
/// It asserts nothing and never fails. The results are the names of the
/// tests it returns, since the build server which runs HANA keeps those.
class ZzHanaRawProbe {
	private static final String[] ZONES = { "UTC", "America/New_York", "Asia/Kathmandu" };
	private static final String TABLE = "zz_tp_hana_probe";

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern( "uuuu-MM-dd" );
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern( "uuuu-MM-dd HH:mm:ss.SSSSSSS" );

	private final Map<String, int[]> counts = new LinkedHashMap<>();
	private final Map<String, List<String>> anomalies = new LinkedHashMap<>();
	private final List<String> notes = new ArrayList<>();
	private int nextId = 1000;

	@TestFactory
	List<DynamicTest> probe() {
		final var dialect = DialectContext.getDialect();
		assumeTrue( dialect instanceof HANADialect
				|| System.getProperty( "hibernate.test.temporal_preservation.probe" ) != null );
		final TimeZone original = TimeZone.getDefault();
		try {
			run();
		}
		catch (Throwable e) {
			notes.add( "probe !! " + brief( e ) );
		}
		finally {
			TimeZone.setDefault( original );
		}
		final List<DynamicTest> tests = new ArrayList<>();
		for ( String name : digest() ) {
			tests.add( DynamicTest.dynamicTest( name.length() > 400 ? name.substring( 0, 400 ) : name, () -> {
			} ) );
		}
		return tests;
	}

	private List<String> digest() {
		final List<String> digest = new ArrayList<>();
		for ( String note : notes ) {
			digest.add( "HANAraw " + note );
		}
		for ( var entry : counts.entrySet() ) {
			digest.add( "HANAraw " + entry.getKey() + " ok=" + entry.getValue()[0] + "/" + entry.getValue()[1] );
			int n = 0;
			for ( String anomaly : anomalies.getOrDefault( entry.getKey(), List.of() ) ) {
				digest.add( "HANAraw " + entry.getKey() + " #" + ( ++n ) + " " + anomaly );
			}
		}
		return digest;
	}

	private void record(String key, boolean ok, String detail) {
		final int[] count = counts.computeIfAbsent( key, k -> new int[2] );
		count[1]++;
		if ( ok ) {
			count[0]++;
		}
		else {
			final var list = anomalies.computeIfAbsent( key, k -> new ArrayList<>() );
			if ( list.size() < 14 && !list.contains( detail ) ) {
				list.add( detail );
			}
		}
	}

	/// What differs between the two kinds of value.
	private abstract static class Kind {
		final String name;
		final String column;
		final String columnType;
		final int sqlType;
		final Class<?> javaClass;
		final List<Object> values;

		Kind(String name, String column, String columnType, int sqlType, Class<?> javaClass, List<Object> values) {
			this.name = name;
			this.column = column;
			this.columnType = columnType;
			this.sqlType = sqlType;
			this.javaClass = javaClass;
			this.values = values;
		}

		abstract String text(Object value);

		/// The value as a literal which the database itself converts.
		abstract String literal(Object value);

		/// The conversion of a parameter given as text.
		abstract String conversion();

		/// The column as text formatted by the database.
		abstract String formatted();

		abstract Object parse(String text);

		abstract void bindJdbc(PreparedStatement statement, int index, Object value) throws SQLException;

		abstract Object readJdbc(ResultSet results, int index) throws SQLException;
	}

	private static final Kind DATES = new Kind(
			"LocalDate", "d", "date", Types.DATE, LocalDate.class,
			List.of(
					LocalDate.of( 1, 1, 1 ), LocalDate.of( 1582, 10, 4 ), LocalDate.of( 1582, 10, 10 ),
					LocalDate.of( 1582, 10, 15 ), LocalDate.of( 1899, 12, 31 ), LocalDate.of( 1970, 1, 1 ),
					LocalDate.of( 2024, 3, 10 ), LocalDate.of( 9999, 12, 31 )
			)
	) {
		@Override
		String text(Object value) {
			return DATE.format( (LocalDate) value );
		}

		@Override
		String literal(Object value) {
			return "to_date('" + text( value ) + "','YYYY-MM-DD')";
		}

		@Override
		String conversion() {
			return "to_date(?,'YYYY-MM-DD')";
		}

		@Override
		String formatted() {
			return "to_varchar(d,'YYYY-MM-DD')";
		}

		@Override
		Object parse(String text) {
			return LocalDate.parse( text.trim().substring( 0, 10 ) );
		}

		@Override
		void bindJdbc(PreparedStatement statement, int index, Object value) throws SQLException {
			statement.setDate( index, java.sql.Date.valueOf( (LocalDate) value ) );
		}

		@Override
		Object readJdbc(ResultSet results, int index) throws SQLException {
			final var date = results.getDate( index );
			return date == null ? null : date.toLocalDate();
		}
	};

	private static final Kind DATE_TIMES = new Kind(
			"LocalDateTime", "ts", "timestamp", Types.TIMESTAMP, LocalDateTime.class,
			List.of(
					LocalDateTime.of( 1, 1, 1, 0, 0 ), LocalDateTime.of( 1582, 10, 4, 12, 34, 56 ),
					LocalDateTime.of( 1582, 10, 10, 12, 34, 56 ), LocalDateTime.of( 1582, 10, 15, 0, 0 ),
					LocalDateTime.of( 1899, 12, 31, 23, 59, 59 ), LocalDateTime.of( 1970, 1, 1, 0, 0 ),
					// skipped and repeated in New York
					LocalDateTime.of( 2024, 3, 10, 2, 30 ), LocalDateTime.of( 2024, 11, 3, 1, 30 ),
					// Kathmandu changed its offset at the start of 1986
					LocalDateTime.of( 1986, 1, 1, 0, 5 ),
					LocalDateTime.of( 2021, 6, 15, 13, 14, 15, 123_456_700 ),
					LocalDateTime.of( 9999, 12, 31, 23, 59, 59, 999_999_900 )
			)
	) {
		@Override
		String text(Object value) {
			return DATE_TIME.format( (LocalDateTime) value );
		}

		@Override
		String literal(Object value) {
			return "to_timestamp('" + text( value ) + "','YYYY-MM-DD HH24:MI:SS.FF7')";
		}

		@Override
		String conversion() {
			return "to_timestamp(?,'YYYY-MM-DD HH24:MI:SS.FF7')";
		}

		@Override
		String formatted() {
			return "to_varchar(ts,'YYYY-MM-DD HH24:MI:SS.FF7')";
		}

		@Override
		Object parse(String text) {
			final String trimmed = text.trim();
			return LocalDateTime.parse( trimmed.substring( 0, 10 ) + "T" + trimmed.substring( 11 ) );
		}

		@Override
		void bindJdbc(PreparedStatement statement, int index, Object value) throws SQLException {
			statement.setTimestamp( index, java.sql.Timestamp.valueOf( (LocalDateTime) value ) );
		}

		@Override
		Object readJdbc(ResultSet results, int index) throws SQLException {
			final var timestamp = results.getTimestamp( index );
			return timestamp == null ? null : timestamp.toLocalDateTime();
		}
	};

	private interface Binding {
		void bind(PreparedStatement statement, int index, Kind kind, Object value) throws SQLException;
	}

	/// A way to hand a value to the database: the SQL around the parameter, and how it is bound.
	private record WriteRoute(String name, boolean converted, boolean cast, Binding binding) {
		String expression(Kind kind) {
			if ( converted ) {
				return kind.conversion();
			}
			return cast ? "cast(cast(? as nvarchar(40)) as " + kind.columnType + ")" : "?";
		}
	}

	private static final List<WriteRoute> WRITE_ROUTES = List.of(
			new WriteRoute( "setObject", false, false,
					(statement, index, kind, value) -> statement.setObject( index, value ) ),
			new WriteRoute( "setObjectTyped", false, false,
					(statement, index, kind, value) -> statement.setObject( index, value, kind.sqlType ) ),
			new WriteRoute( "setString", false, false,
					(statement, index, kind, value) -> statement.setString( index, kind.text( value ) ) ),
			new WriteRoute( "setStringConverted", true, false,
					(statement, index, kind, value) -> statement.setString( index, kind.text( value ) ) ),
			new WriteRoute( "setStringCast", false, true,
					(statement, index, kind, value) -> statement.setString( index, kind.text( value ) ) ),
			new WriteRoute( "javaSql", false, false,
					(statement, index, kind, value) -> kind.bindJdbc( statement, index, value ) )
	);

	private static final List<String> READ_ROUTES = List.of( "getObject", "getString", "formattedText", "javaSql" );

	private void run() throws Exception {
		// rows written by the database from literals, under no particular time zone
		TimeZone.setDefault( TimeZone.getTimeZone( "UTC" ) );
		inConnection( connection -> {
			try {
				execute( connection, "drop table " + TABLE );
			}
			catch (SQLException ignore) {
			}
			execute( connection, "create table " + TABLE + " (id integer not null primary key, d date, ts timestamp)" );
			notes.add( "server " + connection.getMetaData().getDatabaseProductVersion() + " driver "
					+ connection.getMetaData().getDriverVersion() );
			for ( Kind kind : List.of( DATES, DATE_TIMES ) ) {
				for ( int i = 0; i < kind.values.size(); i++ ) {
					final Object value = kind.values.get( i );
					try {
						execute( connection, "insert into " + TABLE + " (id, " + kind.column + ") values ("
								+ nativeId( kind, i ) + ", " + kind.literal( value ) + ")" );
					}
					catch (SQLException e) {
						notes.add( kind.name + " literal " + kind.text( value ) + " !! " + brief( e ) );
					}
				}
				execute( connection, "insert into " + TABLE + " (id) values (" + nativeId( kind, 99 ) + ")" );
			}
			// what the database makes of each literal, by its own arithmetic
			for ( int i = 0; i < DATES.values.size(); i++ ) {
				try ( var results = connection.createStatement().executeQuery(
						"select days_between(to_date('1970-01-01','YYYY-MM-DD'), d), " + DATES.formatted()
								+ " from " + TABLE + " where id = " + nativeId( DATES, i ) ) ) {
					if ( results.next() ) {
						final long expected = ( (LocalDate) DATES.values.get( i ) ).toEpochDay();
						record( "LocalDate literal epochDay", results.getLong( 1 ) == expected,
								DATES.text( DATES.values.get( i ) ) + " days=" + results.getLong( 1 ) + " expected=" + expected
										+ " text=" + results.getString( 2 ) );
					}
				}
				catch (SQLException e) {
					record( "LocalDate literal epochDay", false, DATES.text( DATES.values.get( i ) ) + " !! " + brief( e ) );
				}
			}
		} );

		try {
			for ( String zone : ZONES ) {
				TimeZone.setDefault( TimeZone.getTimeZone( zone ) );
				inConnection( connection -> {
					for ( Kind kind : List.of( DATES, DATE_TIMES ) ) {
						reads( connection, kind, zone );
						writes( connection, kind, zone );
					}
				} );
			}
		}
		finally {
			TimeZone.setDefault( TimeZone.getTimeZone( "UTC" ) );
			inConnection( connection -> execute( connection, "drop table " + TABLE ) );
		}
	}

	/// Read the rows which the database wrote, by each route.
	private void reads(Connection connection, Kind kind, String zone) {
		for ( String route : READ_ROUTES ) {
			final String key = kind.name + " read:" + route;
			final String selected = route.equals( "formattedText" ) ? kind.formatted() : kind.column;
			for ( int i = 0; i <= kind.values.size(); i++ ) {
				final boolean isNull = i == kind.values.size();
				final Object expected = isNull ? null : kind.values.get( i );
				final String described = zone + " " + ( isNull ? "null" : kind.text( expected ) );
				try ( var statement = connection.prepareStatement(
						"select " + selected + " from " + TABLE + " where id = ?" ) ) {
					statement.setInt( 1, nativeId( kind, isNull ? 99 : i ) );
					try ( var results = statement.executeQuery() ) {
						if ( !results.next() ) {
							record( key, false, described + " no row" );
							continue;
						}
						final Object read;
						final String shown;
						switch ( route ) {
							case "getObject" -> {
								read = results.getObject( 1, kind.javaClass );
								shown = String.valueOf( read );
							}
							case "javaSql" -> {
								read = kind.readJdbc( results, 1 );
								shown = String.valueOf( read );
							}
							default -> {
								shown = results.getString( 1 );
								read = shown == null ? null : kind.parse( shown );
							}
						}
						record( key, isNull ? read == null : expected.equals( read ), described + " read=" + shown );
					}
				}
				catch (Throwable e) {
					record( key, false, described + " !! " + brief( e ) );
				}
			}
		}
	}

	/// Write each value by each route, then ask the database what it holds. Also
	/// look for the row which the database wrote, with the value given the same way.
	private void writes(Connection connection, Kind kind, String zone) {
		for ( WriteRoute route : WRITE_ROUTES ) {
			final String expression = route.expression( kind );
			for ( int i = 0; i < kind.values.size(); i++ ) {
				final Object value = kind.values.get( i );
				final String described = zone + " " + kind.text( value );
				final int id = nextId++;
				final String writeKey = kind.name + " write:" + route.name;
				try ( var statement = connection.prepareStatement(
						"insert into " + TABLE + " (id, " + kind.column + ") values (?, " + expression + ")" ) ) {
					statement.setInt( 1, id );
					route.binding.bind( statement, 2, kind, value );
					statement.executeUpdate();
					try ( var results = connection.createStatement().executeQuery(
							"select case when " + kind.column + " = " + kind.literal( value ) + " then 1 else 0 end, "
									+ kind.formatted() + " from " + TABLE + " where id = " + id ) ) {
						results.next();
						record( writeKey, results.getInt( 1 ) == 1, described + " stored=" + results.getString( 2 ) );
					}
				}
				catch (Throwable e) {
					record( writeKey, false, described + " !! " + brief( e ) );
				}

				final String findKey = kind.name + " find:" + route.name;
				try ( var statement = connection.prepareStatement(
						"select count(*) from " + TABLE + " where id = ? and " + kind.column + " = " + expression ) ) {
					statement.setInt( 1, nativeId( kind, i ) );
					route.binding.bind( statement, 2, kind, value );
					try ( var results = statement.executeQuery() ) {
						results.next();
						record( findKey, results.getInt( 1 ) == 1, described + " found=" + results.getInt( 1 ) );
					}
				}
				catch (Throwable e) {
					record( findKey, false, described + " !! " + brief( e ) );
				}
			}

			// a null
			final String nullKey = kind.name + " null:" + route.name;
			final int id = nextId++;
			try ( var statement = connection.prepareStatement(
					"insert into " + TABLE + " (id, " + kind.column + ") values (?, " + expression + ")" ) ) {
				statement.setInt( 1, id );
				statement.setNull( 2, route.name.startsWith( "setString" ) ? Types.VARCHAR : kind.sqlType );
				statement.executeUpdate();
				try ( var results = connection.createStatement().executeQuery(
						"select count(*) from " + TABLE + " where id = " + id + " and " + kind.column + " is null" ) ) {
					results.next();
					record( nullKey, results.getInt( 1 ) == 1, zone + " found=" + results.getInt( 1 ) );
				}
			}
			catch (Throwable e) {
				record( nullKey, false, zone + " !! " + brief( e ) );
			}
		}
	}

	private static int nativeId(Kind kind, int index) {
		return ( kind == DATES ? 100 : 200 ) + index;
	}

	private interface Work {
		void accept(Connection connection) throws Exception;
	}

	/// Run with a connection opened under the default time zone now in force.
	private void inConnection(Work work) throws Exception {
		final Exception[] failure = new Exception[1];
		try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "hibernate.hbm2ddl.auto", "none" )
				.build();
				var factory = (SessionFactoryImplementor) new MetadataSources( registry ).buildMetadata().buildSessionFactory() ) {
			factory.inSession( session -> session.doWork( connection -> {
				final boolean autoCommit = connection.getAutoCommit();
				connection.setAutoCommit( true );
				try {
					work.accept( connection );
				}
				catch (SQLException e) {
					throw e;
				}
				catch (Exception e) {
					failure[0] = e;
				}
				finally {
					connection.setAutoCommit( autoCommit );
				}
			} ) );
		}
		if ( failure[0] != null ) {
			throw failure[0];
		}
	}

	private static void execute(Connection connection, String sql) throws SQLException {
		try ( var statement = connection.createStatement() ) {
			statement.execute( sql );
		}
	}

	private static String brief(Throwable e) {
		Throwable root = e;
		while ( root.getCause() != null ) {
			root = root.getCause();
		}
		final String message = String.valueOf( root.getMessage() ).replace( '\n', ' ' );
		return root.getClass().getSimpleName() + ": " + ( message.length() > 150 ? message.substring( 0, 150 ) : message );
	}
}
