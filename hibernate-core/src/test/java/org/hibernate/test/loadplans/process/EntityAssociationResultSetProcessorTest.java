/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.loadplans.process;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessor;
import org.hibernate.loader.plan.exec.query.spi.NamedParameterContext;
import org.hibernate.loader.plan.exec.spi.LoadQueryDetails;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.entity.EntityPersister;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.ExtraAssertions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class EntityAssociationResultSetProcessorTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Message.class, Poster.class, ReportedMessage.class };
	}

	@Test
	public void testManyToOneEntityProcessing() throws Exception {
		final EntityPersister entityPersister = sessionFactory().getEntityPersister( Message.class.getName() );

		// create some test data
		Session session = openSession();
		session.beginTransaction();
		Message message = new Message( 1, "the message" );
		Poster poster = new Poster( 2, "the poster" );
		session.save( message );
		session.save( poster );
		message.poster = poster;
		poster.messages.add( message );
		session.getTransaction().commit();
		session.close();

		{
			final LoadPlan plan = Helper.INSTANCE.buildLoadPlan( sessionFactory(), entityPersister );

			final LoadQueryDetails queryDetails = Helper.INSTANCE.buildLoadQueryDetails( plan, sessionFactory() );
			final String sql = queryDetails.getSqlStatement();
			final ResultSetProcessor resultSetProcessor = queryDetails.getResultSetProcessor();

			final List results = new ArrayList();

			final Session workSession = openSession();
			workSession.beginTransaction();
			workSession.doWork(
					new Work() {
						@Override
						public void execute(Connection connection) throws SQLException {
							PreparedStatement ps = connection.prepareStatement( sql );
							ps.setInt( 1, 1 );
							ResultSet resultSet = ps.executeQuery();
							results.addAll(
									resultSetProcessor.extractResults(
											resultSet,
											(SessionImplementor) workSession,
											new QueryParameters(),
											new NamedParameterContext() {
												@Override
												public int[] getNamedParameterLocations(String name) {
													return new int[0];
												}
											},
											true,
											false,
											null,
											null
									)
							);
							resultSet.close();
							ps.close();
						}
					}
			);
			assertEquals( 1, results.size() );
			Object result = results.get( 0 );
			assertNotNull( result );

			Message workMessage = ExtraAssertions.assertTyping( Message.class, result );
			assertEquals( 1, workMessage.mid.intValue() );
			assertEquals( "the message", workMessage.msgTxt );
			assertTrue( Hibernate.isInitialized( workMessage.poster ) );
			Poster workPoster = workMessage.poster;
			assertEquals( 2, workPoster.pid.intValue() );
			assertEquals( "the poster", workPoster.name );
			assertFalse( Hibernate.isInitialized( workPoster.messages ) );

			workSession.getTransaction().commit();
			workSession.close();
		}

		// clean up test data
		session = openSession();
		session.beginTransaction();
		session.createQuery( "delete Message" ).executeUpdate();
		session.createQuery( "delete Poster" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testNestedManyToOneEntityProcessing() throws Exception {
		final EntityPersister entityPersister = sessionFactory().getEntityPersister( ReportedMessage.class.getName() );

		// create some test data
		Session session = openSession();
		session.beginTransaction();
		Message message = new Message( 1, "the message" );
		Poster poster = new Poster( 2, "the poster" );
		session.save( message );
		session.save( poster );
		message.poster = poster;
		poster.messages.add( message );
		ReportedMessage reportedMessage = new ReportedMessage( 0, "inappropriate", message );
		session.save( reportedMessage );
		session.getTransaction().commit();
		session.close();

		{
			final LoadPlan plan = Helper.INSTANCE.buildLoadPlan( sessionFactory(), entityPersister );

			final LoadQueryDetails queryDetails = Helper.INSTANCE.buildLoadQueryDetails( plan, sessionFactory() );
			final String sql = queryDetails.getSqlStatement();
			final ResultSetProcessor resultSetProcessor = queryDetails.getResultSetProcessor();

			final List results = new ArrayList();

			final Session workSession = openSession();
			workSession.beginTransaction();
			workSession.doWork(
					new Work() {
						@Override
						public void execute(Connection connection) throws SQLException {
							PreparedStatement ps = connection.prepareStatement( sql );
							ps.setInt( 1, 0 );
							ResultSet resultSet = ps.executeQuery();
							results.addAll(
									resultSetProcessor.extractResults(
											resultSet,
											(SessionImplementor) workSession,
											new QueryParameters(),
											new NamedParameterContext() {
												@Override
												public int[] getNamedParameterLocations(String name) {
													return new int[0];
												}
											},
											true,
											false,
											null,
											null
									)
							);
							resultSet.close();
							ps.close();
						}
					}
			);
			assertEquals( 1, results.size() );
			Object result = results.get( 0 );
			assertNotNull( result );

			ReportedMessage workReportedMessage = ExtraAssertions.assertTyping( ReportedMessage.class, result );
			assertEquals( 0, workReportedMessage.id.intValue() );
			assertEquals( "inappropriate", workReportedMessage.reason );
			Message workMessage = workReportedMessage.message;
			assertNotNull( workMessage );
			assertTrue( Hibernate.isInitialized( workMessage ) );
			assertEquals( 1, workMessage.mid.intValue() );
			assertEquals( "the message", workMessage.msgTxt );
			assertTrue( Hibernate.isInitialized( workMessage.poster ) );
			Poster workPoster = workMessage.poster;
			assertEquals( 2, workPoster.pid.intValue() );
			assertEquals( "the poster", workPoster.name );
			assertFalse( Hibernate.isInitialized( workPoster.messages ) );

			workSession.getTransaction().commit();
			workSession.close();
		}

		// clean up test data
		session = openSession();
		session.beginTransaction();
		session.createQuery( "delete ReportedMessage" ).executeUpdate();
		session.createQuery( "delete Message" ).executeUpdate();
		session.createQuery( "delete Poster" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	@Entity( name = "ReportedMessage" )
	public static class ReportedMessage {
		@Id
		private Integer id;
		private String reason;
		@ManyToOne
		@JoinColumn
		private Message message;

		public ReportedMessage() {}

		public ReportedMessage(Integer id, String reason, Message message) {
			this.id = id;
			this.reason = reason;
			this.message = message;
		}
	}

	@Entity( name = "Message" )
	public static class Message {
		@Id
		private Integer mid;
		private String msgTxt;
		@ManyToOne( cascade = CascadeType.MERGE )
		@JoinColumn
		private Poster poster;

		public Message() {}

		public Message(Integer mid, String msgTxt) {
			this.mid = mid;
			this.msgTxt = msgTxt;
		}
	}

	@Entity( name = "Poster" )
	public static class Poster {
		@Id
		private Integer pid;
		private String name;
		@OneToMany(mappedBy = "poster")
		private List<Message> messages = new ArrayList<Message>();

		public Poster() {}

		public Poster(Integer pid, String name) {
			this.pid = pid;
			this.name = name;
		}
	}
}
