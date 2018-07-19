/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11216")
public class ElementCollectionTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {Task.class};
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( Environment.ORDER_INSERTS, "true" );
		settings.put( Environment.STATEMENT_BATCH_SIZE, "10" );
		}

	@Test
	public void testBatchOrdering() {
		doInHibernate( this::sessionFactory, session -> {
			Task task = new Task();
			task.addCategory(Category.A);
			session.persist( task );

			Task task1 = new Task();
			task1.addCategory(Category.A);
			session.persist( task1 );
		} );
	}

	@Entity
	@Table(name = "TASK")
	public static class Task {

		@Id
		@SequenceGenerator(name = "ID", sequenceName = "TASK_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID")
		private int id;

		@ElementCollection(targetClass = Category.class)
		@CollectionTable(name = "TASK_CATEGORY", joinColumns = {@JoinColumn(name = "TASK_ID")})
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
