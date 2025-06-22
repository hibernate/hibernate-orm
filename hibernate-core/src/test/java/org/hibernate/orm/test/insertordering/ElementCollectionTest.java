/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-11216")
public class ElementCollectionTest extends BaseInsertOrderingTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Task.class };
	}

	@Test
	public void testBatchOrdering() {
		sessionFactoryScope().inTransaction( session -> {
			Task task = new Task();
			task.addCategory( Category.A );
			session.persist( task );

			Task task1 = new Task();
			task1.addCategory( Category.A );
			session.persist( task1 );

			clearBatches();
		} );

		verifyContainsBatches(
				new Batch( "insert into TASK (name,id) values (?,?)", 2 ),
				new Batch( "insert into TASK_CATEGORY (TASK_ID,categories) values (?,?)", 2 )
		);
	}

	@Entity
	@Table(name = "TASK")
	public static class Task {

		@Id
		@SequenceGenerator(name = "ID", sequenceName = "TASK_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID")
		private int id;

		private String name;

		@ElementCollection(targetClass = Category.class)
		@CollectionTable(name = "TASK_CATEGORY", joinColumns = { @JoinColumn(name = "TASK_ID") })
		@Enumerated(EnumType.STRING)
		private final Set<Category> categories = new HashSet<>();

		public void addCategory(Category c) {
			categories.add( c );
		}
	}

	public enum Category {

		A( "CAT1" ), B( "CAT2" );

		private String name;

		Category(final String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
