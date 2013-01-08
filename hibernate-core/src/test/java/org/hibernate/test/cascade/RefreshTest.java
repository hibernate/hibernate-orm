/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.cascade;
import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Implementation of RefreshTest.
 *
 * @author Steve Ebersole
 */
public class RefreshTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "cascade/Job.hbm.xml", "cascade/JobBatch.hbm.xml" };
	}

	@Test
	public void testRefreshCascade() throws Throwable {
		Session session = openSession();
		Transaction txn = session.beginTransaction();

		JobBatch batch = new JobBatch( new Date() );
		batch.createJob().setProcessingInstructions( "Just do it!" );
		batch.createJob().setProcessingInstructions( "I know you can do it!" );

		// write the stuff to the database; at this stage all job.status values are zero
		session.persist( batch );
		session.flush();

		// behind the session's back, let's modify the statuses
		updateStatuses( (SessionImplementor)session );

		// Now lets refresh the persistent batch, and see if the refresh cascaded to the jobs collection elements
		session.refresh( batch );

		Iterator itr = batch.getJobs().iterator();
		while( itr.hasNext() ) {
			Job job = ( Job ) itr.next();
			assertEquals( "Jobs not refreshed!", 1, job.getStatus() );
		}

		txn.rollback();
		session.close();
	}

	private void updateStatuses(final SessionImplementor session) throws Throwable {
		((Session)session).doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						PreparedStatement stmnt = null;
						try {
							stmnt = session.getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().prepareStatement( "UPDATE T_JOB SET JOB_STATUS = 1" );
							session.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().executeUpdate( stmnt );
						}
						finally {
							if ( stmnt != null ) {
								try {
									session.getTransactionCoordinator().getJdbcCoordinator().release( stmnt );
								}
								catch( Throwable ignore ) {
								}
							}
						}
					}
				}
		);
	}
}
