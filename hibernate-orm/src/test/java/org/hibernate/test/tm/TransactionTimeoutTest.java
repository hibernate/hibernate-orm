/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.tm;

import javax.persistence.PersistenceException;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyHbmImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.jdbc.Person;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6780")
@SkipForDialect( value ={ PostgreSQL81Dialect.class, PostgreSQLDialect.class}, comment = "PostgreSQL jdbc driver doesn't impl timeout method")
public class TransactionTimeoutTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] {"jdbc/Mappings.hbm.xml"};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setImplicitNamingStrategy( ImplicitNamingStrategyLegacyHbmImpl.INSTANCE );
	}

	@Test
	public void testJdbcCoordinatorTransactionTimeoutCheck() {
		Session session = openSession();
		Transaction transaction = session.getTransaction();
		transaction.setTimeout( 2 );
		assertEquals( -1, ((SessionImplementor)session).getJdbcCoordinator().determineRemainingTransactionTimeOutPeriod() );
		transaction.begin();
		assertNotSame( -1, ((SessionImplementor)session).getJdbcCoordinator().determineRemainingTransactionTimeOutPeriod() );
		transaction.commit();
		session.close();
	}

	@Test
	public void testTransactionTimeoutFailure() throws InterruptedException {
		Session session = openSession();
		try {
			Transaction transaction = session.getTransaction();
			transaction.setTimeout( 1 );
			assertEquals( -1,
						  ( (SessionImplementor) session ).getJdbcCoordinator().determineRemainingTransactionTimeOutPeriod()
			);
			transaction.begin();
			Thread.sleep( 1000 );
			session.persist( new Person( "Lukasz", "Antoniak" ) );
			transaction.commit();
		}
		catch (TransactionException e) {
			// expected
		}
		catch (PersistenceException e) {
			assertTyping( TransactionException.class, e.getCause() );
		}
		finally {
			session.close();
		}
	}

	@Test
	public void testTransactionTimeoutSuccess() {
		Session session = openSession();
		Transaction transaction = session.getTransaction();
		transaction.setTimeout( 60 );
		transaction.begin();
		session.persist( new Person( "Lukasz", "Antoniak" ) );
		transaction.commit();
		session.close();
	}
}
