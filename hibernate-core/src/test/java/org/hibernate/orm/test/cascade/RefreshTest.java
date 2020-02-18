/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cascade;

import java.sql.Statement;
import java.util.Date;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
	public void testRefreshCascade() {
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

		batch.getJobs().forEach( job -> assertEquals( "Jobs not refreshed!", 1, job.getStatus() ) );

		txn.rollback();
		session.close();
	}

	private void updateStatuses(final SessionImplementor session) {
		session.doWork(
			connection -> {
				Statement stmnt = null;
				try {
					stmnt = session.getJdbcCoordinator().getStatementPreparer().createStatement();
					stmnt.execute( "UPDATE T_JOB SET JOB_STATUS = 1" );
				}
				finally {
					if ( stmnt != null ) {
						try {
							session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( stmnt );
						}
						catch( Throwable ignore ) {
						}
					}
				}
			}
		);
	}
}
