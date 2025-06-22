/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.GenerationType.SEQUENCE;

@JiraKey(value = "HHH-12074")
public class InsertOrderingWithBidirectionalOneToManyFlushProblem extends BaseInsertOrderingTest {

	@Test
	public void testBatchingWithFlush() {
		sessionFactoryScope().inTransaction(
				session -> {
					TopEntity top1 = new TopEntity();

					session.persist( top1 );

					clearBatches();

					// InsertActionSorter#sort is invoked during this flush.
					//
					// input: [top1]
					// output: [top1]
					session.flush();

					verifyContainsBatches( new Batch( "insert into TopEntity (name,id) values (?,?)" ) );

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

					clearBatches();
				}
		);

		verifyContainsBatches(
				new Batch( "insert into TopEntity (name,id) values (?,?)" ),
				new Batch( "insert into MiddleEntity (name,top_id,id) values (?,?,?)", 2 ),
				new Batch( "insert into BottomEntity (middle_id,name,id) values (?,?,?)", 2 )
		);
	}

	@Test
	@JiraKey(value = "HHH-12086")
	public void testBatchingWithFlush2() {
		sessionFactoryScope().inTransaction(
				session -> {
					TopEntity top1 = new TopEntity();

					session.persist( top1 );

					// InsertActionSorter#sort is invoked during this flush.
					//
					// input: [top1]
					// output: [top1]
					clearBatches();
					session.flush();

					verifyContainsBatches( new Batch( "insert into TopEntity (name,id) values (?,?)" ) );

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

					session.persist( new TopEntity() );

					// InsertActionSorter#sort is invoked during this flush
					//
					// input: [middle1,bottom1,top2,middle2,bottom2] output:
					// [middle1,middle2,bottom1,bottom2,top2]
					//
					// This ordering causes a constraint violation during the flush
					// when the attempt to insert middle2 before top2 is made.
					//
					// correct ordering is: [top2,middle1,middle2,bottom1,bottom2]
					clearBatches();
				}
		);

		verifyContainsBatches(
				new Batch( "insert into TopEntity (name,id) values (?,?)", 2 ),
				new Batch( "insert into MiddleEntity (name,top_id,id) values (?,?,?)", 2 ),
				new Batch( "insert into BottomEntity (middle_id,name,id) values (?,?,?)", 2 ),
				new Batch( "insert into BottomEntity2 (middle_id,name,id) values (?,?,?)", 2 )
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TopEntity.class, MiddleEntity.class, BottomEntity.class, BottomEntity2.class };
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

		private String name;

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

		private String name;

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

		private String name;

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

		private String name;

		@OneToMany(mappedBy = "top")
		private List<MiddleEntity> middles = new ArrayList<>();

		void addMiddle(MiddleEntity middle) {
			middles.add( middle );
			middle.top = this;
		}
	}
}
