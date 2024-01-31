package org.hibernate.orm.test.orderby;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				OrderByToOneTest.Task.class,
				OrderByToOneTest.TaskVersion.class,
				OrderByToOneTest.User.class
		}
)
@SessionFactory
public class OrderByToOneTest {

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					User u1 = new User( 1L, "u1" );
					session.persist( u1 );
					User u2 = new User( 2L, "u2" );
					session.persist( u2 );

					Task t = new Task();
					t.setId( 1L );
					TaskVersion tv1 = new TaskVersion();
					tv1.setName( "tv1" );
					tv1.setAssignee( u2 );
					List<TaskVersion> versions = new ArrayList<>();
					versions.add( tv1 );
					t.setTaskVersions( versions );
					tv1.setTask( t );

					TaskVersion tv2 = new TaskVersion();
					tv2.setName( "tv2" );
					tv2.setAssignee( u1 );
					t.getTaskVersions().add( tv2 );
					tv2.setTask( t );
					session.persist( t );
				}
		);
	}

	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from TaskVersion" ).executeUpdate();
					session.createMutationQuery( "delete from UUser" ).executeUpdate();
					session.createMutationQuery( "delete from Task" ).executeUpdate();
				}
		);
	}

	@Test
	@JiraKey("HHH-17623")
	public void testOrderByToOne(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Task task = session.createQuery( "from Task t", Task.class ).getSingleResult();
					final List<TaskVersion> taskVersions = task.getTaskVersions();
					assertEquals( 2, taskVersions.size() );
					assertEquals( "tv2", taskVersions.get( 0 ).getName() );
					assertEquals( "tv1", taskVersions.get( 1 ).getName() );
				}
		);
	}

	@Entity(name = "Task")
	public static class Task {

		@Id
		private Long id;

		@OneToMany(mappedBy = "task", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		@OrderBy("assignee ASC")
		private List<TaskVersion> taskVersions;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<TaskVersion> getTaskVersions() {
			return taskVersions;
		}

		public void setTaskVersions(List<TaskVersion> taskVersions) {
			this.taskVersions = taskVersions;
		}
	}

	@Entity(name = "TaskVersion")
	public static class TaskVersion {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "assignee", nullable = true)
		private User assignee;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "task_id", nullable = false)
		private Task task;

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Task getTask() {
			return task;
		}

		public void setTask(Task task) {
			this.task = task;
		}

		public User getAssignee() {
			return assignee;
		}

		public void setAssignee(User assignee) {
			this.assignee = assignee;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "UUser")
	public static class User {

		@Id
		private Long id;

		private String name;

		public User() {
		}

		public User(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
