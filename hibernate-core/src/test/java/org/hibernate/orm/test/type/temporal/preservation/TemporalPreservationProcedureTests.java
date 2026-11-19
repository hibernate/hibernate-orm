package org.hibernate.orm.test.type.temporal.preservation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.TemporalPreservationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Verifies against a live database what temporal preservation does with the
/// parameters of a stored procedure. A value passed in is bound as any other
/// parameter is, by position and by name, and is stored unchanged whatever
/// the time zone of the database session; a null is passed as a null. A
/// value passed out, by an `OUT` or an `INOUT` parameter, is rejected before
/// the procedure is executed, since
/// no JDBC driver has been established to return Java Time values from a
/// [java.sql.CallableStatement] without loss.
///
/// The procedures are written for each database, so the test runs on those
/// for which they have been.
///
/// @author Steve Ebersole
@RequiresDialect(HSQLDialect.class)
@RequiresDialect(value = PostgreSQLDialect.class, matchSubTypes = false)
@RequiresDialect(MySQLDialect.class)
@RequiresDialect(SQLServerDialect.class)
class TemporalPreservationProcedureTests {
	private static final String[] DATABASE_SESSION_ZONES = { null, "Asia/Kathmandu", "America/New_York" };

	private static final LocalDateTime DATE_TIME = LocalDateTime.of( 2021, 3, 14, 2, 30, 0, 123_456_000 );
	private static final LocalDate DATE = LocalDate.of( 2021, 3, 14 );
	private static final Instant INSTANT = Instant.parse( "2021-11-07T05:30:00.123456Z" );

	@Test
	void valuesPassedInByPositionAreStoredUnchanged() {
		inEverySessionZone( true, session -> {
			final var call = session.createStoredProcedureQuery( "tp_store" );
			call.registerStoredProcedureParameter( 1, Integer.class, ParameterMode.IN );
			call.registerStoredProcedureParameter( 2, LocalDateTime.class, ParameterMode.IN );
			call.registerStoredProcedureParameter( 3, LocalDate.class, ParameterMode.IN );
			call.registerStoredProcedureParameter( 4, Instant.class, ParameterMode.IN );
			call.setParameter( 1, 1 );
			call.setParameter( 2, DATE_TIME );
			call.setParameter( 3, DATE );
			call.setParameter( 4, INSTANT );
			call.execute();
			assertStored( session, 1, DATE_TIME, DATE, INSTANT );
		} );
	}

	@Test
	void valuesPassedInByNameAreStoredUnchanged() {
		inEverySessionZone( true, session -> {
			final var call = session.createStoredProcedureQuery( "tp_store" );
			call.registerStoredProcedureParameter( "p_id", Integer.class, ParameterMode.IN );
			call.registerStoredProcedureParameter( "p_ldt", LocalDateTime.class, ParameterMode.IN );
			call.registerStoredProcedureParameter( "p_ld", LocalDate.class, ParameterMode.IN );
			call.registerStoredProcedureParameter( "p_instant", Instant.class, ParameterMode.IN );
			call.setParameter( "p_id", 1 );
			call.setParameter( "p_ldt", DATE_TIME );
			call.setParameter( "p_ld", DATE );
			call.setParameter( "p_instant", INSTANT );
			call.execute();
			assertStored( session, 1, DATE_TIME, DATE, INSTANT );
		} );
	}

	@Test
	void nullsPassedInAreStoredAsNulls() {
		inEverySessionZone( true, session -> {
			final var byPosition = session.createStoredProcedureQuery( "tp_store" );
			byPosition.registerStoredProcedureParameter( 1, Integer.class, ParameterMode.IN );
			byPosition.registerStoredProcedureParameter( 2, LocalDateTime.class, ParameterMode.IN );
			byPosition.registerStoredProcedureParameter( 3, LocalDate.class, ParameterMode.IN );
			byPosition.registerStoredProcedureParameter( 4, Instant.class, ParameterMode.IN );
			byPosition.setParameter( 1, 1 );
			byPosition.setParameter( 2, null );
			byPosition.setParameter( 3, null );
			byPosition.setParameter( 4, null );
			byPosition.execute();
			assertStored( session, 1, null, null, null );

			final var byName = session.createStoredProcedureQuery( "tp_store" );
			byName.registerStoredProcedureParameter( "p_id", Integer.class, ParameterMode.IN );
			byName.registerStoredProcedureParameter( "p_ldt", LocalDateTime.class, ParameterMode.IN );
			byName.registerStoredProcedureParameter( "p_ld", LocalDate.class, ParameterMode.IN );
			byName.registerStoredProcedureParameter( "p_instant", Instant.class, ParameterMode.IN );
			byName.setParameter( "p_id", 2 );
			byName.setParameter( "p_ldt", null );
			byName.setParameter( "p_ld", null );
			byName.setParameter( "p_instant", null );
			byName.execute();
			assertStored( session, 2, null, null, null );
		} );
	}

	/// A value passed out is rejected, by position and by name, also where
	/// what the procedure returns is a null.
	@Test
	void valuesPassedOutAreRejected() {
		final Dialect[] dialect = new Dialect[1];
		inEverySessionZone( true, session -> {
			dialect[0] = session.getSessionFactory().unwrap( SessionFactoryImplementor.class ).getJdbcServices().getDialect();
			// PostgreSQL returns a value from a procedure through an INOUT parameter
			final var mode = dialect[0] instanceof PostgreSQLDialect ? ParameterMode.INOUT : ParameterMode.OUT;
			for ( boolean stored : new boolean[] { true, false } ) {
				final int id = stored ? 1 : 2;
				final var entity = new Stored();
				entity.id = id;
				entity.localDateTime = stored ? DATE_TIME : null;
				session.persist( entity );
				session.flush();

				assertThatThrownBy( () -> {
					final var call = session.createStoredProcedureQuery( "tp_load" );
					call.registerStoredProcedureParameter( 1, Integer.class, ParameterMode.IN );
					call.registerStoredProcedureParameter( 2, LocalDateTime.class, mode );
					call.setParameter( 1, id );
					if ( mode == ParameterMode.INOUT ) {
						call.setParameter( 2, DATE_TIME );
					}
					call.execute();
					call.getOutputParameterValue( 2 );
				} ).as( "by position, stored: " + stored )
						.isInstanceOf( TemporalPreservationException.class )
						.hasStackTraceContaining( "output parameter of a stored procedure or function" );

				assertThatThrownBy( () -> {
					final var call = session.createStoredProcedureQuery( "tp_load" );
					call.registerStoredProcedureParameter( "p_id", Integer.class, ParameterMode.IN );
					call.registerStoredProcedureParameter( "p_ldt", LocalDateTime.class, mode );
					call.setParameter( "p_id", id );
					if ( mode == ParameterMode.INOUT ) {
						call.setParameter( "p_ldt", DATE_TIME );
					}
					call.execute();
					call.getOutputParameterValue( "p_ldt" );
				} ).as( "by name, stored: " + stored )
						.isInstanceOf( TemporalPreservationException.class )
						.hasStackTraceContaining( "output parameter of a stored procedure or function" );
			}
		} );
	}

	/// A value passed in and out again is rejected when it is read.
	@Test
	void valuesPassedInAndOutAreRejected() {
		inEverySessionZone( true, session -> {
			assertThatThrownBy( () -> {
				final var call = session.createStoredProcedureQuery( "tp_echo" );
				call.registerStoredProcedureParameter( 1, LocalDate.class, ParameterMode.INOUT );
				call.setParameter( 1, DATE );
				call.execute();
				call.getOutputParameterValue( 1 );
			} ).as( "by position" )
					.isInstanceOf( TemporalPreservationException.class )
					.hasStackTraceContaining( "output parameter of a stored procedure or function" );

			assertThatThrownBy( () -> {
				final var call = session.createStoredProcedureQuery( "tp_echo" );
				call.registerStoredProcedureParameter( "p_ld", LocalDate.class, ParameterMode.INOUT );
				call.setParameter( "p_ld", DATE );
				call.execute();
				call.getOutputParameterValue( "p_ld" );
			} ).as( "by name" )
					.isInstanceOf( TemporalPreservationException.class )
					.hasStackTraceContaining( "output parameter of a stored procedure or function" );
		} );
	}

	/// With temporal preservation disabled, a value is passed out as it
	/// always was.
	@Test
	void valuesPassedOutAreReadWhenDisabled() {
		inEverySessionZone( false, session -> {
			final var dialect = session.getSessionFactory().unwrap( SessionFactoryImplementor.class ).getJdbcServices().getDialect();
			final var mode = dialect instanceof PostgreSQLDialect ? ParameterMode.INOUT : ParameterMode.OUT;
			final var entity = new Stored();
			entity.id = 1;
			entity.localDateTime = LocalDateTime.of( 2021, 6, 1, 12, 0, 0 );
			session.persist( entity );
			session.flush();
			final var call = session.createStoredProcedureQuery( "tp_load" );
			call.registerStoredProcedureParameter( 1, Integer.class, ParameterMode.IN );
			call.registerStoredProcedureParameter( 2, LocalDateTime.class, mode );
			call.setParameter( 1, 1 );
			if ( mode == ParameterMode.INOUT ) {
				call.setParameter( 2, entity.localDateTime );
			}
			call.execute();
			assertThat( call.getOutputParameterValue( 2 ) ).isEqualTo( entity.localDateTime );
		} );
	}

	private static void assertStored(Session session, int id, LocalDateTime dateTime, LocalDate date, Instant instant) {
		session.clear();
		final var stored = session.find( Stored.class, id );
		assertThat( stored ).isNotNull();
		assertThat( stored.localDateTime ).isEqualTo( dateTime );
		assertThat( stored.localDate ).isEqualTo( date );
		assertThat( stored.instant ).isEqualTo( instant );
	}

	private static void inEverySessionZone(boolean enabled, Consumer<Session> action) {
		final var builder = ServiceRegistryUtil.serviceRegistryBuilder();
		builder.applySetting( "hibernate.hbm2ddl.auto", "create-drop" );
		// explicitly, since the build may enable it for every test
		builder.applySetting( MappingSettings.TEMPORAL_PRESERVATION, enabled );
		try ( var registry = builder.build() ) {
			final var metadata = (MetadataImplementor) new MetadataSources( registry )
					.addAnnotatedClass( Stored.class )
					.buildMetadata();
			try ( var factory = (SessionFactoryImplementor) metadata.buildSessionFactory() ) {
				final var dialect = factory.getJdbcServices().getDialect();
				final var value = metadata.getEntityBinding( Stored.class.getName() );
				final String dateTimeType = columnType( metadata, value.getProperty( "localDateTime" ).getColumns().get( 0 ) );
				final String dateType = columnType( metadata, value.getProperty( "localDate" ).getColumns().get( 0 ) );
				final String instantType = columnType( metadata, value.getProperty( "instant" ).getColumns().get( 0 ) );
				execute( factory, dropProcedures( dialect ), true );
				execute( factory, createProcedures( dialect, dateTimeType, dateType, instantType ), false );
				try {
					for ( String zone : DATABASE_SESSION_ZONES ) {
						TemporalPreservationOffsetTests.inSession( factory, null, zone, action );
					}
				}
				finally {
					execute( factory, dropProcedures( dialect ), true );
				}
			}
		}
	}

	private static String columnType(MetadataImplementor metadata, Column column) {
		return column.getSqlType( metadata );
	}

	private static void execute(SessionFactoryImplementor factory, List<String> statements, boolean ignoreFailure) {
		for ( String sql : statements ) {
			try {
				factory.inTransaction( session -> session.doWork( connection -> {
					try ( var statement = connection.createStatement() ) {
						statement.execute( sql );
					}
				} ) );
			}
			catch (RuntimeException e) {
				if ( !ignoreFailure ) {
					throw e;
				}
			}
		}
	}

	private static List<String> dropProcedures(Dialect dialect) {
		return List.of( "drop procedure tp_store", "drop procedure tp_load", "drop procedure tp_echo" );
	}

	private static List<String> createProcedures(Dialect dialect, String dateTime, String date, String instant) {
		final String insert = "insert into tp_proc (id, ldt_value, ld_value, instant_value)";
		if ( dialect instanceof MySQLDialect ) {
			return List.of(
					"create procedure tp_store(in p_id int, in p_ldt " + dateTime + ", in p_ld " + date
							+ ", in p_instant " + instant + ") begin " + insert
							+ " values (p_id, p_ldt, p_ld, p_instant); end",
					"create procedure tp_load(in p_id int, out p_ldt " + dateTime
							+ ") begin select ldt_value into p_ldt from tp_proc where id = p_id; end",
					"create procedure tp_echo(inout p_ld " + date + ") begin set p_ld = p_ld; end"
			);
		}
		else if ( dialect instanceof PostgreSQLDialect ) {
			return List.of(
					"create procedure tp_store(in p_id int, in p_ldt " + dateTime + ", in p_ld " + date
							+ ", in p_instant " + instant + ") language plpgsql as $$ begin " + insert
							+ " values (p_id, p_ldt, p_ld, p_instant); end $$",
					"create procedure tp_load(in p_id int, inout p_ldt " + dateTime
							+ ") language plpgsql as $$ begin select ldt_value into p_ldt from tp_proc where id = p_id; end $$",
					"create procedure tp_echo(inout p_ld " + date + ") language plpgsql as $$ begin p_ld := p_ld; end $$"
			);
		}
		else if ( dialect instanceof SQLServerDialect ) {
			return List.of(
					"create procedure tp_store @p_id int, @p_ldt " + dateTime + ", @p_ld " + date
							+ ", @p_instant " + instant + " as " + insert
							+ " values (@p_id, @p_ldt, @p_ld, @p_instant)",
					"create procedure tp_load @p_id int, @p_ldt " + dateTime
							+ " output as select @p_ldt = ldt_value from tp_proc where id = @p_id",
					"create procedure tp_echo @p_ld " + date + " output as set @p_ld = @p_ld"
			);
		}
		else {
			return List.of(
					"create procedure tp_store(in p_id int, in p_ldt " + dateTime + ", in p_ld " + date
							+ ", in p_instant " + instant + ") modifies sql data begin atomic " + insert
							+ " values (p_id, p_ldt, p_ld, p_instant); end",
					"create procedure tp_load(in p_id int, out p_ldt " + dateTime
							+ ") reads sql data begin atomic set p_ldt = (select ldt_value from tp_proc where id = p_id); end",
					"create procedure tp_echo(inout p_ld " + date + ") begin atomic set p_ld = p_ld; end"
			);
		}
	}

	@Entity(name = "Stored")
	@Table(name = "tp_proc")
	static class Stored {
		@Id
		Integer id;
		@jakarta.persistence.Column(name = "ldt_value")
		LocalDateTime localDateTime;
		@jakarta.persistence.Column(name = "ld_value")
		LocalDate localDate;
		@jakarta.persistence.Column(name = "instant_value")
		Instant instant;
	}
}
