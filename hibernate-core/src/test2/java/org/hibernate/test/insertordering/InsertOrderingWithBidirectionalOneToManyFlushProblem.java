/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.GenerationType.SEQUENCE;
import static org.hibernate.cfg.AvailableSettings.ORDER_INSERTS;
import static org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

@TestForIssue(jiraKey = "HHH-12074")
public class InsertOrderingWithBidirectionalOneToManyFlushProblem
		extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testBatchingWithFlush() {
		doInHibernate(
			this::sessionFactory,
			session -> {
				TopEntity top1 = new TopEntity();

				session.persist( top1 );

				// InsertActionSorter#sort is invoked during this flush.
				//
				// input: [top1]
				// output: [top1]
				session.flush();

				MiddleEntity middle1 = new MiddleEntity();

				middle1.addBottom( new BottomEntity() );
				top1.addMiddle( middle1 );
				session.persist( middle1 );

				TopEntity top2 = new TopEntity();

				session.persist( top2 );

				MiddleEntity middle2 = new MiddleEntity();

				middle2.addBottom( new BottomEntity() );
				top2.addMiddle( middle2 );
				session.persist( middle2 );

				// InsertActionSorter#sort is invoked during this flush
				//
				// input: [middle1,bottom1,top2,middle2,bottom2] output:
				// [middle1,middle2,bottom1,bottom2,top2]
				//
				// This ordering causes a constraint violation during the flush
				// when the attempt to insert middle2 before top2 is made.
				//
				// correct ordering is: [top2,middle1,middle2,bottom1,bottom2]
			}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12086")
	public void testBatchingWithFlush2() {
		doInHibernate(
			this::sessionFactory,
			session -> {
				TopEntity top1 = new TopEntity();

				session.persist( top1 );

				// InsertActionSorter#sort is invoked during this flush.
				//
				// input: [top1]
				// output: [top1]
				session.flush();

				MiddleEntity middle1 = new MiddleEntity();

				middle1.addBottom( new BottomEntity() );
				middle1.addBottom2( new BottomEntity2() );
				top1.addMiddle( middle1 );
				session.persist( middle1 );

				TopEntity top2 = new TopEntity();

				session.persist( top2 );

				MiddleEntity middle2 = new MiddleEntity();

				middle2.addBottom( new BottomEntity() );
				middle2.addBottom2( new BottomEntity2() );
				top2.addMiddle( middle2 );
				session.persist( middle2 );

				session.persist(new TopEntity());

				// InsertActionSorter#sort is invoked during this flush
				//
				// input: [middle1,bottom1,top2,middle2,bottom2] output:
				// [middle1,middle2,bottom1,bottom2,top2]
				//
				// This ordering causes a constraint violation during the flush
				// when the attempt to insert middle2 before top2 is made.
				//
				// correct ordering is: [top2,middle1,middle2,bottom1,bottom2]
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
		return new Class[] { TopEntity.class, MiddleEntity.class, BottomEntity.class, BottomEntity2.class };
	}

	@Entity(name = "BottomEntity")
	public static class BottomEntity {

		@Column(nullable = false)
		@GeneratedValue(
				strategy = SEQUENCE,
				generator = "ID"
		)
		@Id
		@SequenceGenerator(
				name = "ID",
				sequenceName = "BOTTOM_SEQ"
		)
		private Long id;

		@ManyToOne(optional = false)
		private MiddleEntity middle;
	}

	@Entity(name = "BottomEntity2")
	public static class BottomEntity2 {

		@Column(nullable = false)
		@GeneratedValue(
				strategy = SEQUENCE,
				generator = "ID_2"
		)
		@Id
		@SequenceGenerator(
				name = "ID_2",
				sequenceName = "BOTTOM2_SEQ"
		)
		private Long id;

		@ManyToOne(optional = false)
		private MiddleEntity middle;
	}

	@Entity(name = "MiddleEntity")
	public static class MiddleEntity {

		@Column(nullable = false)
		@GeneratedValue(
				strategy = SEQUENCE,
				generator = "ID_3"
		)
		@Id
		@SequenceGenerator(
				name = "ID_3",
				sequenceName = "MIDDLE_SEQ"
		)
		private Long id;

		@ManyToOne(optional = false)
		private TopEntity top;

		@OneToMany(
				cascade = PERSIST,
				mappedBy = "middle"
		)
		private List<BottomEntity> bottoms = new ArrayList<>();

		@OneToMany(
				cascade = PERSIST,
				mappedBy = "middle"
		)
		private List<BottomEntity2> bottom2s = new ArrayList<>();

		private void addBottom(BottomEntity bottom) {
			bottoms.add( bottom );
			bottom.middle = this;
		}

		private void addBottom2(BottomEntity2 bottom2) {
			bottom2s.add( bottom2 );
			bottom2.middle = this;
		}
	}

	@Entity(name = "TopEntity")
	public static class TopEntity {

		@Column(nullable = false)
		@GeneratedValue(
				strategy = SEQUENCE,
				generator = "ID_4"
		)
		@Id
		@SequenceGenerator(
				name = "ID_4",
				sequenceName = "TOP_SEQ"
		)
		private Long id;

		@OneToMany(mappedBy = "top")
		private List<MiddleEntity> middles = new ArrayList<>();

		void addMiddle(MiddleEntity middle) {
			middles.add( middle );
			middle.top = this;
		}
	}
}
