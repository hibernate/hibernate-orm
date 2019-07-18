/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.connections;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(H2Dialect.class)
public class AggressiveReleaseQueryIterationTest extends ConnectionManagementTestCase {
	@Override
	@SuppressWarnings("unchecked")
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		TestingJtaBootstrap.prepare( settings );
//		settings.put( Environment.TRANSACTION_STRATEGY, CMTTransactionFactory.class.getName() );
		settings.put( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, JtaTransactionCoordinatorBuilderImpl.class.getName() );
		settings.put( Environment.RELEASE_CONNECTIONS, ConnectionReleaseMode.AFTER_STATEMENT.toString() );
		settings.put( Environment.GENERATE_STATISTICS, "true" );
		settings.put( Environment.STATEMENT_BATCH_SIZE, "0" );
	}

	@Override
	protected Session getSessionUnderTest() throws Throwable {
		return openSession();
	}

	@Override
	protected void reconnect(Session session) {
	}

	@Override
	protected void prepare() throws Throwable {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
	}

	@Override
	protected void done() throws Throwable {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	@Test
	public void testQueryIteration() throws Throwable {
		prepare();
		Session s = getSessionUnderTest();
		Silly silly = new Silly( "silly" );
		s.save( silly );
		s.flush();

		List itr = s.createQuery( "from Silly" ).iterate();
		assertTrue( itr.hasNext() );
		Silly silly2 = ( Silly ) itr.next();
		assertEquals( silly, silly2 );
		Hibernate.close( itr );

		itr = s.createQuery( "from Silly" ).iterate();
		Iterator itr2 = s.createQuery( "from Silly where name = 'silly'" ).iterate();

		assertTrue( itr.hasNext() );
		assertEquals( silly, itr.next() );
		assertTrue( itr2.hasNext() );
		assertEquals( silly, itr2.next() );

		Hibernate.close( itr );
		Hibernate.close( itr2 );

		s.delete( silly );
		s.flush();

		release( s );
		done();
	}


}
