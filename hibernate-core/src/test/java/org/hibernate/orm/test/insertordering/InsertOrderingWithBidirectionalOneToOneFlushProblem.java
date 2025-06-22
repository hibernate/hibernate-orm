/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;


@JiraKey(value = "HHH-12105")
public class InsertOrderingWithBidirectionalOneToOneFlushProblem extends BaseInsertOrderingTest {

	@Test
	public void testInsertSortingWithFlushPersistLeftBeforeRight() {
		sessionFactoryScope().inTransaction(
				session -> {
					TopEntity top1 = new TopEntity();

					session.persist( top1 );
					clearBatches();
					session.flush();

					verifyContainsBatches( new Batch( "insert into TopEntity (name,id) values (?,?)" ) );

					LeftEntity left = new LeftEntity();
					RightEntity right = new RightEntity();
					TopEntity top2 = new TopEntity();

					top1.lefts.add( left );
					left.top = top1;
					top1.rights.add( right );
					right.top = top1;

					// This one-to-one triggers the problem
					right.left = left;

					// If you persist right before left the problem goes away
					session.persist( left );
					session.persist( right );
					session.persist( top2 );

					clearBatches();
				}
		);

		verifyContainsBatches(
				new Batch( "insert into TopEntity (name,id) values (?,?)" ),
				new Batch( "insert into LeftEntity (name,top_id,id) values (?,?,?)" ),
				new Batch( "insert into RightEntity (left_id,name,top_id,id) values (?,?,?,?)" )
		);
	}

	@Test
	public void testInsertSortingWithFlushPersistRightBeforeLeft() {
		sessionFactoryScope().inTransaction(
				session -> {
					TopEntity top1 = new TopEntity();

					session.persist( top1 );
					clearBatches();
					session.flush();

					verifyContainsBatches( new Batch( "insert into TopEntity (name,id) values (?,?)" ) );

					LeftEntity left = new LeftEntity();
					RightEntity right = new RightEntity();
					TopEntity top2 = new TopEntity();

					top1.lefts.add( left );
					left.top = top1;
					top1.rights.add( right );
					right.top = top1;

					// This one-to-one triggers the problem
					right.left = left;

					// If you persist right before left the problem goes away
					session.persist( right );
					session.persist( left );
					session.persist( top2 );

					clearBatches();
				}
		);

		verifyContainsBatches(
				new Batch( "insert into TopEntity (name,id) values (?,?)" ),
				new Batch( "insert into LeftEntity (name,top_id,id) values (?,?,?)" ),
				new Batch( "insert into RightEntity (left_id,name,top_id,id) values (?,?,?,?)" )
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				LeftEntity.class, RightEntity.class, TopEntity.class,
		};
	}

	@Entity(name = "LeftEntity")
	public static class LeftEntity {
		@GeneratedValue
		@Id
		private Long id;

		private String name;

		@ManyToOne
		private TopEntity top;
	}

	@Entity(name = "RightEntity")
	public static class RightEntity {
		@GeneratedValue
		@Id
		private Long id;

		private String name;

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

		private String name;

		@OneToMany(mappedBy = "top")
		private List<RightEntity> rights = new ArrayList<>();

		@OneToMany(mappedBy = "top")
		private List<LeftEntity> lefts = new ArrayList<>();
	}
}
