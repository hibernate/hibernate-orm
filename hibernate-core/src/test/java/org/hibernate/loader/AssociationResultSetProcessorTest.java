/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.loader.internal.EntityLoadQueryBuilderImpl;
import org.hibernate.loader.internal.ResultSetProcessorImpl;
import org.hibernate.loader.plan.internal.SingleRootReturnLoadPlanBuilderStrategy;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.LoadPlanBuilder;
import org.hibernate.loader.spi.NamedParameterContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.ExtraAssertions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class AssociationResultSetProcessorTest extends BaseCoreFunctionalTestCase {

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
			final SingleRootReturnLoadPlanBuilderStrategy strategy = new SingleRootReturnLoadPlanBuilderStrategy(
					sessionFactory(),
					LoadQueryInfluencers.NONE,
					"abc",
					0
			);
			final LoadPlan plan = LoadPlanBuilder.buildRootEntityLoadPlan( strategy, entityPersister );
			final EntityLoadQueryBuilderImpl queryBuilder = new EntityLoadQueryBuilderImpl(
					sessionFactory(),
					LoadQueryInfluencers.NONE,
					plan
			);
			final String sql = queryBuilder.generateSql( 1 );

			final ResultSetProcessorImpl resultSetProcessor = new ResultSetProcessorImpl( plan );
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
			final SingleRootReturnLoadPlanBuilderStrategy strategy = new SingleRootReturnLoadPlanBuilderStrategy(
					sessionFactory(),
					LoadQueryInfluencers.NONE,
					"abc",
					0
			);
			final LoadPlan plan = LoadPlanBuilder.buildRootEntityLoadPlan( strategy, entityPersister );
			final EntityLoadQueryBuilderImpl queryBuilder = new EntityLoadQueryBuilderImpl(
					sessionFactory(),
					LoadQueryInfluencers.NONE,
					plan
			);
			final String sql = queryBuilder.generateSql( 1 );

			final ResultSetProcessorImpl resultSetProcessor = new ResultSetProcessorImpl( plan );
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
