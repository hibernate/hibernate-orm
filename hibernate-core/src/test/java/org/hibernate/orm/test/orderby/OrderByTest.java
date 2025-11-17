/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orderby;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				OrderByTest.Person.class,
				OrderByTest.P1.class,
				OrderByTest.P2.class,
				OrderByTest.Task.class,
				OrderByTest.TaskVersion.class,
				OrderByTest.User.class,
				OrderByTest.Group.class
		}
)
@SessionFactory
public class OrderByTest {

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new P1( 1L, "abc" ) );
					session.persist( new P1( 2L, "abc" ) );
					session.persist( new P2( 3L, "def" ) );

					Group g1 = new Group();
					g1.setName( "g1" );
					Group g2 = new Group();
					g2.setName( "g2" );
					Set<Group> groups = new HashSet();
					groups.add( g1 );
					groups.add( g2 );
					User u = new User();
					u.setGroups( groups );
					session.persist( u );

					Task t = new Task();
					t.setId( 1L );
					TaskVersion tv1 = new TaskVersion();
					tv1.setName("tv1");
					tv1.setAssignee(u);
					List<TaskVersion> versions = new ArrayList<>();
					versions.add( tv1 );
					t.setTaskVersions( versions );
					tv1.setTask(t);

					TaskVersion tv2 = new TaskVersion();
					tv2.setName("tv2");
					tv2.setAssignee(u);
					t.getTaskVersions().add(tv2);
					tv2.setTask(t);
					session.persist( t );
				}
		);
	}

	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-14351")
	public void testOrderBySqlNode(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Person> list = session.createQuery( "from Person p order by type(p) desc, p.id", Person.class )
							.getResultList();
					assertEquals( 3L, list.get( 0 ).getId().longValue() );
					assertEquals( 1L, list.get( 1 ).getId().longValue() );
					assertEquals( 2L, list.get( 2 ).getId().longValue() );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-15885")
	public void testOrderBy(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					session.get(Task.class, 1L);
				}
		);
	}

	@Entity(name = "Person")
	public static abstract class Person {
		@Id
		private Long id;
		private String name;

		public Person() {
		}

		public Person(Long id, String name) {
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

	@Entity(name = "P1")
	@DiscriminatorValue("P1")
	public static class P1 extends Person {
		public P1() {
		}

		public P1(Long id, String name) {
			super( id, name );
		}
	}

	@Entity(name = "P2")
	@DiscriminatorValue("P2")
	public static class P2 extends Person {
		public P2() {
		}

		public P2(Long id, String name) {
			super( id, name );
		}
	}

	@Entity(name = "Task")
	public static class Task {

		@Id
		private Long id;

		@OneToMany(mappedBy = "task", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		@OrderBy("id DESC")
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
		@GeneratedValue
		private Long id;

		@ManyToMany(fetch = FetchType.EAGER, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
		@JoinTable(name = "user_groups", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "group_id"))
		@OrderBy("name")
		private Set<Group> groups;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<Group> getGroups() {
			return groups;
		}

		public void setGroups(Set<Group> groups) {
			this.groups = groups;
		}
	}

	@Entity(name = "GGroup")
	public static class Group {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

}
