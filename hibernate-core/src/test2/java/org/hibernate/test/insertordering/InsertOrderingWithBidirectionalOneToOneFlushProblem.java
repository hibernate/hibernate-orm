/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hibernate.cfg.AvailableSettings.ORDER_INSERTS;
import static org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

@TestForIssue(jiraKey = "HHH-12105")
public class InsertOrderingWithBidirectionalOneToOneFlushProblem
		extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testInsertSortingWithFlushPersistLeftBeforeRight() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					TopEntity top1 = new TopEntity();

					session.persist(top1);
					session.flush();

					LeftEntity left = new LeftEntity();
					RightEntity right = new RightEntity();
					TopEntity top2 = new TopEntity();

					top1.lefts.add(left);
					left.top = top1;
					top1.rights.add(right);
					right.top = top1;

					// This one-to-one triggers the problem
					right.left = left;

					// If you persist right before left the problem goes away
					session.persist(left);
					session.persist(right);
					session.persist(top2);
				}
		);
	}

	@Test
	public void testInsertSortingWithFlushPersistRightBeforeLeft() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					TopEntity top1 = new TopEntity();

					session.persist(top1);
					session.flush();

					LeftEntity left = new LeftEntity();
					RightEntity right = new RightEntity();
					TopEntity top2 = new TopEntity();

					top1.lefts.add(left);
					left.top = top1;
					top1.rights.add(right);
					right.top = top1;

					// This one-to-one triggers the problem
					right.left = left;

					// If you persist right before left the problem goes away
					session.persist(right);
					session.persist(left);
					session.persist(top2);
				}
		);
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( ORDER_INSERTS, "true" );
		settings.put( STATEMENT_BATCH_SIZE, "10" );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				LeftEntity.class, RightEntity.class, TopEntity.class,
		};
	}

	@Entity(name = "LeftEntity")
	public static class LeftEntity {
		@GeneratedValue
		@Id
		private Long id;

		@ManyToOne
		private TopEntity top;
	}

	@Entity(name = "RightEntity")
	public static class RightEntity {
		@GeneratedValue
		@Id
		private Long id;

		@ManyToOne
		private TopEntity top;

		@OneToOne
		private LeftEntity left;
	}

	@Entity(name = "TopEntity")
	public static class TopEntity {
		@GeneratedValue
		@Id
		private Long id;

		@OneToMany(mappedBy = "top")
		private List<RightEntity> rights = new ArrayList<>();

		@OneToMany(mappedBy = "top")
		private List<LeftEntity> lefts = new ArrayList<>();
	}
}
