/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-11996")
@RequiresDialectFeature(DialectChecks.SupportsJdbcDriverProxying.class)
public class InsertOrderingWithMultipleManyToOne extends BaseInsertOrderingTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Parent.class,
			ChildA.class,
			ChildB.class,
		};
	}

	@Test
	public void testBatching() {
		doInHibernate( this::sessionFactory, session -> {
			Parent parent = new Parent();
			session.persist(parent);

			ChildA childA = new ChildA();
			childA.setParent(parent);
			session.persist(childA);

			ChildB childB = new ChildB();
			childB.setParent(parent);
			session.persist(childB);

			clearBatches();
		} );

		verifyPreparedStatementCount( 3 );
		/*
		Map<String, Integer> expectedBatching = new HashMap<>();
		expectedBatching.put( "insert into Address (ID) values (?)", 2 );
		expectedBatching.put( "insert into Person (ID) values (?)", 4 );
		verifyBatching( expectedBatching );
		*/
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		@GeneratedValue
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "ChildA")
	public static class ChildA {
		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		private Parent parent;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "ChildB")
	public static class ChildB {
		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		private Parent parent;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}
}
