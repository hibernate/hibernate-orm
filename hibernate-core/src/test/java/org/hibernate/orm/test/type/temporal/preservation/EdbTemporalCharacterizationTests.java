package org.hibernate.orm.test.type.temporal.preservation;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Live evidence for the finite-date gate. Candidate endpoints are observations,
/// not assertions of support: each driver/mode/type outcome is retained for review.
/// Run this class against EDB separately with each configured JDBC driver. The
/// ordinary preservation matrix continues to enforce its unchanged expectations.
/// All compatibility changes and diagnostic tables are transaction-local.
///
/// @author Steve Ebersole
@RequiresDialect(PostgresPlusDialect.class)
class EdbTemporalCharacterizationTests {
	@Test
	void timestampExpressionsAndTypedDateCandidates() throws Exception {
		var settings = new HashMap<String, Object>();
		Environment.getProperties().forEach( (key, value) -> settings.put( key.toString(), value ) );
		var provider = new GuardedConnectionProvider( false );
		provider.configure( settings );
		try ( var connection = provider.getConnection() ) {
			connection.setAutoCommit( false );
			var metadata = connection.getMetaData();
			System.out.println( "EDB_CHARACTERIZATION database=" + metadata.getDatabaseProductVersion()
					+ " driver=" + metadata.getDriverName() + " " + metadata.getDriverVersion() );
			for ( int transaction = 0; transaction < 2; transaction++ ) {
				for ( int statement = 0; statement < 2; statement++ ) {
					try ( var query = connection.createStatement(); var result = query.executeQuery(
							"select current_timestamp, current_timestamp, now(), transaction_timestamp(), localtimestamp" ) ) {
						assertThat( result.next() ).isTrue();
						System.out.println( "EDB_CLOCK transaction=" + transaction + " statement=" + statement
								+ " current=" + result.getString( 1 ) + " duplicate=" + result.getString( 2 )
								+ " now=" + result.getString( 3 ) + " transaction_timestamp=" + result.getString( 4 )
								+ " localtimestamp=" + result.getString( 5 ) );
					}
					Thread.sleep( 20 ); // Characterization only, never a history-continuity assertion.
				}
				connection.rollback();
			}
			for ( String mode : List.of( "true", "false" ) ) {
				try {
					try ( var statement = connection.createStatement() ) {
						statement.execute( "set local edb_redwood_date = " + mode );
						try ( var result = statement.executeQuery( "select current_setting('edb_redwood_date'), current_setting('TimeZone')" ) ) {
							assertThat( result.next() ).isTrue();
							System.out.println( "EDB_DATE_MODE requested=" + mode + " actual=" + result.getString( 1 ) + " zone=" + result.getString( 2 ) );
						}
					}
					for ( String type : List.of( "date", "pg_catalog.date" ) ) probeDates( connection, mode, type );
				}
				catch (SQLException failure) {
					System.out.println( "EDB_DATE_MODE unavailable=" + mode + " state=" + failure.getSQLState() + " message=" + failure.getMessage() );
				}
				finally { connection.rollback(); }
			}
		}
	}

	private static void probeDates(Connection connection, String mode, String type) throws SQLException {
		try ( var statement = connection.createStatement() ) {
			statement.execute( "create temporary table edb_temporal_date_probe (value " + type + ") on commit drop" );
			try ( var result = statement.executeQuery( "select value from edb_temporal_date_probe where 1=0" ) ) {
				System.out.println( "EDB_DATE_TYPE mode=" + mode + " declaration=" + type
						+ " physical=" + result.getMetaData().getColumnTypeName( 1 )
						+ " jdbc=" + result.getMetaData().getColumnType( 1 ) );
			}
		}
		for ( var date : List.of( LocalDate.of( -4712, 1, 1 ), LocalDate.of( -4712, 1, 2 ), LocalDate.of( -4713, 12, 31 ),
				LocalDate.of( -1, 12, 31 ), LocalDate.of( 0, 1, 1 ), LocalDate.of( 1, 1, 1 ),
				LocalDate.of( 2024, 7, 15 ), LocalDate.of( 294276, 12, 30 ), LocalDate.of( 294276, 12, 31 ), LocalDate.of( 294277, 1, 1 ),
				LocalDate.of( 5874897, 12, 31 ), LocalDate.MIN, LocalDate.MAX ) ) {
			var savepoint = connection.setSavepoint();
			try ( var insert = connection.prepareStatement( "insert into edb_temporal_date_probe (value) values (?)" ) ) {
				insert.setObject( 1, date ); insert.executeUpdate();
				try ( var query = connection.createStatement(); var result = query.executeQuery( "select value from edb_temporal_date_probe" ) ) {
					assertThat( result.next() ).isTrue();
					var actual = result.getObject( 1, LocalDate.class );
					System.out.println( "EDB_DATE_CANDIDATE mode=" + mode + " type=" + type + " input=" + date
							+ " output=" + actual + " raw=" + result.getString( 1 ) + " exact=" + date.equals( actual ) );
				}
			}
			catch (SQLException failure) {
				System.out.println( "EDB_DATE_CANDIDATE mode=" + mode + " type=" + type + " input=" + date
						+ " state=" + failure.getSQLState() + " message=" + failure.getMessage() );
			}
			finally { connection.rollback( savepoint ); connection.releaseSavepoint( savepoint ); }
		}
		try ( var statement = connection.createStatement() ) { statement.execute( "drop table edb_temporal_date_probe" ); }
	}
}
