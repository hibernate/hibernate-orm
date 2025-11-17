/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade;

import java.sql.Statement;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Implementation of RefreshTest.
 *
 * @author Steve Ebersole
 */
@DomainModel(
	annotatedClasses = {
		RefreshTest.Job.class,
		RefreshTest.JobBatch.class
	}
)
@SessionFactory
public class RefreshTest {

	private JobBatch batch;

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
			session -> {
				batch = new JobBatch( new Date() );
				batch.createJob().processingInstructions = "Just do it!";
				batch.createJob().processingInstructions = "I know you can do it!";

				// write the stuff to the database; at this stage all job.status values are zero
				session.persist( batch );
			}
		);

		// behind the session's back, let's modify the statuses to one
		scope.inSession( this::updateStatuses );
	}

	@Test
	void testCannotRefreshCascadeDetachedEntity(SessionFactoryScope scope) {
		scope.inTransaction(
			session -> {
				assertThrows(IllegalArgumentException.class,
							() -> session.refresh( batch ),
							"Given entity is not associated with the persistence context"
				);
				batch.jobs.forEach( job -> assertEquals( 0, job.status ) );
			}
		);
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

	@Entity( name = "Job" )
	@Table( name = "T_JOB" )
	static class Job {

		@Id @GeneratedValue
		Long id;

		@ManyToOne( fetch = FetchType.LAZY )
		JobBatch batch;

		@Column( name = "PI", nullable = false )
		String processingInstructions;

		@Column( name = "JOB_STATUS", nullable = false )
		int status;

		Job() {}

		Job(JobBatch batch) {
			this.batch = batch;
		}

	}

	@Entity( name = "JobBatch" )
	@Table( name = "T_JOB_BATCH" )
	static class JobBatch {

		@Id @GeneratedValue
		Long id;

		@Column( nullable = false )
		@Temporal( TemporalType.TIMESTAMP )
		Date batchDate;

		@OneToMany( mappedBy = "batch", fetch = FetchType.LAZY, cascade = CascadeType.ALL )
		@Fetch( FetchMode.SELECT )
		Set<Job> jobs = new HashSet<>();

		JobBatch() {}

		JobBatch(Date batchDate) {
			this.batchDate = batchDate;
		}

		Job createJob() {
			Job job = new Job( this );
			jobs.add( job );
			return job;
		}
	}
}
