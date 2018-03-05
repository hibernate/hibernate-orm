/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.loadplans.process;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessor;
import org.hibernate.loader.plan.exec.query.spi.NamedParameterContext;
import org.hibernate.loader.plan.exec.spi.LoadQueryDetails;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.param.ParameterBinder;
import org.hibernate.persister.entity.EntityPersister;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.ExtraAssertions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */
public class EntityWithNonLazyOneToManySetResultSetProcessorTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Poster.class, Message.class };
	}

	@Test
	public void testEntityWithSet() throws Exception {
		final EntityPersister entityPersister = sessionFactory().getEntityPersister( Poster.class.getName() );

		// create some test data
		Session session = openSession();
		session.beginTransaction();
		Poster poster = new Poster();
		poster.pid = 0;
		poster.name = "John Doe";
		Message message1 = new Message();
		message1.mid = 1;
		message1.msgTxt = "Howdy!";
		message1.poster = poster;
		poster.messages.add( message1 );
		Message message2 = new Message();
		message2.mid = 2;
		message2.msgTxt = "Bye!";
		message2.poster = poster;
		poster.messages.add( message2 );
		session.save( poster );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		Poster posterGotten = (Poster) session.get( Poster.class, poster.pid );
		assertEquals( 0, posterGotten.pid.intValue() );
		assertEquals( poster.name, posterGotten.name );
		assertTrue( Hibernate.isInitialized( posterGotten.messages ) );
		assertEquals( 2, posterGotten.messages.size() );
		for ( Message message : posterGotten.messages ) {
			if ( message.mid == 1 ) {
				assertEquals( message1.msgTxt, message.msgTxt );
			}
			else if ( message.mid == 2 ) {
				assertEquals( message2.msgTxt, message.msgTxt );
			}
			else {
				fail( "unexpected message id." );
			}
			assertSame( posterGotten, message.poster );
		}
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
											Helper.parameterContext(),
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
			assertEquals( 2, results.size() );
			Object result1 = results.get( 0 );
			assertNotNull( result1 );
			assertSame( result1, results.get( 1 ) );

			Poster workPoster = ExtraAssertions.assertTyping( Poster.class, result1 );
			assertEquals( 0, workPoster.pid.intValue() );
			assertEquals( poster.name, workPoster.name );
			assertTrue( Hibernate.isInitialized( workPoster.messages ) );
			assertEquals( 2, workPoster.messages.size() );
			assertTrue( Hibernate.isInitialized( posterGotten.messages ) );
			assertEquals( 2, workPoster.messages.size() );
			for ( Message message : workPoster.messages ) {
				if ( message.mid == 1 ) {
					assertEquals( message1.msgTxt, message.msgTxt );
				}
				else if ( message.mid == 2 ) {
					assertEquals( message2.msgTxt, message.msgTxt );
				}
				else {
					fail( "unexpected message id." );
				}
				assertSame( workPoster, message.poster );
			}
			workSession.getTransaction().commit();
			workSession.close();
		}

		// clean up test data
		session = openSession();
		session.beginTransaction();
		session.delete( poster );
		session.getTransaction().commit();
		session.close();
	}

	@Entity( name = "Message" )
	public static class Message {
		@Id
		private Integer mid;
		private String msgTxt;
		@ManyToOne
		@JoinColumn
		private Poster poster;
	}

	@Entity( name = "Poster" )
	public static class Poster {
		@Id
		private Integer pid;
		private String name;
		@OneToMany(mappedBy = "poster", fetch = FetchType.EAGER, cascade = CascadeType.ALL )
		private Set<Message> messages = new HashSet<Message>();
	}
}
