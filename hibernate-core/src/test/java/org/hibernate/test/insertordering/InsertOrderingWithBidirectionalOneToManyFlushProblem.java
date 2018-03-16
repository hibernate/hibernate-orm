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

import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.GenerationType.SEQUENCE;
import static org.hibernate.cfg.AvailableSettings.ORDER_INSERTS;
import static org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE;

@TestForIssue(jiraKey = "HHH-12074")
public class InsertOrderingWithBidirectionalOneToManyFlushProblem
		extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testBatchingWithFlush() {
		Session session = openSession();
		session.getTransaction().begin();
		{
				Top top1 = new Top();

				session.persist( top1 );

				// InsertActionSorter#sort is invoked during this flush.
				//
				// input: [top1]
				// output: [top1]
				session.flush();

				Middle middle1 = new Middle();

				middle1.addBottom( new Bottom() );
				top1.addMiddle( middle1 );
				session.persist( middle1 );

				Top top2 = new Top();

				session.persist( top2 );

				Middle middle2 = new Middle();

				middle2.addBottom( new Bottom() );
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
		session.getTransaction().commit();
		session.close();
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( ORDER_INSERTS, "true" );
		settings.put( STATEMENT_BATCH_SIZE, "10" );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Top.class, Middle.class, Bottom.class };
	}

	@Entity(name = "Bottom")
	public static class Bottom {

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
		private Middle middle;
	}

	@Entity(name = "Middle")
	public static class Middle {

		@Column(nullable = false)
		@GeneratedValue(
				strategy = SEQUENCE,
				generator = "ID"
		)
		@Id
		@SequenceGenerator(
				name = "ID",
				sequenceName = "MIDDLE_SEQ"
		)
		private Long id;

		@ManyToOne(optional = false)
		private Top top;

		@OneToMany(
				cascade = PERSIST,
				mappedBy = "middle"
		)
		private List<Bottom> bottoms = new ArrayList<>();

		private void addBottom(Bottom bottom) {
			bottoms.add( bottom );
			bottom.middle = this;
		}
	}

	@Entity(name = "Top")
	public static class Top {

		@Column(nullable = false)
		@GeneratedValue(
				strategy = SEQUENCE,
				generator = "ID"
		)
		@Id
		@SequenceGenerator(
				name = "ID",
				sequenceName = "TOP_SEQ"
		)
		private Long id;

		@OneToMany(mappedBy = "top")
		private List<Middle> middles = new ArrayList<>();

		void addMiddle(Middle middle) {
			middles.add( middle );
			middle.top = this;
		}
	}
}
