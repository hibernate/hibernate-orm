package org.hibernate.orm.test.type.temporal.preservation;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// TEMPORARY probes, not to be merged. Each test can be run on its own and
/// asserts nothing: it prints lines starting with `TP-PROBE|`, which the
/// build copies to its log.
///
/// - [#db2CurrentUtc()]: an expression for the current date and time at UTC
///   on Db2, and whether the server is in fact away from UTC.
/// - [#db2LocalDateTimeAsText()]: a `LocalDateTime` exchanged with Db2 as text.
/// - [#oracleInstantAsUtcTimestamp()]: an instant held on Oracle as its date
///   and time at UTC in a plain `timestamp`, which a primary key allows.
class ZzCarrierProbe {
	private static final String[] JVM_ZONES = { "UTC", "America/New_York", "Asia/Kathmandu" };
	private static final DateTimeFormatter SPACED =
			DateTimeFormatter.ofPattern( "uuuu-MM-dd HH:mm:ss.SSSSSSSSS", Locale.ROOT );
	private static final DateTimeFormatter DASHED =
			DateTimeFormatter.ofPattern( "uuuu-MM-dd-HH.mm.ss.SSSSSSSSS", Locale.ROOT );

	private static final List<LocalDateTime> DATE_TIMES = List.of(
			LocalDateTime.of( 1, 1, 1, 0, 0, 0 ),
			LocalDateTime.of( 1582, 10, 4, 12, 0, 0 ),
			LocalDateTime.of( 1582, 10, 10, 12, 0, 0 ),
			LocalDateTime.of( 1582, 10, 15, 0, 0, 0 ),
			LocalDateTime.of( 1883, 11, 18, 12, 0, 0 ),
			LocalDateTime.of( 1900, 1, 1, 0, 0, 0 ),
			LocalDateTime.of( 1970, 1, 1, 0, 0, 0 ),
			// skipped, then repeated, in America/New_York
			LocalDateTime.of( 2021, 3, 14, 2, 30, 0, 123_456_789 ),
			LocalDateTime.of( 2021, 11, 7, 1, 30, 0 ),
			LocalDateTime.of( 2021, 12, 31, 23, 59, 59, 999_999_999 ),
			LocalDateTime.of( 9999, 12, 31, 23, 59, 59, 999_999_000 )
	);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Db2: the current date and time at UTC

	@Test
	void db2CurrentUtc() {
		assumeTrue( DialectContext.getDialect() instanceof DB2Dialect );
		final String p = "db2-utc";
		inConnection( "UTC", connection -> {
			print( p, "server", "current timezone", query( connection, "values varchar(current timezone)" ) );
			print( p, "server", "current timezone nonzero",
					String.valueOf( !"0".equals( query( connection, "values varchar(int(current timezone))" ).trim() ) ) );
			print( p, "server", "current timestamp", query( connection, "values varchar(current timestamp)" ) );
			for ( String expression : new String[] {
					"current timestamp - current timezone",
					"current timestamp(12) - current timezone",
					"current timestamp"
			} ) {
				final String text = query( connection, "values varchar(" + expression + ")" );
				print( p, expression, "as text", text + " | jvm utc=" + LocalDateTime.now( ZoneOffset.UTC ) );
				try ( var statement = connection.createStatement();
						var resultSet = statement.executeQuery( "values " + expression ) ) {
					resultSet.next();
					print( p, expression, "type", resultSet.getMetaData().getColumnTypeName( 1 )
							+ "(" + resultSet.getMetaData().getPrecision( 1 ) + "," + resultSet.getMetaData().getScale( 1 ) + ")" );
					// the way an Instant is read on Db2
					final Timestamp timestamp = resultSet.getTimestamp( 1, Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ) );
					print( p, expression, "read as instant, skew seconds",
							String.valueOf( Duration.between( Instant.now(), timestamp.toInstant() ).toSeconds() ) );
				}
			}
			// stored in, and compared with, a column which holds the date and time at UTC
			execute( connection, "drop table tp_probe_utc", true );
			execute( connection, "create table tp_probe_utc (id int not null primary key, ts timestamp(12))", false );
			execute( connection, "insert into tp_probe_utc values (1, current timestamp - current timezone)", false );
			try ( var statement = connection.prepareStatement( "insert into tp_probe_utc values (2, ?)" ) ) {
				statement.setTimestamp( 1, Timestamp.from( Instant.now() ), Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ) );
				statement.executeUpdate();
			}
			print( p, "stored", "generated row", query( connection, "select varchar(ts) from tp_probe_utc where id = 1" ) );
			print( p, "stored", "row bound from the jvm", query( connection, "select varchar(ts) from tp_probe_utc where id = 2" ) );
			print( p, "stored", "seconds between the two",
					query( connection, "select varchar(timestampdiff(2, char(max(ts) - min(ts)))) from tp_probe_utc" ) );
			print( p, "compared", "rows within an hour of the expression", query( connection,
					"select varchar(count(*)) from tp_probe_utc where ts between (current timestamp - current timezone) - 1 hour"
							+ " and (current timestamp - current timezone) + 1 hour" ) );
			print( p, "compared", "rows within an hour of current timestamp", query( connection,
					"select varchar(count(*)) from tp_probe_utc where ts between current timestamp - 1 hour"
							+ " and current timestamp + 1 hour" ) );

			// one value for a statement, or for a transaction?
			connection.setAutoCommit( false );
			final String first = query( connection, "values varchar(current timestamp(12) - current timezone)" );
			sleep();
			final String second = query( connection, "values varchar(current timestamp(12) - current timezone)" );
			print( p, "stability", "same value later in the transaction", first.equals( second ) + " | " + first + " | " + second );
			print( p, "stability", "same value twice in a statement", query( connection,
					"values case when (current timestamp(12) - current timezone) = (current timestamp(12) - current timezone)"
							+ " then 'true' else 'false' end" ) );
			connection.rollback();
			connection.setAutoCommit( true );
			execute( connection, "drop table tp_probe_utc", true );
		} );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Db2: a LocalDateTime as text

	@Test
	void db2LocalDateTimeAsText() {
		assumeTrue( DialectContext.getDialect() instanceof DB2Dialect );
		final String p = "db2-text";
		inConnection( "UTC", connection -> {
			execute( connection, "drop table tp_probe_text", true );
			execute( connection, "create table tp_probe_text (id int not null primary key, route varchar(40),"
					+ " expected varchar(40), ts timestamp(12), ts6 timestamp(6))", false );
		} );
		int id = 0;
		for ( String writeZone : JVM_ZONES ) {
			final int firstId = id + 1;
			final int[] nextId = { id };
			inConnection( writeZone, connection -> {
				for ( LocalDateTime dateTime : DATE_TIMES ) {
					for ( String route : new String[] { "spaced", "dashed", "spaced-cast", "iso" } ) {
						final String text = switch ( route ) {
							case "dashed" -> DASHED.format( dateTime );
							case "iso" -> dateTime.toString();
							default -> SPACED.format( dateTime );
						};
						final String marker = route.equals( "spaced-cast" ) ? "cast(? as timestamp(12))" : "?";
						final String sql = "insert into tp_probe_text (id, route, expected, ts, ts6) values (?, ?, ?, "
								+ marker + ", " + marker + ")";
						final int rowId = ++nextId[0];
						try ( var statement = connection.prepareStatement( sql ) ) {
							statement.setInt( 1, rowId );
							statement.setString( 2, route );
							statement.setString( 3, SPACED.format( dateTime ) );
							statement.setString( 4, text );
							statement.setString( 5, text );
							statement.executeUpdate();
						}
						catch (SQLException e) {
							print( p, "write " + writeZone, route + " " + dateTime, failure( e ) );
						}
					}
				}
			} );
			id = nextId[0];
			final int lastId = id;
			for ( String readZone : JVM_ZONES ) {
				inConnection( readZone, connection -> {
					int exactText = 0;
					int exactServer = 0;
					int exactObject = 0;
					int rows = 0;
					try ( var statement = connection.prepareStatement(
							"select id, route, expected, ts, varchar_format(ts, 'YYYY-MM-DD HH24:MI:SS.FF9'), ts, ts6"
									+ " from tp_probe_text where id between ? and ? order by id" ) ) {
						statement.setInt( 1, firstId );
						statement.setInt( 2, lastId );
						try ( var resultSet = statement.executeQuery() ) {
							while ( resultSet.next() ) {
								rows++;
								final String expected = resultSet.getString( 3 );
								final String asText = resultSet.getString( 4 );
								final String asServerText = resultSet.getString( 5 );
								String asObject;
								try {
									asObject = SPACED.format( resultSet.getObject( 6, LocalDateTime.class ) );
								}
								catch (SQLException | RuntimeException e) {
									asObject = failure( e );
								}
								final boolean textOk = expected.equals( normalize( asText ) );
								final boolean serverOk = expected.equals( asServerText );
								final boolean objectOk = expected.equals( asObject );
								exactText += textOk ? 1 : 0;
								exactServer += serverOk ? 1 : 0;
								exactObject += objectOk ? 1 : 0;
								if ( !textOk || !serverOk ) {
									print( p, "read " + writeZone + ">" + readZone,
											resultSet.getString( 2 ) + " " + expected,
											"getString=" + asText + " | server=" + asServerText + " | getObject=" + asObject
													+ " | ts6=" + resultSet.getString( 7 ) );
								}
							}
						}
					}
					print( p, "read " + writeZone + ">" + readZone, "summary",
							"rows=" + rows + " getString exact=" + exactText + " server text exact=" + exactServer
									+ " getObject(LocalDateTime) exact=" + exactObject );
				} );
			}
		}

		inConnection( "America/New_York", connection -> {
			// the raw form of what getString returns, also under another default locale
			final Locale locale = Locale.getDefault();
			try {
				for ( Locale candidate : new Locale[] { Locale.ROOT, Locale.forLanguageTag( "th-TH-u-nu-thai" ), Locale.forLanguageTag( "ar-SA" ) } ) {
					Locale.setDefault( candidate );
					print( p, "format", "getString under locale " + candidate.toLanguageTag(), query( connection,
							"select ts from tp_probe_text where expected = '2021-03-14 02:30:00.123456789' fetch first 1 row only" ) );
				}
			}
			finally {
				Locale.setDefault( locale );
			}

			// rows written by SQL, independently of any binding
			for ( String literal : new String[] {
					"timestamp '2021-03-14 02:30:00.123456789012'",
					"timestamp '2021-03-14 24:00:00'",
					"timestamp '0001-01-01 00:00:00'",
					"timestamp '9999-12-31 24:00:00'"
			} ) {
				execute( connection, "delete from tp_probe_text where id = 9000", false );
				try {
					execute( connection, "insert into tp_probe_text (id, ts, ts6) values (9000, " + literal + ", " + literal + ")", false );
					print( p, "sql-written", literal, "getString=" + query( connection, "select ts from tp_probe_text where id = 9000" )
							+ " | server=" + query( connection, "select varchar(ts) from tp_probe_text where id = 9000" )
							+ " | getObject=" + object( connection, "select ts from tp_probe_text where id = 9000" ) );
				}
				catch (SQLException e) {
					print( p, "sql-written", literal, failure( e ) );
				}
			}

			// fractions beyond the precision of the column, also across midnight
			for ( String text : new String[] { "2021-03-14 23:59:59.9999995", "2021-03-14 23:59:59.999999999", "2021-03-14 10:00:00.1234565" } ) {
				execute( connection, "delete from tp_probe_text where id = 9001", false );
				try ( var statement = connection.prepareStatement( "insert into tp_probe_text (id, ts6) values (9001, ?)" ) ) {
					statement.setString( 1, text );
					statement.executeUpdate();
					print( p, "precision", text + " into timestamp(6)", query( connection, "select varchar(ts6) from tp_probe_text where id = 9001" ) );
				}
				catch (SQLException e) {
					print( p, "precision", text + " into timestamp(6)", failure( e ) );
				}
			}

			// nulls
			for ( String route : new String[] { "setString(null)", "setNull(VARCHAR)", "setNull(TIMESTAMP)", "setObject(null)" } ) {
				execute( connection, "delete from tp_probe_text where id = 9002", false );
				try ( var statement = connection.prepareStatement( "insert into tp_probe_text (id, ts) values (9002, ?)" ) ) {
					switch ( route ) {
						case "setString(null)" -> statement.setString( 1, null );
						case "setNull(VARCHAR)" -> statement.setNull( 1, Types.VARCHAR );
						case "setNull(TIMESTAMP)" -> statement.setNull( 1, Types.TIMESTAMP );
						default -> statement.setObject( 1, null );
					}
					statement.executeUpdate();
					print( p, "null", route, "stored null=" + query( connection,
							"select case when ts is null then 'true' else 'false' end from tp_probe_text where id = 9002" ) );
				}
				catch (SQLException e) {
					print( p, "null", route, failure( e ) );
				}
			}

			// a text parameter in the places a query puts one
			final String sample = "2021-03-14 02:30:00.123456789";
			for ( String sql : new String[] {
					"select count(*) from tp_probe_text where ts = ?",
					"select count(*) from tp_probe_text where ts = cast(? as timestamp(12))",
					"select count(*) from tp_probe_text where ts between ? and ?",
					"select count(*) from tp_probe_text where ts in (?, ?)",
					"select count(*) from tp_probe_text where ts < ? + 1 day",
					"select count(*) from tp_probe_text where ts < cast(? as timestamp(12)) + 1 day",
					"select count(*) from tp_probe_text where year(ts) = year(?)",
					"select count(*) from tp_probe_text where year(ts) = year(cast(? as timestamp(12)))",
					"select count(*) from tp_probe_text where coalesce(ts, ?) = ?",
					"select count(*) from tp_probe_text where ? is null",
					"select count(*) from tp_probe_text where cast(? as timestamp(12)) is null",
					"values ?",
					"values cast(? as timestamp(12))",
					"select ? from sysibm.sysdummy1",
					"update tp_probe_text set ts = ? where id = -1",
					"select count(*) from tp_probe_text where (id, ts) = (1, ?)"
			} ) {
				try ( var statement = connection.prepareStatement( sql ) ) {
					final int parameters = statement.getParameterMetaData().getParameterCount();
					final StringBuilder types = new StringBuilder();
					for ( int i = 1; i <= parameters; i++ ) {
						statement.setString( i, sample );
						try {
							types.append( statement.getParameterMetaData().getParameterTypeName( i ) ).append( ' ' );
						}
						catch (SQLException e) {
							types.append( "? " );
						}
					}
					String result;
					if ( sql.startsWith( "update" ) ) {
						result = "updated " + statement.executeUpdate();
					}
					else {
						try ( var resultSet = statement.executeQuery() ) {
							resultSet.next();
							result = resultSet.getString( 1 ) + " (" + resultSet.getMetaData().getColumnTypeName( 1 ) + ")";
						}
					}
					print( p, "parameter", sql, result + " | parameter types: " + types.toString().trim() );
				}
				catch (SQLException e) {
					print( p, "parameter", sql, failure( e ) );
				}
			}

			// passed to a stored procedure
			execute( connection, "drop procedure tp_probe_store", true );
			try {
				execute( connection, "create procedure tp_probe_store(in p_ts timestamp(12)) begin"
						+ " insert into tp_probe_text (id, ts) values (9003, p_ts); end", false );
				execute( connection, "delete from tp_probe_text where id = 9003", false );
				try ( var call = connection.prepareCall( "{call tp_probe_store(?)}" ) ) {
					call.setString( 1, sample );
					call.execute();
				}
				print( p, "callable", "setString", query( connection, "select varchar(ts) from tp_probe_text where id = 9003" ) );
			}
			catch (SQLException e) {
				print( p, "callable", "setString", failure( e ) );
			}
			execute( connection, "drop procedure tp_probe_store", true );
			execute( connection, "drop table tp_probe_text", true );
		} );
	}

	/// The second round for a `LocalDateTime` as text on Db2. The first showed that
	/// `setString` is exact only while the driver does not know the parameter to be a
	/// timestamp, and that `getString` on the column is not exact. Here the parameter
	/// is wrapped so that the server types it as text, the statement is described
	/// before anything is bound, and the column is read as text made by the server.
	@Test
	void db2LocalDateTimeAsTextRound2() {
		assumeTrue( DialectContext.getDialect() instanceof DB2Dialect );
		final String p = "db2-text2";
		final String[] wrappers = {
				"cast(cast(? as varchar(40)) as timestamp(12))",
				"timestamp(cast(? as varchar(40)), 12)",
				"timestamp_format(cast(? as varchar(40)), 'YYYY-MM-DD HH24:MI:SS.FF9', 9)"
		};
		final String[] readers = {
				"varchar_format(ts, 'YYYY-MM-DD HH24:MI:SS.FF9')",
				"varchar_format(ts, 'YYYY-MM-DD HH24:MI:SS.FF12')",
				"varchar(ts)",
				"cast(ts as varchar(40))"
		};
		inConnection( "UTC", connection -> {
			execute( connection, "drop table tp_probe_text2", true );
			execute( connection, "create table tp_probe_text2 (id int not null primary key, route varchar(80),"
					+ " expected varchar(40), ts timestamp(12))", false );
		} );
		int id = 0;
		for ( String writeZone : JVM_ZONES ) {
			final int firstId = id + 1;
			final int[] nextId = { id };
			inConnection( writeZone, connection -> {
				for ( String wrapper : wrappers ) {
					// one statement for every value, described before the first is bound
					try ( var statement = connection.prepareStatement(
							"insert into tp_probe_text2 (id, route, expected, ts) values (?, ?, ?, " + wrapper + ")" ) ) {
						print( p, "write " + writeZone, wrapper + " parameter type",
								statement.getParameterMetaData().getParameterTypeName( 4 ) );
						for ( LocalDateTime dateTime : DATE_TIMES ) {
							final int rowId = ++nextId[0];
							try {
								statement.setInt( 1, rowId );
								statement.setString( 2, wrapper );
								statement.setString( 3, SPACED.format( dateTime ) );
								statement.setString( 4, SPACED.format( dateTime ) );
								statement.executeUpdate();
							}
							catch (SQLException e) {
								print( p, "write " + writeZone, wrapper + " " + dateTime, failure( e ) );
							}
						}
						// as a batch, and as a null
						try {
							for ( int i = 0; i < 2; i++ ) {
								statement.setInt( 1, ++nextId[0] );
								statement.setString( 2, wrapper + " batch" );
								statement.setString( 3, "2021-03-14 02:30:00.123456789" );
								statement.setString( 4, "2021-03-14 02:30:00.123456789" );
								statement.addBatch();
							}
							statement.executeBatch();
						}
						catch (SQLException e) {
							print( p, "write " + writeZone, wrapper + " batch", failure( e ) );
						}
						for ( String nullRoute : new String[] { "setNull(VARCHAR)", "setString(null)" } ) {
							try {
								final int rowId = ++nextId[0];
								statement.setInt( 1, rowId );
								statement.setString( 2, wrapper + " " + nullRoute );
								statement.setString( 3, null );
								if ( nullRoute.startsWith( "setNull" ) ) {
									statement.setNull( 4, Types.VARCHAR );
								}
								else {
									statement.setString( 4, null );
								}
								statement.executeUpdate();
							}
							catch (SQLException e) {
								print( p, "write " + writeZone, wrapper + " " + nullRoute, failure( e ) );
							}
						}
					}
					catch (SQLException e) {
						print( p, "write " + writeZone, wrapper, failure( e ) );
					}
				}
				// the routes of the driver itself: a LocalDateTime passed as such, which JDBC 4.2
				// provides for, and the DBTimestamp of the driver, made from text
				for ( String route : new String[] {
						"driver setObject(LocalDateTime)", "driver setObject(LocalDateTime, TIMESTAMP)",
						"driver setDBTimestamp(spaced)", "driver setDBTimestamp(dashed)"
				} ) {
					try ( var statement = connection.prepareStatement(
							"insert into tp_probe_text2 (id, route, expected, ts) values (?, ?, ?, ?)" ) ) {
						statement.getParameterMetaData().getParameterTypeName( 4 );
						for ( LocalDateTime dateTime : DATE_TIMES ) {
							final int rowId = ++nextId[0];
							try {
								statement.setInt( 1, rowId );
								statement.setString( 2, route );
								statement.setString( 3, SPACED.format( dateTime ) );
								switch ( route ) {
									case "driver setObject(LocalDateTime)" -> statement.setObject( 4, dateTime );
									case "driver setObject(LocalDateTime, TIMESTAMP)" -> statement.setObject( 4, dateTime, Types.TIMESTAMP );
									default -> {
										final Class<?> timestampClass = Class.forName( "com.ibm.db2.jcc.DBTimestamp" );
										final Object timestamp = timestampClass.getMethod( "valueOfDBString", String.class ).invoke(
												null,
												( route.endsWith( "(spaced)" ) ? SPACED : DASHED ).format( dateTime )
										);
										final Class<?> statementClass = Class.forName( "com.ibm.db2.jcc.DB2PreparedStatement" );
										statementClass.getMethod( "setDBTimestamp", int.class, timestampClass )
												.invoke( statement.unwrap( statementClass ), 4, timestamp );
									}
								}
								statement.executeUpdate();
							}
							catch (SQLException | ReflectiveOperationException | RuntimeException e) {
								print( p, "write " + writeZone, route + " " + dateTime,
										failure( e.getCause() != null && !( e instanceof SQLException ) ? e.getCause() : e ) );
							}
						}
					}
					catch (SQLException e) {
						print( p, "write " + writeZone, route, failure( e ) );
					}
				}
			} );
			id = nextId[0];
			final int lastId = id;
			for ( String readZone : JVM_ZONES ) {
				inConnection( readZone, connection -> {
					// the routes of the driver itself
					for ( String reader : new String[] {
							"driver getObject(LocalDateTime)", "driver getDBTimestamp().toDBString(true)",
							"driver getDBTimestamp().toDBString(false)"
					} ) {
						int rows = 0;
						int exact = 0;
						try ( var statement = connection.prepareStatement(
								"select id, route, expected, ts from tp_probe_text2 where id between ? and ? and expected is not null order by id" ) ) {
							statement.setInt( 1, firstId );
							statement.setInt( 2, lastId );
							try ( var resultSet = statement.executeQuery() ) {
								while ( resultSet.next() ) {
									rows++;
									final String expected = resultSet.getString( 3 );
									String text;
									try {
										if ( reader.startsWith( "driver getObject" ) ) {
											text = SPACED.format( resultSet.getObject( 4, LocalDateTime.class ) );
										}
										else {
											final Class<?> resultSetClass = Class.forName( "com.ibm.db2.jcc.DB2ResultSet" );
											final Object timestamp = resultSetClass.getMethod( "getDBTimestamp", int.class )
													.invoke( resultSet.unwrap( resultSetClass ), 4 );
											text = (String) timestamp.getClass().getMethod( "toDBString", boolean.class )
													.invoke( timestamp, reader.endsWith( "(true)" ) );
										}
									}
									catch (SQLException | ReflectiveOperationException | RuntimeException e) {
										text = failure( e.getCause() != null && !( e instanceof SQLException ) ? e.getCause() : e );
									}
									if ( expected.equals( text ) || expected.equals( normalize( text ) ) ) {
										exact++;
									}
									else if ( expected.startsWith( "2021-03-14 02:30" ) || expected.startsWith( "1582-10-10" ) || rows <= 2 ) {
										print( p, "read " + writeZone + ">" + readZone,
												reader + " " + resultSet.getString( 2 ) + " " + expected, String.valueOf( text ) );
									}
								}
							}
						}
						catch (SQLException e) {
							print( p, "read " + writeZone + ">" + readZone, reader, failure( e ) );
						}
						print( p, "read " + writeZone + ">" + readZone, reader + " summary", "rows=" + rows + " exact=" + exact );
					}
					for ( String reader : readers ) {
						int rows = 0;
						int exact = 0;
						int nulls = 0;
						String type = "?";
						try ( var statement = connection.prepareStatement(
								"select id, route, expected, " + reader + " from tp_probe_text2 where id between ? and ? order by id" ) ) {
							statement.setInt( 1, firstId );
							statement.setInt( 2, lastId );
							try ( var resultSet = statement.executeQuery() ) {
								type = resultSet.getMetaData().getColumnTypeName( 4 );
								while ( resultSet.next() ) {
									rows++;
									final String expected = resultSet.getString( 3 );
									final String text = resultSet.getString( 4 );
									if ( expected == null ) {
										nulls += text == null ? 1 : 0;
										if ( text != null ) {
											print( p, "read " + writeZone + ">" + readZone, reader + " of a null", text );
										}
									}
									else if ( expected.equals( normalize( text ) ) ) {
										exact++;
									}
									else {
										print( p, "read " + writeZone + ">" + readZone,
												reader + " " + resultSet.getString( 2 ) + " " + expected, String.valueOf( text ) );
									}
								}
							}
						}
						catch (SQLException e) {
							print( p, "read " + writeZone + ">" + readZone, reader, failure( e ) );
						}
						print( p, "read " + writeZone + ">" + readZone, reader + " summary",
								"type=" + type + " rows=" + rows + " exact=" + exact + " nulls read as null=" + nulls );
					}
				} );
			}
		}

		inConnection( "America/New_York", connection -> {
			// the wrapped parameter in the places a query puts one, described before it is bound
			final String sample = "2021-03-14 02:30:00.123456789";
			final String expectedRows = query( connection,
					"select varchar(count(*)) from tp_probe_text2 where ts = timestamp '2021-03-14 02:30:00.123456789'" );
			print( p, "parameter", "rows which hold the sample, by a literal", expectedRows );
			for ( String wrapper : wrappers ) {
				for ( String template : new String[] {
						"select count(*) from tp_probe_text2 where ts = #",
						"select count(*) from tp_probe_text2 where ts between # and #",
						"select count(*) from tp_probe_text2 where ts in (#, #)",
						"select count(*) from tp_probe_text2 where ts >= # and ts < # + 1 second",
						"select count(*) from tp_probe_text2 where year(ts) = year(#) and ts = #",
						"select count(*) from tp_probe_text2 where coalesce(ts, #) = #",
						"select count(*) from tp_probe_text2 where (id, ts) in (select id, ts from tp_probe_text2 where ts = #)",
						"select count(*) from tp_probe_text2 where ts = (select max(#) from sysibm.sysdummy1)",
						"select count(*) from tp_probe_text2 where ts = case when 1 = 1 then # else # end",
						"values varchar_format(#, 'YYYY-MM-DD HH24:MI:SS.FF9')",
						"update tp_probe_text2 set ts = # where id = -1"
				} ) {
					final String sql = template.replace( "#", wrapper );
					try ( var statement = connection.prepareStatement( sql ) ) {
						final int parameters = statement.getParameterMetaData().getParameterCount();
						final StringBuilder types = new StringBuilder();
						for ( int i = 1; i <= parameters; i++ ) {
							types.append( statement.getParameterMetaData().getParameterTypeName( i ) ).append( ' ' );
							statement.setString( i, sample );
						}
						String result;
						if ( sql.startsWith( "update" ) ) {
							result = "updated " + statement.executeUpdate();
						}
						else {
							try ( var resultSet = statement.executeQuery() ) {
								resultSet.next();
								result = resultSet.getString( 1 );
							}
						}
						print( p, "parameter", sql, result + " | expected " + expectedRows + " | parameter types: " + types.toString().trim() );
					}
					catch (SQLException e) {
						print( p, "parameter", sql, failure( e ) );
					}
				}
				// a null parameter
				for ( String sql : new String[] {
						"select count(*) from tp_probe_text2 where " + wrapper + " is null",
						"select count(*) from tp_probe_text2 where ts = " + wrapper
				} ) {
					try ( var statement = connection.prepareStatement( sql ) ) {
						statement.getParameterMetaData().getParameterTypeName( 1 );
						statement.setNull( 1, Types.VARCHAR );
						try ( var resultSet = statement.executeQuery() ) {
							resultSet.next();
							print( p, "null parameter", sql, resultSet.getString( 1 ) );
						}
					}
					catch (SQLException e) {
						print( p, "null parameter", sql, failure( e ) );
					}
				}
			}

			// what the server makes of values which Java has no LocalDateTime for
			for ( String literal : new String[] {
					"timestamp '2021-03-14 24:00:00'",
					"timestamp '9999-12-31 24:00:00'",
					"timestamp '2021-03-14 02:30:00.123456789012'"
			} ) {
				execute( connection, "delete from tp_probe_text2 where id = 9000", false );
				try {
					execute( connection, "insert into tp_probe_text2 (id, ts) values (9000, " + literal + ")", false );
					final StringBuilder texts = new StringBuilder();
					for ( String reader : readers ) {
						texts.append( reader ).append( '=' )
								.append( query( connection, "select " + reader + " from tp_probe_text2 where id = 9000" ) ).append( " | " );
					}
					print( p, "sql-written", literal, texts.toString() );
				}
				catch (SQLException e) {
					print( p, "sql-written", literal, failure( e ) );
				}
			}
			// text which is not a date and time
			for ( String text : new String[] { "2021-03-14 24:00:00.000000000", "2021-02-30 10:00:00.000000000", "not a timestamp", "" } ) {
				try ( var statement = connection.prepareStatement( "values " + wrappers[0] ) ) {
					statement.getParameterMetaData().getParameterTypeName( 1 );
					statement.setString( 1, text );
					try ( var resultSet = statement.executeQuery() ) {
						resultSet.next();
						print( p, "invalid text", "'" + text + "'", "accepted as " + resultSet.getString( 1 ) );
					}
				}
				catch (SQLException e) {
					print( p, "invalid text", "'" + text + "'", failure( e ) );
				}
			}

			// passed to a stored procedure
			execute( connection, "drop procedure tp_probe_store2", true );
			try {
				execute( connection, "create procedure tp_probe_store2(in p_ts timestamp(12)) begin"
						+ " insert into tp_probe_text2 (id, ts) values (9003, p_ts); end", false );
				for ( String call : new String[] { "{call tp_probe_store2(" + wrappers[0] + ")}", "{call tp_probe_store2(?)}" } ) {
					execute( connection, "delete from tp_probe_text2 where id = 9003", false );
					try ( var statement = connection.prepareCall( call ) ) {
						String type;
						try {
							type = statement.getParameterMetaData().getParameterTypeName( 1 );
						}
						catch (SQLException e) {
							type = failure( e );
						}
						statement.setString( 1, sample );
						statement.execute();
						print( p, "callable", call, "parameter type=" + type + " | stored="
								+ query( connection, "select " + readers[0] + " from tp_probe_text2 where id = 9003" ) );
					}
					catch (SQLException e) {
						print( p, "callable", call, failure( e ) );
					}
				}
			}
			catch (SQLException e) {
				print( p, "callable", "create procedure", failure( e ) );
			}
			execute( connection, "drop procedure tp_probe_store2", true );
			execute( connection, "drop table tp_probe_text2", true );
		} );
	}

	/// What `getString` returns for a timestamp, in the form the samples are written in.
	private static String normalize(String text) {
		if ( text == null ) {
			return null;
		}
		String normalized = text.trim();
		// 2021-03-14-02.30.00.123456789012 as well as 2021-03-14 02:30:00.123456789012
		if ( normalized.length() > 19 && normalized.charAt( 10 ) == '-' ) {
			normalized = normalized.substring( 0, 10 ) + " " + normalized.substring( 11, 19 ).replace( '.', ':' )
					+ normalized.substring( 19 );
		}
		final int point = normalized.indexOf( '.' );
		final String fraction = point < 0 ? "" : normalized.substring( point + 1 );
		final String seconds = point < 0 ? normalized : normalized.substring( 0, point );
		return seconds + "." + ( fraction + "000000000" ).substring( 0, 9 );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Oracle: an instant as its date and time at UTC in a plain timestamp

	@Test
	void oracleInstantAsUtcTimestamp() {
		assumeTrue( DialectContext.getDialect() instanceof OracleDialect );
		final String p = "ora-utc";
		inConnection( "UTC", connection -> {
			print( p, "server", "version", connection.getMetaData().getDatabaseProductVersion().replace( '\n', ' ' )
					+ " | driver " + connection.getMetaData().getDriverVersion() );
			// what a primary key allows
			for ( String ddl : new String[] {
					"create table tp_probe_pk1 (ts timestamp(9) with time zone primary key)",
					"create table tp_probe_pk2 (ts timestamp(9) with local time zone primary key)",
					"create table tp_probe_pk3 (id number(10), ts timestamp(9), primary key (id, ts))",
					"create table tp_probe_fk (id number(10) primary key, ref_id number(10), ref_ts timestamp(9),"
							+ " foreign key (ref_id, ref_ts) references tp_probe_pk3 (id, ts))",
					"create table tp_probe_uq (ts timestamp(9) with time zone, unique (ts))"
			} ) {
				final String table = ddl.substring( "create table ".length(), ddl.indexOf( ' ', "create table ".length() ) );
				execute( connection, "drop table " + table + " cascade constraints", true );
				try {
					execute( connection, ddl, false );
					print( p, "ddl", ddl, "accepted" );
				}
				catch (SQLException e) {
					print( p, "ddl", ddl, failure( e ) );
				}
			}
			execute( connection, "drop table tp_probe_utc cascade constraints", true );
			execute( connection, "create table tp_probe_utc (id number(10), zones varchar2(80), expected varchar2(40),"
					+ " ts timestamp(9), primary key (id, ts))", false );
		} );

		final String[] sessionZones = { null, "Asia/Kathmandu", "America/New_York" };
		int id = 0;
		for ( String writeZone : JVM_ZONES ) {
			for ( String sessionZone : sessionZones ) {
				final int firstId = id + 1;
				final int[] nextId = { id };
				inConnection( writeZone, connection -> {
					if ( sessionZone != null ) {
						execute( connection, "alter session set time_zone = '" + sessionZone + "'", false );
					}
					for ( LocalDateTime atUtc : DATE_TIMES ) {
						for ( String route : new String[] { "setObject", "setObject+TIMESTAMP" } ) {
							final int rowId = ++nextId[0];
							try ( var statement = connection.prepareStatement(
									"insert into tp_probe_utc (id, zones, expected, ts) values (?, ?, ?, ?)" ) ) {
								statement.setInt( 1, rowId );
								statement.setString( 2, route );
								statement.setString( 3, SPACED.format( atUtc ) );
								if ( route.equals( "setObject" ) ) {
									statement.setObject( 4, atUtc );
								}
								else {
									statement.setObject( 4, atUtc, Types.TIMESTAMP );
								}
								statement.executeUpdate();
							}
							catch (SQLException e) {
								print( p, "write " + writeZone + "/" + sessionZone, route + " " + atUtc, failure( e ) );
							}
						}
					}
				} );
				id = nextId[0];
				final int lastId = id;
				for ( String readZone : JVM_ZONES ) {
					for ( String readSessionZone : sessionZones ) {
						inConnection( readZone, connection -> {
							if ( readSessionZone != null ) {
								execute( connection, "alter session set time_zone = '" + readSessionZone + "'", false );
							}
							int rows = 0;
							int stored = 0;
							int read = 0;
							int found = 0;
							try ( var statement = connection.prepareStatement(
									"select id, zones, expected, to_char(ts, 'SYYYY-MM-DD HH24:MI:SS.FF9'), ts"
											+ " from tp_probe_utc where id between ? and ? order by id" ) ) {
								statement.setInt( 1, firstId );
								statement.setInt( 2, lastId );
								try ( var resultSet = statement.executeQuery() ) {
									while ( resultSet.next() ) {
										rows++;
										final String expected = resultSet.getString( 3 );
										final String asServerText = resultSet.getString( 4 ).trim();
										String asObject;
										LocalDateTime value = null;
										try {
											value = resultSet.getObject( 5, LocalDateTime.class );
											asObject = SPACED.format( value );
										}
										catch (SQLException | RuntimeException e) {
											asObject = failure( e );
										}
										final boolean storedOk = expected.equals( asServerText );
										final boolean readOk = expected.equals( asObject );
										stored += storedOk ? 1 : 0;
										read += readOk ? 1 : 0;
										// found again by the value which was read
										boolean foundOk = false;
										if ( value != null ) {
											try ( var lookup = connection.prepareStatement(
													"select count(*) from tp_probe_utc where id = ? and ts = ?" ) ) {
												lookup.setInt( 1, resultSet.getInt( 1 ) );
												lookup.setObject( 2, value );
												try ( var count = lookup.executeQuery() ) {
													count.next();
													foundOk = count.getInt( 1 ) == 1;
												}
											}
										}
										found += foundOk ? 1 : 0;
										if ( !storedOk || !readOk || !foundOk ) {
											print( p, "read " + writeZone + "/" + sessionZone + ">" + readZone + "/" + readSessionZone,
													resultSet.getString( 2 ) + " " + expected,
													"server=" + asServerText + " | getObject=" + asObject + " | found=" + foundOk );
										}
									}
								}
							}
							print( p, "read " + writeZone + "/" + sessionZone + ">" + readZone + "/" + readSessionZone, "summary",
									"rows=" + rows + " stored exact=" + stored + " read exact=" + read + " found by parameter=" + found );
						} );
					}
				}
			}
		}

		// the current date and time at UTC
		for ( String jvmZone : new String[] { "UTC", "Asia/Kathmandu" } ) {
			for ( String sessionZone : sessionZones ) {
				inConnection( jvmZone, connection -> {
					if ( sessionZone != null ) {
						execute( connection, "alter session set time_zone = '" + sessionZone + "'", false );
					}
					final String context = "current " + jvmZone + "/" + sessionZone;
					for ( String expression : new String[] {
							"sys_extract_utc(systimestamp)",
							"sys_extract_utc(current_timestamp)",
							"cast(systimestamp at time zone 'UTC' as timestamp(9))",
							"localtimestamp",
							"systimestamp"
					} ) {
						try ( var statement = connection.createStatement();
								var resultSet = statement.executeQuery( "select " + expression + " from dual" ) ) {
							resultSet.next();
							final String type = resultSet.getMetaData().getColumnTypeName( 1 )
									+ "(" + resultSet.getMetaData().getScale( 1 ) + ")";
							String skew;
							try {
								skew = String.valueOf( Duration.between(
										LocalDateTime.now( ZoneOffset.UTC ),
										resultSet.getObject( 1, LocalDateTime.class )
								).toSeconds() );
							}
							catch (SQLException | RuntimeException e) {
								skew = failure( e );
							}
							print( p, context, expression, "type=" + type + " | read as LocalDateTime, skew seconds from utc=" + skew );
						}
						catch (SQLException e) {
							print( p, context, expression, failure( e ) );
						}
					}
					// generated into, and compared with, the column
					execute( connection, "delete from tp_probe_utc where id in (-1, -2)", false );
					execute( connection, "insert into tp_probe_utc (id, ts) values (-1, sys_extract_utc(systimestamp))", false );
					try ( var statement = connection.prepareStatement( "insert into tp_probe_utc (id, ts) values (-2, ?)" ) ) {
						statement.setObject( 1, LocalDateTime.now( ZoneOffset.UTC ) );
						statement.executeUpdate();
					}
					print( p, context, "generated row and row bound from the jvm, seconds apart", query( connection,
							"select to_char(abs(extract(second from (max(ts) - min(ts))) + 60 * extract(minute from (max(ts) - min(ts)))"
									+ " + 3600 * extract(hour from (max(ts) - min(ts))) + 86400 * extract(day from (max(ts) - min(ts)))))"
									+ " from tp_probe_utc where id in (-1, -2)" ) );
					print( p, context, "rows within an hour of sys_extract_utc(systimestamp)", query( connection,
							"select to_char(count(*)) from tp_probe_utc where id in (-1, -2)"
									+ " and ts between sys_extract_utc(systimestamp) - interval '1' hour"
									+ " and sys_extract_utc(systimestamp) + interval '1' hour" ) );
					print( p, context, "rows within an hour of systimestamp", query( connection,
							"select to_char(count(*)) from tp_probe_utc where id in (-1, -2)"
									+ " and ts between systimestamp - interval '1' hour and systimestamp + interval '1' hour" ) );
					execute( connection, "delete from tp_probe_utc where id in (-1, -2)", false );
				} );
			}
		}

		inConnection( "Asia/Kathmandu", connection -> {
			// for the record: a timestamp with local time zone, which a primary key may allow
			try {
				execute( connection, "delete from tp_probe_pk2", false );
				try ( var statement = connection.prepareStatement( "insert into tp_probe_pk2 (ts) values (?)" ) ) {
					statement.setObject( 1, OffsetDateTime.of( 2021, 3, 14, 11, 0, 0, 0, ZoneOffset.ofHours( 2 ) ) );
					statement.executeUpdate();
				}
				print( p, "local time zone", "written as OffsetDateTime 2021-03-14T11:00+02:00",
						"server at utc=" + query( connection, "select to_char(sys_extract_utc(ts), 'YYYY-MM-DD HH24:MI:SS') from tp_probe_pk2" )
								+ " | getObject(OffsetDateTime)=" + object( connection, "select ts from tp_probe_pk2", OffsetDateTime.class ) );
			}
			catch (SQLException e) {
				print( p, "local time zone", "written as OffsetDateTime", failure( e ) );
			}
			for ( String table : new String[] { "tp_probe_fk", "tp_probe_pk1", "tp_probe_pk2", "tp_probe_pk3", "tp_probe_uq", "tp_probe_utc" } ) {
				execute( connection, "drop table " + table + " cascade constraints", true );
			}
		} );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@FunctionalInterface
	private interface ConnectionWork {
		void run(Connection connection) throws SQLException;
	}

	/// Run with a connection opened while the given zone is the default
	/// time zone of the JVM, from a factory built for it alone.
	private static void inConnection(String jvmZone, ConnectionWork work) {
		final TimeZone original = TimeZone.getDefault();
		TimeZone.setDefault( TimeZone.getTimeZone( jvmZone ) );
		try {
			final var builder = ServiceRegistryUtil.serviceRegistryBuilder();
			builder.applySetting( "hibernate.hbm2ddl.auto", "none" );
			try ( var registry = builder.build();
					var factory = (SessionFactoryImplementor) new MetadataSources( registry ).buildMetadata().buildSessionFactory() ) {
				factory.inSession( session -> session.doWork( connection -> {
					try {
						work.run( connection );
					}
					catch (SQLException | RuntimeException e) {
						System.out.println( "TP-PROBE|aborted|jvm " + jvmZone + "|" + failure( e ) );
					}
				} ) );
			}
		}
		finally {
			TimeZone.setDefault( original );
		}
	}

	private static void print(String probe, String context, String item, String result) {
		System.out.println( "TP-PROBE|" + probe + "|" + context + "|" + item + "|" + result );
	}

	private static String failure(Throwable e) {
		final String message = String.valueOf( e.getMessage() ).replace( '\n', ' ' );
		return "!! " + e.getClass().getSimpleName() + ": " + message.substring( 0, Math.min( 160, message.length() ) );
	}

	private static String query(Connection connection, String sql) throws SQLException {
		try ( Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery( sql ) ) {
			return resultSet.next() ? resultSet.getString( 1 ) : "<no row>";
		}
	}

	private static String object(Connection connection, String sql) {
		return object( connection, sql, LocalDateTime.class );
	}

	private static String object(Connection connection, String sql, Class<?> type) {
		try ( Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery( sql ) ) {
			return resultSet.next() ? String.valueOf( resultSet.getObject( 1, type ) ) : "<no row>";
		}
		catch (SQLException | RuntimeException e) {
			return failure( e );
		}
	}

	private static void execute(Connection connection, String sql, boolean ignoreFailure) throws SQLException {
		try ( Statement statement = connection.createStatement() ) {
			statement.execute( sql );
		}
		catch (SQLException e) {
			if ( !ignoreFailure ) {
				throw e;
			}
		}
	}

	private static void sleep() {
		try {
			Thread.sleep( 1200 );
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@SuppressWarnings("unused")
	private static Dialect dialect() {
		return DialectContext.getDialect();
	}
}
