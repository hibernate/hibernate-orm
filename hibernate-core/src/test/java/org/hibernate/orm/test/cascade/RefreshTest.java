/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cascade;

import java.sql.PreparedStatement;
import java.util.Date;
import java.util.Iterator;

import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Implementation of RefreshTest.
 *
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/cascade/Job.hbm.xml",
				"org/hibernate/orm/test/cascade/JobBatch.hbm.xml"
		}
)
@SessionFactory
public class RefreshTest {

	@Test
	public void testRefreshCascade(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					JobBatch batch = new JobBatch( new Date() );
					batch.createJob().setProcessingInstructions( "Just do it!" );
					batch.createJob().setProcessingInstructions( "I know you can do it!" );

					// write the stuff to the database; at this stage all job.status values are zero
					session.persist( batch );
					session.flush();

					// behind the session's back, let's modify the statuses
					updateStatuses( session );

					// Now lets refresh the persistent batch, and see if the refresh cascaded to the jobs collection elements
					session.refresh( batch );

					Iterator itr = batch.getJobs().iterator();
					while ( itr.hasNext() ) {
						Job job = (Job) itr.next();
						assertEquals( 1, job.getStatus(), "Jobs not refreshed!" );
					}
				}
		);
	}

	private void updateStatuses(final SessionImplementor session) {
		session.doWork(
				connection -> {
					PreparedStatement stmnt = null;
					try {
						stmnt = session.getJdbcCoordinator().getStatementPreparer().prepareStatement(
								"UPDATE T_JOB SET JOB_STATUS = 1" );
						session.getJdbcCoordinator().getResultSetReturn().executeUpdate( stmnt );
					}
					finally {
						if ( stmnt != null ) {
							try {
								session.getJdbcCoordinator().getResourceRegistry().release( stmnt );
							}
							catch (Throwable ignore) {
							}
						}
					}
				}
		);
	}
}
