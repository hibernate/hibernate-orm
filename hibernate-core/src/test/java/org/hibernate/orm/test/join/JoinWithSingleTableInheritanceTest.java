/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.join;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Jan Schatteman
 */
@DomainModel(
		annotatedClasses = {
				JoinWithSingleTableInheritanceTest.AbstractSuperClass.class,
				JoinWithSingleTableInheritanceTest.ChildEntityA.class,
				JoinWithSingleTableInheritanceTest.ChildEntityB.class,
				JoinWithSingleTableInheritanceTest.RootOne.class,
				JoinWithSingleTableInheritanceTest.MyEntity1.class,
				JoinWithSingleTableInheritanceTest.MyEntity2.class,
				JoinWithSingleTableInheritanceTest.MyEntity3.class,
				JoinWithSingleTableInheritanceTest.MySubEntity2.class
		}
)
@SessionFactory
public class JoinWithSingleTableInheritanceTest {

	@AfterEach
	public void cleanup( SessionFactoryScope scope ) {
		scope.inTransaction(
				s -> {
					s.createMutationQuery( "delete from RootOne" ).executeUpdate();
					s.createMutationQuery( "delete from AbstractSuperClass" ).executeUpdate();
					s.createMutationQuery( "delete from MyEntity1" ).executeUpdate();
					s.createMutationQuery( "delete from MyEntity2" ).executeUpdate();
					s.createMutationQuery( "delete from MyEntity3" ).executeUpdate();
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-16438")
	public void testLeftJoinOnSingleTableInheritance(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> s.persist( new RootOne(1) )
		);

		scope.inTransaction(
				s -> {
					List<RootOne> l = s.createSelectionQuery( "select r from RootOne r left join ChildEntityA ce on ce.id = r.someOtherId", RootOne.class ).list();
					assertEquals( 1, l.size() );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-16438")
	public void testLeftJoinOnSingleTableInheritance2(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					s.persist(new ChildEntityA( 11 ));
					s.persist(new ChildEntityB( 21 ));
					RootOne r1 = new RootOne(1);
					r1.setSomeOtherId( 11 );
					s.persist( r1 );
					RootOne r2 = new RootOne(2);
					r2.setSomeOtherId( 21 );
					s.persist( r2 );
				}
		);

		scope.inTransaction(
				s -> {
					List<Object[]> l = s.createSelectionQuery( "select r.id, r.someOtherId, ce.id, ce.disc_col from RootOne r left join ChildEntityA ce on ce.id = r.someOtherId order by r.id", Object[].class ).list();
					assertEquals( 2, l.size() );
					Object[] r1 = l.get( 0 );
					assertEquals( 1, (int) r1[0] );
					assertEquals( 11, (int) r1[1] );
					assertEquals( 11, (int) r1[2] );
					assertEquals( 1, (int) r1[3] );
					// r2 has to be there, but shouldn't have any data w/ respect to ChildEntityB
					Object[] r2 = l.get( 1 );
					assertEquals( 2, (int) r2[0] );
					assertEquals( 21, (int) r2[1] );
					assertNull( r2[2] );
					assertNull( r2[3] );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-16438, HHH-16494")
	@Disabled(value = "HHH-16494")
	public void testRightJoinOnSingleTableInheritance(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					s.persist(new ChildEntityA( 11 ));
					s.persist(new ChildEntityA( 12 ));
					s.persist(new ChildEntityB( 21 ));
					RootOne r1 = new RootOne(1);
					r1.setSomeOtherId( 11 );
					s.persist( r1 );
					RootOne r2 = new RootOne(2);
					r2.setSomeOtherId( 11 );
					s.persist( r2 );
					RootOne r3 = new RootOne(3);
					r3.setSomeOtherId( 21 );
					s.persist( r3 );
				}
		);

		scope.inTransaction(
				s -> {
					List<Object[]> l = s.createSelectionQuery( "select r.id, r.someOtherId, ce.id, ce.disc_col from RootOne r right join ChildEntityA ce on ce.id = r.someOtherId order by ce.id, r.id", Object[].class ).list();
					assertEquals( 3, l.size() );
					Object[] r1 = l.get( 0 );
					assertEquals( 1, (int) r1[0] );
					assertEquals( 11, (int) r1[1] );
					assertEquals( 11, (int) r1[2] );
					assertEquals( 1, (int) r1[3] );
					Object[] r2 = l.get( 1 );
					assertEquals( 2, (int) r2[0] );
					assertEquals( 11, (int) r2[1] );
					assertEquals( 11, (int) r2[2] );
					assertEquals( 1, (int) r2[3] );
					Object[] r3 = l.get( 2 );
					assertNull( r3[0] );
					assertNull( r3[1] );
					assertEquals( 12, (int) r3[2] );
					assertEquals( 1, (int) r3[3] );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-16438, HHH-16494")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsFullJoin.class)
	@Disabled(value = "HHH-16494")
	public void testFullJoinOnSingleTableInheritance(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					s.persist(new ChildEntityA( 11 ));
					s.persist(new ChildEntityA( 12 ));
					s.persist(new ChildEntityB( 21 ));
					s.persist(new ChildEntityB( 22 ));
					RootOne r1 = new RootOne(1);
					r1.setSomeOtherId( 11 );
					s.persist( r1 );
					RootOne r2 = new RootOne(2);
					r2.setSomeOtherId( 11 );
					s.persist( r2 );
					RootOne r3 = new RootOne(3);
					r3.setSomeOtherId( 21 );
					s.persist( r3 );
					RootOne r4 = new RootOne(4);
					s.persist( r4 );
				}
		);

		scope.inTransaction(
				s -> {
					List<Object[]> l = s.createSelectionQuery( "select r.id, r.someOtherId from RootOne r full join ChildEntityA ce on ce.id = r.someOtherId order by ce.id, r.id", Object[].class ).list();
					assertEquals( 7, l.size() );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-16472")
	@FailureExpected(reason = "Needs fix for HHH-16472")
	public void testTreatedLeftJoinWithRestrictionOnEntity( SessionFactoryScope scope ) {
		scope.inTransaction(
				s -> {
					MyEntity3 entity3 = new MyEntity3();
					s.persist(entity3);

					MyEntity2 entity2 = new MyEntity2();
					s.persist(entity2);

					MySubEntity2 subentity2 = new MySubEntity2();
					subentity2.setRef3(entity3);
					s.persist(subentity2);

					MyEntity1 entity1a = new MyEntity1();
					s.persist(entity1a);

					MyEntity1 entity1b = new MyEntity1();
					entity1b.setRef2(subentity2);
					s.persist(entity1b);

					s.flush();

					CriteriaBuilder builder = s.getCriteriaBuilder();

					CriteriaQuery<MyEntity1> criteria = builder.createQuery( MyEntity1.class);
					Root<MyEntity1> root = criteria.from( MyEntity1.class);
					criteria.select(root)
							.where(
									builder.or(
											root.get("ref2").isNull(),
											builder.and(
													root.get("ref2").isNotNull(),
													builder.equal(
															builder.treat(
																	root.join( "ref2", JoinType.LEFT), MySubEntity2.class).get( "ref3"),
															entity3)
											)
									)
							);
					assertEquals(2, s.createQuery(criteria).getResultList().size());
				}
		);
	}

	@Entity(name = "AbstractSuperClass")
	@DiscriminatorColumn(name = "disc_col", discriminatorType = DiscriminatorType.INTEGER)
	public static abstract class AbstractSuperClass {
		@Id
		private Integer id;

		@Column(insertable = false, updatable = false)
		private Integer disc_col;

		public AbstractSuperClass(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "ChildEntityA")
	@DiscriminatorValue("1")
	public static class ChildEntityA extends AbstractSuperClass {
		public ChildEntityA(Integer id) {
			super( id );
		}
	}

	@Entity(name = "ChildEntityB")
	@DiscriminatorValue("2")
	public static class ChildEntityB extends AbstractSuperClass {
		public ChildEntityB(Integer id) {
			super( id );
		}
	}

	@Entity(name = "RootOne")
	public static class RootOne {
		@Id
		Integer id;
		Integer someOtherId;

		public RootOne() {
		}

		public RootOne(Integer id) {
			this.id = id;
		}

		public Integer getSomeOtherId() {
			return someOtherId;
		}

		public void setSomeOtherId(Integer someOtherId) {
			this.someOtherId = someOtherId;
		}
	}

	@Entity(name = "MyEntity1")
	public static class MyEntity1 {
		@Id()
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@ManyToOne
		@JoinColumn(name = "ref2", nullable = true)
		private MyEntity2 ref2;

		public MyEntity1() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public MyEntity2 getRef2() {
			return ref2;
		}

		public void setRef2(MyEntity2 ref2) {
			this.ref2 = ref2;
		}
	}

	@Entity(name = "MyEntity2")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class MyEntity2 {
		@Id()
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		public MyEntity2() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "MyEntity3")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class MyEntity3 {
		@Id()
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		public MyEntity3() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "MySubEntity2")
	public static class MySubEntity2 extends MyEntity2 {
		@ManyToOne
		@JoinColumn(name = "ref3")
		private MyEntity3 ref3;

		public MySubEntity2() {
		}

		public MyEntity3 getRef3() {
			return ref3;
		}

		public void setRef3(MyEntity3 ref3) {
			this.ref3 = ref3;
		}
	}

}
