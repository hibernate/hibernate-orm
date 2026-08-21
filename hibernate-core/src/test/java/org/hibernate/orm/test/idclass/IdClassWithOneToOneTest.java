/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idclass;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@DomainModel(
		annotatedClasses = {
				IdClassWithOneToOneTest.Task.class,
				IdClassWithOneToOneTest.TaskText.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
public class IdClassWithOneToOneTest {

	@BeforeEach
	void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Task task = new Task(new TaskText("en", "Localized in en"));
					session.persist( task );
				}
		);
	}

	@Test
	public void testCreate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Task task = session.get(Task.class, 1L);
					assertThat( task.text.content, is( "Localized in en" ) );
					assertThat( task.text.locale, is( "en" ) );
				}
		);
	}

	@Entity
	public static class Task {

		@Id
		@GeneratedValue
		public Long id;

		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "task")
		public TaskText text;

		public Task() {}

		public Task(TaskText text) {
			this.text = text;
			text.task = this;
		}
	}

	@Entity
	@IdClass(TaskText.TaskTextPK.class)
	public static class TaskText {

		public static class TaskTextPK implements Serializable {
			public Long task;
			public String locale;

			public TaskTextPK() {}

			public TaskTextPK(Long task, String locale) {
				this.task = task;
				this.locale = locale;
			}

			@Override
			public boolean equals(Object obj) {
				if (!(obj instanceof TaskTextPK)) {
					return false;
				} else {
					TaskTextPK pk = (TaskTextPK) obj;
					return Objects.equals(task, pk.task) && locale.equals(pk.locale);
				}
			}

			@Override
			public int hashCode() {
				return task.hashCode() + locale.hashCode();
			}
		}

		public TaskText() {}

		public TaskText(String locale, String content) {
			this.locale = locale;
			this.content = content;
		}

		@Id
		@OneToOne(fetch = FetchType.EAGER)
		@OnDelete(action = OnDeleteAction.CASCADE)
		@JoinColumn(name = "id")
		public Task task;

		@Id
		public String locale;

		public String content;
	}
}
