package org.hibernate.temporal.audit;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.InheritanceType.TABLE_PER_CLASS;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The order column of an audited mappedBy one-to-many whose element is a {@code TABLE_PER_CLASS}
 * hierarchy is written to, and audited in, the table of the concrete subclass of each element.
 */
@Jira("https://hibernate.atlassian.net/browse/HHH-21239")
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditMappedByIndexTablePerClassTest.JobQueue.class,
		AuditMappedByIndexTablePerClassTest.Job.class,
		AuditMappedByIndexTablePerClassTest.BuildJob.class,
		AuditMappedByIndexTablePerClassTest.DeployJob.class
})
@ServiceRegistry(settings = @Setting(name = CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditOneToManyIndexTest$ChangesetIds"))
class AuditMappedByIndexTablePerClassTest {
	@Test
	void readIndicesAtChangeset(SessionFactoryScope scope) {
		final var first = createQueue( scope, 1 );
		final var second = scope.fromTransaction( session -> {
			final var queue = session.find( JobQueue.class, 1 );
			queue.jobs.add( queue.jobs.remove( 0 ) );
			return session.getCurrentChangesetIdentifier();
		} );

		checkOrder( scope, first, 1, List.of( 1, 2, 3 ) );
		checkOrder( scope, second, 1, List.of( 2, 3, 1 ) );
		try (var log = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertEquals( List.of( first, second ), log.getChangesets( BuildJob.class, 1 ) );
			assertEquals( List.of( first, second ), log.getChangesets( DeployJob.class, 2 ) );
			assertEquals( List.of( first, second ), log.getChangesets( BuildJob.class, 3 ) );
		}
	}

	@Test
	void appendToUninitializedList(SessionFactoryScope scope) {
		final var first = createQueue( scope, 11 );
		final var added = scope.fromTransaction( session -> {
			final var queue = session.find( JobQueue.class, 11 );
			final var job = new DeployJob();
			job.id = 14;
			job.queue = queue;
			queue.jobs.add( job );
			assertFalse( Hibernate.isInitialized( queue.jobs ) );
			session.persist( job );
			return session.getCurrentChangesetIdentifier();
		} );

		checkOrder( scope, first, 11, List.of( 11, 12, 13 ) );
		checkOrder( scope, added, 11, List.of( 11, 12, 13, 14 ) );
		try (var log = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertEquals( List.of( first ), log.getChangesets( BuildJob.class, 11 ) );
			assertEquals( List.of( first ), log.getChangesets( DeployJob.class, 12 ) );
			assertEquals( List.of( added ), log.getChangesets( DeployJob.class, 14 ) );
		}
	}

	/**
	 * Persists a queue holding a build job, a deploy job and a build job, with consecutive ids.
	 */
	private static Object createQueue(SessionFactoryScope scope, int firstId) {
		return scope.fromTransaction( session -> {
			final var queue = new JobQueue();
			queue.id = firstId;
			session.persist( queue );
			final List<Job> jobs = List.of( new BuildJob(), new DeployJob(), new BuildJob() );
			for ( int i = 0; i < jobs.size(); i++ ) {
				final var job = jobs.get( i );
				job.id = firstId + i;
				job.queue = queue;
				queue.jobs.add( job );
				session.persist( job );
			}
			return session.getCurrentChangesetIdentifier();
		} );
	}

	private static void checkOrder(SessionFactoryScope scope, Object changeset, int queueId, List<Integer> order) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			assertEquals( order, session.find( JobQueue.class, queueId ).jobs.stream().map( job -> job.id ).toList() );
		}
	}

	@Entity(name = "JobQueue")
	@Audited
	static class JobQueue {
		@Id
		int id;
		@OneToMany(mappedBy = "queue")
		@OrderColumn(name = "job_index")
		List<Job> jobs = new ArrayList<>();
	}

	@Entity(name = "Job")
	@Audited
	@Inheritance(strategy = TABLE_PER_CLASS)
	static abstract class Job {
		@Id
		int id;
		@ManyToOne
		JobQueue queue;
	}

	@Entity(name = "BuildJob")
	@Audited
	static class BuildJob extends Job {
	}

	@Entity(name = "DeployJob")
	@Audited
	static class DeployJob extends Job {
	}
}
