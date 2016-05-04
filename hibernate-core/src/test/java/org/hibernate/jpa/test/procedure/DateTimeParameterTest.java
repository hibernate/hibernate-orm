/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.procedure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.dialect.DerbyTenSevenDialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class DateTimeParameterTest extends BaseUnitTestCase {
	HibernateEntityManagerFactory entityManagerFactory;

	private static GregorianCalendar nowCal = new GregorianCalendar();
	private static Date now = new Date( nowCal.getTime().getTime() );

	@Test
	public void testBindingCalendarAsDate() {
		EntityManager em = entityManagerFactory.createEntityManager();
		em.getTransaction().begin();

		try {
			StoredProcedureQuery query = em.createStoredProcedureQuery( "findMessagesByDate" );
			query.registerStoredProcedureParameter( 1, Calendar.class, ParameterMode.IN );
			query.setParameter( 1, nowCal, TemporalType.DATE );
			List list = query.getResultList();
			assertEquals( 1, list.size() );
		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}
	}

	@Test
	public void testBindingCalendarAsTime() {
		EntityManager em = entityManagerFactory.createEntityManager();
		em.getTransaction().begin();

		try {
			StoredProcedureQuery query = em.createStoredProcedureQuery( "findMessagesByTime" );
			query.registerStoredProcedureParameter( 1, Calendar.class, ParameterMode.IN );
			query.setParameter( 1, nowCal, TemporalType.TIME );
			List list = query.getResultList();
			assertEquals( 1, list.size() );
		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}
	}

	@Before
	public void startUp() {
		// create the EMF
		entityManagerFactory = Bootstrap.getEntityManagerFactoryBuilder(
				buildPersistenceUnitDescriptor(),
				buildSettingsMap()
		).build().unwrap( HibernateEntityManagerFactory.class );

		// create the procedures
		createTestData( entityManagerFactory );
		createProcedures( entityManagerFactory );
	}

	private PersistenceUnitDescriptor buildPersistenceUnitDescriptor() {
		return new BaseEntityManagerFunctionalTestCase.TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() );
	}

	@SuppressWarnings("unchecked")
	private Map buildSettingsMap() {
		Map settings = new HashMap();

		settings.put( AvailableSettings.LOADED_CLASSES, Collections.singletonList( Message.class ) );

		settings.put( org.hibernate.cfg.AvailableSettings.DIALECT, DerbyTenSevenDialect.class.getName() );
		settings.put( org.hibernate.cfg.AvailableSettings.DRIVER, org.apache.derby.jdbc.EmbeddedDriver.class.getName() );
		settings.put( org.hibernate.cfg.AvailableSettings.URL, "jdbc:derby:memory:hibernate-orm-testing;create=true" );
		settings.put( org.hibernate.cfg.AvailableSettings.USER, "" );

		settings.put( org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		return settings;
	}

	@After
	public void tearDown() {
		if ( entityManagerFactory == null ) {
			return;
		}

		deleteTestData( entityManagerFactory );
		dropProcedures( entityManagerFactory );

		entityManagerFactory.close();
	}

	private void createProcedures(HibernateEntityManagerFactory emf) {
		final SessionFactoryImplementor sf = emf.unwrap( SessionFactoryImplementor.class );
		final JdbcConnectionAccess connectionAccess = sf.getServiceRegistry().getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess();
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
								"external name 'org.hibernate.jpa.test.procedure.DateTimeParameterTest.findMessagesByDate' " +
								"parameter style java"
				);

				statement.execute(
						"CREATE PROCEDURE findMessagesByTime(IN chkTime TIME) " +
								"language java " +
								"dynamic result sets 1 " +
								"external name 'org.hibernate.jpa.test.procedure.DateTimeParameterTest.findMessagesByTime' " +
								"parameter style java"
				);

				statement.execute(
						"CREATE PROCEDURE findMessagesByTimestampRange(IN startDt TIMESTAMP, IN endDt TIMESTAMP) " +
								"language java " +
								"dynamic result sets 1 " +
								"external name 'org.hibernate.jpa.test.procedure.DateTimeParameterTest.findMessagesByTimestampRange' " +
								"parameter style java"
				);

				statement.execute(
						"CREATE PROCEDURE retrieveTimestamp(IN ts1 TIMESTAMP, OUT ts2 TIMESTAMP) " +
								"language java " +
								"dynamic result sets 0 " +
								"external name 'org.hibernate.jpa.test.procedure.DateTimeParameterTest.retrieveTimestamp' " +
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
					System.out.println( "Unable to commit transaction afterQuery creating creating procedures");
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
		Connection conn = DriverManager.getConnection( "jdbc:default:connection" );
		PreparedStatement ps = conn.prepareStatement( "select * from msg where post_date=?" );
		ps.setDate( 1, date );
		results[0] = ps.executeQuery();
		conn.close();
	}

	public static void findMessagesByTime(java.sql.Time time, ResultSet[] results) throws SQLException {
		Connection conn = DriverManager.getConnection( "jdbc:default:connection" );
		PreparedStatement ps = conn.prepareStatement( "select * from msg where post_time=?" );
		ps.setTime( 1, time );
		results[0] = ps.executeQuery();
		conn.close();
	}

	public static void findMessagesByTimestampRange(Timestamp start, Timestamp end, ResultSet[] results) throws SQLException {
		Connection conn = DriverManager.getConnection( "jdbc:default:connection" );
		PreparedStatement ps = conn.prepareStatement( "select * from msg where ts between ? and ?" );
		ps.setDate( 1, new java.sql.Date( start.getTime() ) );
		ps.setDate( 2, new java.sql.Date( end.getTime() ) );
		results[0] = ps.executeQuery();
		conn.close();
	}

	public static void retrieveTimestamp(Timestamp in, Timestamp[] out ) throws SQLException {
		out[0] = in;
	}

	private void createTestData(HibernateEntityManagerFactory entityManagerFactory) {
		EntityManager em = entityManagerFactory.createEntityManager();
		em.getTransaction().begin();
		em.persist( new Message( 1, "test", now, now, now ) );
		em.getTransaction().commit();
		em.close();
	}

	private void deleteTestData(HibernateEntityManagerFactory entityManagerFactory) {
		EntityManager em = entityManagerFactory.createEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete from Message" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

	private void dropProcedures(HibernateEntityManagerFactory emf) {
		final SessionFactoryImplementor sf = emf.unwrap( SessionFactoryImplementor.class );
		final JdbcConnectionAccess connectionAccess = sf.getServiceRegistry().getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess();
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
					System.out.println( "Unable to commit transaction afterQuery creating dropping procedures");
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

	@Entity( name = "Message" )
	@Table( name = "MSG" )
	public static class Message {
		@Id
		private Integer id;
		private String body;
		@Column( name = "POST_DATE" )
		@Temporal( TemporalType.DATE )
		private Date postDate;
		@Column( name = "POST_TIME" )
		@Temporal( TemporalType.TIME )
		private Date postTime;
		@Column( name = "TS" )
		@Temporal( TemporalType.TIMESTAMP )
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
