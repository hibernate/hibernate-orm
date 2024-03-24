package org.hibernate.orm.test.query.hql.joinorder;

import jakarta.persistence.*;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@SessionFactory
@DomainModel(annotatedClasses = {JoinOrderTest.Task.class, JoinOrderTest.Location.class, JoinOrderTest.TaskLocation.class, JoinOrderTest.Vineyard.class, JoinOrderTest.TaskEventLog.class})
@TestForIssue(jiraKey="HHH-16522")
public class JoinOrderTest {

	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction( s-> {
			s.createQuery(
					"select distinct t from Task t join TaskEventLog tel on tel.task.id = t.id join Vineyard v on t.taskLocation.vineyard.id = v.id",
					Task.class).getResultList();
		});
	}

	@Entity(name = "Task")
	@Table(name = "vin_task")
	public class Task {
		@Id
		@GeneratedValue Long id;

		@OneToOne(cascade = CascadeType.PERSIST, orphanRemoval = true)
		@JoinColumn(name = "task_location_id", referencedColumnName = "id")
		private TaskLocation taskLocation;

	}

	@Entity(name = "TaskLocation")
	public class TaskLocation extends Location {
		@Id @GeneratedValue Long id;

		@OneToOne(mappedBy = "taskLocation")
		private Task task;

	}

	@Entity(name = "Location")
	@Table(name = "vin_location")
	public class Location {
		@Id @GeneratedValue Long id;

		@ManyToOne
		@JoinColumn(name = "vineyard_id")
		private Vineyard vineyard;
	}

	@Entity(name = "Vineyard")
	@Table(name = "vin_vineyard")
	public class Vineyard {
		@Id @GeneratedValue Long id;
	}

	@Entity(name = "TaskEventLog")
	@Table(name = "vin_task_event_log")
	public class TaskEventLog {
		@Id @GeneratedValue
		@JoinColumn(name = "task_id")
		Long id;

		@ManyToOne Task task;
	}

}