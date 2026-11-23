package org.hibernate.temporal.audit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
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

import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@Jira("https://hibernate.atlassian.net/browse/HHH-21228")
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditMappedByIndexTest.JobQueue.class, AuditMappedByIndexTest.Job.class, AuditMappedByIndexTest.Task.class
})
@ServiceRegistry(settings = @Setting(name = CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditOneToManyIndexTest$ChangesetIds"))
class AuditMappedByIndexTest {
	@Test
	void readIndicesAtChangeset(SessionFactoryScope scope) {
		final var first = scope.fromTransaction( session -> {
			final var queue = new JobQueue();
			session.persist( queue );
			for ( int i = 1; i <= 2; i++ ) {
				final var job = new Job();
				job.id = i;
				job.queue = queue;
				final var task = new Task();
				task.id = i;
				task.queue = queue;
				queue.jobs.add( job );
				queue.tasks.put( "task" + i, task );
				session.persist( task );
				session.persist( job );
			}
			return session.getCurrentChangesetIdentifier();
		} );
		final var second = scope.fromTransaction( session -> {
			final var queue = session.find( JobQueue.class, 1 );
			queue.jobs.add( queue.jobs.remove( 0 ) );
			queue.tasks.put( "temporary", queue.tasks.remove( "task1" ) );
			session.flush();
			queue.tasks.put( "renamed", queue.tasks.remove( "temporary" ) );
			return session.getCurrentChangesetIdentifier();
		} );
		final var third = scope.fromTransaction( session -> {
			session.find( Job.class, 1 ).description = "updated";
			session.find( Task.class, 1 ).description = "updated";
			return session.getCurrentChangesetIdentifier();
		} );

		final var fourth = scope.fromTransaction( session -> {
			final var queue = session.find( JobQueue.class, 1 );
			queue.jobs = new ArrayList<>( List.of( session.find( Job.class, 1 ), session.find( Job.class, 2 ) ) );
			return session.getCurrentChangesetIdentifier();
		} );

		checkIndices( scope, first, List.of( 1, 2 ), "task1", "renamed" );
		checkIndices( scope, second, List.of( 2, 1 ), "renamed", "task1" );
		checkIndices( scope, third, List.of( 2, 1 ), "renamed", "task1" );
		checkIndices( scope, fourth, List.of( 1, 2 ), "renamed", "task1" );
		try (var log = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertEquals( List.of( first, second, third ), log.getChangesets( Task.class, 1 ) );
			assertEquals( List.of( first ), log.getChangesets( Task.class, 2 ) );
		}
	}

	private static void checkIndices(SessionFactoryScope scope, Object changeset, List<Integer> order,
			String firstKey, String missingKey) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			final var queue = session.find( JobQueue.class, 1 );
			assertSame( session.find( Job.class, order.get( 0 ) ), Hibernate.get( queue.jobs, 0 ) );
			assertSame( session.find( Task.class, 1 ), Hibernate.get( queue.tasks, firstKey ) );
			assertNull( Hibernate.get( queue.tasks, missingKey ) );
			assertFalse( Hibernate.isInitialized( queue.jobs ) );
			assertFalse( Hibernate.isInitialized( queue.tasks ) );
			assertEquals( order, queue.jobs.stream().map( job -> job.id ).toList() );
			assertEquals( Map.of( firstKey, session.find( Task.class, 1 ), "task2", session.find( Task.class, 2 ) ),
					queue.tasks );
		}
	}

	@Entity(name = "JobQueue")
	@Audited
	static class JobQueue {
		@Id
		int id = 1;
		@OneToMany(mappedBy = "queue")
		@OrderColumn(name = "job_index")
		List<Job> jobs = new ArrayList<>();
		@OneToMany(mappedBy = "queue")
		@MapKeyColumn(name = "job_key", nullable = true)
		Map<String, Task> tasks = new HashMap<>();
	}

	@Entity(name = "Job")
	@Audited
	static class Job {
		@Id
		int id;
		@ManyToOne
		JobQueue queue;
		String description;
	}

	@Entity(name = "Task")
	@Audited
	static class Task {
		@Id
		int id;
		@ManyToOne
		JobQueue queue;
		String description;
	}
}
