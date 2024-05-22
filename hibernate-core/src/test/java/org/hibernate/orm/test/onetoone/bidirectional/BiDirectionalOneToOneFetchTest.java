/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.orm.test.onetoone.bidirectional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import org.hibernate.engine.internal.StatisticalLoggingSessionEventListener;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Test cases for fetch joining a bidirectional one-to-one mapping.
 *
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				BiDirectionalOneToOneFetchTest.EntityA.class,
				BiDirectionalOneToOneFetchTest.EntityB.class,
				BiDirectionalOneToOneFetchTest.EntityC.class
		}
)
@SessionFactory
public class BiDirectionalOneToOneFetchTest {

	@AfterEach
	public void delete(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.createQuery( "delete from EntityA" ).executeUpdate();
			s.createQuery( "delete from EntityB" ).executeUpdate();
			s.createQuery( "delete from EntityC" ).executeUpdate();
		} );
	}

	@Test
	@JiraKey("HHH-3930")
	public void testEagerFetchBidirectionalOneToOneWithDirectFetching(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityA a = new EntityA( 1L, new EntityB( 2L ), new EntityC( 3L ) );

			session.persist( a );
			session.flush();
			session.clear();

			// Use atomic integer because we need something mutable
			final AtomicInteger queryExecutionCount = new AtomicInteger();

			session.getEventListenerManager().addListener( new StatisticalLoggingSessionEventListener() {
				@Override
				public void jdbcExecuteStatementStart() {
					super.jdbcExecuteStatementStart();
					queryExecutionCount.getAndIncrement();
				}
			} );

			session.find( EntityA.class, 1L );

			assertEquals(
					"Join fetching inverse one-to-one didn't use the object already present in the result set!",
					1,
					queryExecutionCount.get()
			);
		} );
	}

	@Test
	@JiraKey("HHH-3930")
	public void testFetchBidirectionalOneToOneWithOneJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityA a = new EntityA( 1L, new EntityB( 2L ), new EntityC( 3L ) );

			session.persist( a );
			session.flush();
			session.clear();

			// Use atomic integer because we need something mutable
			final AtomicInteger queryExecutionCount = new AtomicInteger();

			session.getEventListenerManager().addListener( new StatisticalLoggingSessionEventListener() {
				@Override
				public void jdbcExecuteStatementStart() {
					super.jdbcExecuteStatementStart();
					queryExecutionCount.getAndIncrement();
				}
			} );

			List<EntityA> list = session.createQuery(
					"from EntityA a join fetch a.b"
			).list();

			EntityA entityA = list.get( 0 );
			assertSame( entityA, entityA.getB().getA() );


			assertEquals(
					"Join fetching inverse one-to-one didn't use the object already present in the result set!",
					1,
					queryExecutionCount.get()
			);
		} );
	}

	@Test
	@JiraKey("HHH-3930")
	public void testFetchBidirectionalOneToOneWithCircularJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityA a = new EntityA( 1L, new EntityB( 2L ), new EntityC( 3L ) );

			session.persist( a );
			session.flush();
			session.clear();

			// Use atomic integer because we need something mutable
			final AtomicInteger queryExecutionCount = new AtomicInteger();
			session.getEventListenerManager().addListener( new StatisticalLoggingSessionEventListener() {
				@Override
				public void jdbcExecuteStatementStart() {
					super.jdbcExecuteStatementStart();
					queryExecutionCount.getAndIncrement();
				}
			} );

			session.createQuery(
					"from EntityA a join fetch a.b b join fetch b.a"
			).list();

			assertEquals(
					"Join fetching inverse one-to-one didn't use the object already present in the result set!",
					1,
					queryExecutionCount.get()
			);
		} );
	}

	@Test
	@JiraKey("HHH-12885")
	public void testSelectInverseOneToOne(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA a = new EntityA( 1L, new EntityB( 2L ), new EntityC( 3L ) );

					session.persist( a );
					session.flush();
					session.clear();

					List<Object[]> tupleList = session.createQuery(
							"select b, b.id, a from EntityB b left join b.a a",
							Object[].class
					).list();

					List<EntityA> list = session.createQuery(
							"select a from EntityB b left join b.a a",
							EntityA.class
					).list();

					assertEquals(
							"Selecting inverse one-to-one didn't construct the object properly from the result set!",
							list.get( 0 ),
							tupleList.get( 0 )[2]
					);
				}
		);
	}

	@Test
	@JiraKey("HHH-12885")
	public void testSelectInverseOneToOne2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA a = new EntityA( 1L, new EntityB( 2L ), new EntityC( 3L ) );

					session.persist( a );
					session.flush();
					session.clear();

					List<Object[]> tupleList = session.createQuery(
							"select b, b.id, a, a.id from EntityB b left join b.a a",
							Object[].class
					).list();

					List<EntityA> list = session.createQuery(
							"select a from EntityB b left join b.a a",
							EntityA.class
					).list();

					assertEquals(
							"Selecting inverse one-to-one didn't construct the object properly from the result set!",
							list.get( 0 ),
							tupleList.get( 0 )[2]
					);
				}
		);
	}

	@Test
	@JiraKey("HHH-12885")
	public void testSelectInverseOneToOne3(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA a = new EntityA( 1L, new EntityB( 2L ), new EntityC( 3L ) );

					session.persist( a );
					session.flush();
					session.clear();

					List<Object[]> tupleList = session.createQuery(
							"select b, b.id, a, a.id from EntityB b left join b.a a left join fetch a.c",
							Object[].class
					).list();

					List<EntityA> list = session.createQuery(
							"select a from EntityB b left join b.a a left join fetch a.c",
							EntityA.class
					).list();

					assertEquals(
							"Selecting inverse one-to-one didn't construct the object properly from the result set!",
							list.get( 0 ),
							tupleList.get( 0 )[2]
					);
				}
		);
	}

	@Entity(name = "EntityA")
	public static class EntityA {

		@Id
		private Long id;

		@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		@JoinColumn(name = "b_id")
		private EntityB b;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		@JoinColumn(name = "c_id")
		private EntityC c;

		public EntityA() {
		}

		public EntityA(Long id, EntityB b, EntityC c) {
			this.id = id;
			this.b = b;
			this.b.a = this;
			this.c = c;
		}

		public EntityB getB() {
			return b;
		}

		public void setB(EntityB b) {
			this.b = b;
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB {

		@Id
		private Long id;

		@OneToOne(mappedBy = "b", fetch = FetchType.EAGER)
		private EntityA a;

		public EntityB() {
		}

		public EntityB(Long id) {
			this.id = id;
		}

		public EntityA getA() {
			return a;
		}
	}

	@Entity(name = "EntityC")
	public static class EntityC {

		@Id
		private Long id;

		public EntityC() {
		}

		public EntityC(Long id) {
			this.id = id;
		}
	}

}
