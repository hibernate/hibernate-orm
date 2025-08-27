/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Steve Ebersole
 */
@RequiresDialect(DerbyDialect.class)
@Jpa(
		annotatedClasses = { DerbyDateTimeParameterTest.Message.class }
)
public class DerbyDateTimeParameterTest {

	private static GregorianCalendar nowCal = new GregorianCalendar();
	private static Date now = new Date( nowCal.getTime().getTime() );

	@Test
	public void testBindingCalendarAsDate(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					StoredProcedureQuery query = em.createStoredProcedureQuery( "findMessagesByDate" );
					query.registerStoredProcedureParameter( 1, Calendar.class, ParameterMode.IN );
					query.setParameter( 1, nowCal, TemporalType.DATE );
					List list = query.getResultList();
					assertEquals( 1, list.size() );
				}
		);
	}

	@Test
	public void testBindingCalendarAsTime(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					StoredProcedureQuery query = em.createStoredProcedureQuery( "findMessagesByTime" );
					query.registerStoredProcedureParameter( 1, Calendar.class, ParameterMode.IN );
					query.setParameter( 1, nowCal, TemporalType.TIME );
					List list = query.getResultList();
					assertEquals( 1, list.size() );
				}
		);
	}

	@BeforeEach
	public void startUp(EntityManagerFactoryScope scope) {
		// create the procedures
		createTestData( scope );
		createProcedures( scope.getEntityManagerFactory() );
	}


	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		deleteTestData( scope );
		dropProcedures( scope.getEntityManagerFactory() );
	}

	private void createProcedures(EntityManagerFactory emf) {
		final SessionFactoryImplementor sf = emf.unwrap( SessionFactoryImplementor.class );
		final JdbcConnectionAccess connectionAccess = sf.getServiceRegistry()
				.getService( JdbcServices.class )
				.getBootstrapJdbcConnectionAccess();
		final Connection conn;
		try {
			conn = connectionAccess.obtainConnection();
			conn.setAutoCommit( false );

			try {
				Statement statement = conn.createStatement();

				// drop them, just to be sure
				try {
					dropProcedures( statement );
				}
				catch (SQLException ignore) {
				}

				statement.execute(
						"CREATE PROCEDURE findMessagesByDate(IN chkDt DATE) " +
								"language java " +
								"dynamic result sets 1 " +
								"external name 'org.hibernate.community.dialect.DerbyDateTimeParameterTest.findMessagesByDate' " +
								"parameter style java"
				);

				statement.execute(
						"CREATE PROCEDURE findMessagesByTime(IN chkTime TIME) " +
								"language java " +
								"dynamic result sets 1 " +
								"external name 'org.hibernate.community.dialect.DerbyDateTimeParameterTest.findMessagesByTime' " +
								"parameter style java"
				);

				statement.execute(
						"CREATE PROCEDURE findMessagesByTimestampRange(IN startDt TIMESTAMP, IN endDt TIMESTAMP) " +
								"language java " +
								"dynamic result sets 1 " +
								"external name 'org.hibernate.community.dialect.DerbyDateTimeParameterTest.findMessagesByTimestampRange' " +
								"parameter style java"
				);

				statement.execute(
						"CREATE PROCEDURE retrieveTimestamp(IN ts1 TIMESTAMP, OUT ts2 TIMESTAMP) " +
								"language java " +
								"dynamic result sets 0 " +
								"external name 'org.hibernate.community.dialect.DerbyDateTimeParameterTest.retrieveTimestamp' " +
								"parameter style java"
				);

				try {
					statement.close();
				}
				catch (SQLException ignore) {
				}
			}
			finally {
				try {
					conn.commit();
				}
				catch (SQLException e) {
					System.out.println( "Unable to commit transaction after creating creating procedures" );
				}

				try {
					connectionAccess.releaseConnection( conn );
				}
				catch (SQLException ignore) {
				}
			}
		}
		catch (SQLException e) {
			throw new RuntimeException( "Unable to create stored procedures", e );
		}
	}

	private void dropProcedures(Statement statement) throws SQLException {
		statement.execute( "DROP PROCEDURE findMessagesByDate" );
		statement.execute( "DROP PROCEDURE findMessagesByTime" );
		statement.execute( "DROP PROCEDURE findMessagesByTimestampRange" );
		statement.execute( "DROP PROCEDURE retrieveTimestamp" );
	}

	public static void findMessagesByDate(java.sql.Date date, ResultSet[] results) throws SQLException {
		try (Connection conn = DriverManager.getConnection( "jdbc:default:connection" )) {
			PreparedStatement ps = conn.prepareStatement( "select * from msg where post_date=?" );
			ps.setDate( 1, date );
			results[0] = ps.executeQuery();
		}
	}

	public static void findMessagesByTime(java.sql.Time time, ResultSet[] results) throws SQLException {
		try (Connection conn = DriverManager.getConnection( "jdbc:default:connection" )) {
			PreparedStatement ps = conn.prepareStatement( "select * from msg where post_time=?" );
			ps.setTime( 1, time );
			results[0] = ps.executeQuery();
		}
	}

	public static void findMessagesByTimestampRange(Timestamp start, Timestamp end, ResultSet[] results)
			throws SQLException {
		try (Connection conn = DriverManager.getConnection( "jdbc:default:connection" )) {
			PreparedStatement ps = conn.prepareStatement( "select * from msg where ts between ? and ?" );
			ps.setDate( 1, new java.sql.Date( start.getTime() ) );
			ps.setDate( 2, new java.sql.Date( end.getTime() ) );
			results[0] = ps.executeQuery();
		}
	}

	public static void retrieveTimestamp(Timestamp in, Timestamp[] out) throws SQLException {
		out[0] = in;
	}

	private void createTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					em.persist( new Message( 1, "test", now, now, now ) );
				}
		);
	}

	private void deleteTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em ->
						em.createQuery( "delete from Message" ).executeUpdate()
		);
	}

	private void dropProcedures(EntityManagerFactory emf) {
		final SessionFactoryImplementor sf = emf.unwrap( SessionFactoryImplementor.class );
		final JdbcConnectionAccess connectionAccess = sf.getServiceRegistry()
				.getService( JdbcServices.class )
				.getBootstrapJdbcConnectionAccess();
		final Connection conn;
		try {
			conn = connectionAccess.obtainConnection();
			conn.setAutoCommit( false );

			try {
				Statement statement = conn.createStatement();
				dropProcedures( statement );
				try {
					statement.close();
				}
				catch (SQLException ignore) {
				}
			}
			finally {
				try {
					conn.commit();
				}
				catch (SQLException e) {
					System.out.println( "Unable to commit transaction after creating dropping procedures" );
				}

				try {
					connectionAccess.releaseConnection( conn );
				}
				catch (SQLException ignore) {
				}
			}
		}
		catch (SQLException e) {
			throw new RuntimeException( "Unable to drop stored procedures", e );
		}
	}

	@Entity(name = "Message")
	@Table(name = "MSG")
	public static class Message {
		@Id
		private Integer id;
		private String body;
		@Column(name = "POST_DATE")
		@Temporal(TemporalType.DATE)
		private Date postDate;
		@Column(name = "POST_TIME")
		@Temporal(TemporalType.TIME)
		private Date postTime;
		@Column(name = "TS")
		@Temporal(TemporalType.TIMESTAMP)
		private Date ts;

		public Message() {
		}

		public Message(Integer id, String body, Date postDate, Date postTime, Date ts) {
			this.id = id;
			this.body = body;
			this.postDate = postDate;
			this.postTime = postTime;
			this.ts = ts;
		}
	}
}
