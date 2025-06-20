/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batchfetch;

import java.util.List;

import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				SubselectFetchTest.Employee.class,
				SubselectFetchTest.Task.class
		}
)
@SessionFactory
public class SubselectFetchTest {
	private static final int NUMBER_OF_EMPLOYEES = 8;

	@BeforeEach
	public void createData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for ( int i = 0; i < NUMBER_OF_EMPLOYEES; i++ ) {
						Task mainTask = new Task();
						mainTask.id = 100 + i;
						session.persist( mainTask );
						Task task = new Task();
						task.id = i;
						task.parentTask = mainTask;
						session.persist( task );
						Employee e = new Employee( "employee0" + i );
						e.task = task;
						session.persist( e );
					}
				}
		);
	}

	@AfterEach
	public void deleteData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey( value = "HHH-15202")
	public void testSubselectFetchOnlyCreatedIfEnabled(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityPersister employeePersister = session.getSessionFactory()
							.getRuntimeMetamodels()
							.getMappingMetamodel()
							.getEntityDescriptor( Employee.class );
					EntityPersister taskPersister = session.getSessionFactory()
							.getRuntimeMetamodels()
							.getMappingMetamodel()
							.getEntityDescriptor( Task.class );
					BatchFetchQueue batchFetchQueue = session.getPersistenceContextInternal().getBatchFetchQueue();
					List<Employee> results = session.createQuery( "from Employee e order by e.id", Employee.class ).getResultList();
					for ( Employee result : results ) {
						assertThat( batchFetchQueue.getSubselect( session.generateEntityKey( result.name, employeePersister ) ) ).isNull();
						Task task = session.createQuery( "from Task t where t.employee = :e", Task.class )
								.setParameter( "e", result )
								.getSingleResult();
						assertThat( batchFetchQueue.getSubselect( session.generateEntityKey( task.id, taskPersister ) ) ).isNull();
					}
				}
		);
	}

	@Entity(name = "Employee")
	public static class Employee {
		@Id
		private String name;

		@OneToOne(fetch = FetchType.LAZY)
		private Task task;

		private Employee() {
		}

		private Employee(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Task")
	public static class Task {
		@Id
		private long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Task parentTask;

		@OneToOne(fetch = FetchType.LAZY, mappedBy = "task")
		private Employee employee;

		public Task() {
		}
	}

}
