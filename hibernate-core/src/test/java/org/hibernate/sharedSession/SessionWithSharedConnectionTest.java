/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.sharedSession;

import org.hibernate.Criteria;
import org.hibernate.IrrelevantEntity;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;

import org.junit.Test;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class SessionWithSharedConnectionTest extends BaseCoreFunctionalTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-7020" )
	@FailureExpected( jiraKey = "HHH-7020" )
	public void testSharedTransactionContextSessionClosing() {
		Session session = sessionFactory().openSession();
		session.getTransaction().begin();

		Session secondSession = session.sessionWithOptions()
				.connection()
				.openSession();
		secondSession.createCriteria( IrrelevantEntity.class ).list();

		//the list should have registered and then released a JDBC resource
		assertFalse(
				((SessionImplementor) secondSession).getTransactionCoordinator()
						.getJdbcCoordinator()
						.getLogicalConnection()
						.getResourceRegistry()
						.hasRegisteredResources()
		);

		//there should be no registered JDBC resources on the original session
// not sure this is ultimately a valid assertion
//		assertFalse(
//				((SessionImplementor) session).getTransactionCoordinator()
//						.getJdbcCoordinator()
//						.getLogicalConnection()
//						.getResourceRegistry()
//						.hasRegisteredResources()
//		);

		secondSession.close();

		session.getTransaction().commit();
		//the session should be allowed to close properly as well
		session.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-7020" )
	@FailureExpected( jiraKey = "HHH-7020" )
	public void testSharedTransactionContextAutoClosing() {
		Session session = sessionFactory().openSession();
		session.getTransaction().begin();

		Session secondSession = session.sessionWithOptions()
				.connection()
				.autoClose( true )
				.openSession();
		secondSession.createCriteria( IrrelevantEntity.class ).list();

		//the list should have registered and then released a JDBC resource
		assertFalse(
				((SessionImplementor) secondSession).getTransactionCoordinator()
						.getJdbcCoordinator()
						.getLogicalConnection()
						.getResourceRegistry()
						.hasRegisteredResources()
		);
		//there should be no registered JDBC resources on the original session
// not sure this is ultimately a valid assertion
//		assertFalse(
//				((SessionImplementor) session).getTransactionCoordinator()
//						.getJdbcCoordinator()
//						.getLogicalConnection()
//						.getResourceRegistry()
//						.hasRegisteredResources()
//		);

		session.getTransaction().commit();

		assertTrue( ((SessionImplementor) session).isClosed() );
		assertTrue( ((SessionImplementor) secondSession).isClosed() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { IrrelevantEntity.class };
	}
}
