/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.tm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.test.jdbc.Person;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6780")
@SkipForDialect( value ={ PostgreSQL81Dialect.class}, comment = "PostgreSQL jdbc driver doesn't impl timeout method")
public class TransactionTimeoutTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] {"jdbc/Mappings.hbm.xml"};
	}

	@Test
	public void testJdbcCoordinatorTransactionTimeoutCheck() {
		Session session = openSession();
		Transaction transaction = session.getTransaction();
		transaction.setTimeout( 2 );
		assertEquals( -1, ((SessionImplementor)session).getTransactionCoordinator().getJdbcCoordinator().determineRemainingTransactionTimeOutPeriod() );
		transaction.begin();
		assertNotSame( -1, ((SessionImplementor)session).getTransactionCoordinator().getJdbcCoordinator().determineRemainingTransactionTimeOutPeriod() );
		transaction.commit();
		session.close();
	}

	@Test(expected = TransactionException.class)
	public void testTransactionTimeoutFailure() throws InterruptedException {
		Session session = openSession();
		Transaction transaction = session.getTransaction();
		transaction.setTimeout( 1 );
		assertEquals( -1, ((SessionImplementor)session).getTransactionCoordinator().getJdbcCoordinator().determineRemainingTransactionTimeOutPeriod() );
		transaction.begin();
		Thread.sleep( 1000 );
		session.persist( new Person( "Lukasz", "Antoniak" ) );
		transaction.commit();
		session.close();
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
