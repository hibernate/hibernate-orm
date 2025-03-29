/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.join;

import java.util.List;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		AttributeJoinWithTablePerClassInheritanceTest.BaseClass.class,
		AttributeJoinWithTablePerClassInheritanceTest.ChildEntityA.class,
		AttributeJoinWithTablePerClassInheritanceTest.SubChildEntityA1.class,
		AttributeJoinWithTablePerClassInheritanceTest.SubChildEntityA2.class,
		AttributeJoinWithTablePerClassInheritanceTest.ChildEntityB.class,
		AttributeJoinWithTablePerClassInheritanceTest.RootOne.class
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-16494" )
public class AttributeJoinWithTablePerClassInheritanceTest {
	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.createMutationQuery( "delete from RootOne" ).executeUpdate();
			s.createMutationQuery( "delete from SubChildEntityA1" ).executeUpdate();
			s.createMutationQuery( "delete from SubChildEntityA2" ).executeUpdate();
			s.createMutationQuery( "delete from BaseClass" ).executeUpdate();
		} );
	}

	@Test
	public void testLeftJoin(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final ChildEntityA childEntityA = new SubChildEntityA1( 11 );
			s.persist( childEntityA );
			final ChildEntityB childEntityB = new ChildEntityB( 21 );
			s.persist( childEntityB );
			s.persist( new RootOne( 1, childEntityA ) );
			s.persist( new RootOne( 2, null ) );
		} );
		scope.inTransaction( s -> {
			// simulate association with ChildEntityB
			s.createNativeMutationQuery( "update root_one set child_id = 21 where id = 2" ).executeUpdate();
		} );
		scope.inTransaction( s -> {
			final List<Tuple> resultList = s.createQuery(
					"select r, ce " +
							"from RootOne r left join r.child ce " +
							"order by r.id",
					Tuple.class
			).getResultList();
			assertEquals( 2, resultList.size() );
			assertResult( resultList.get( 0 ), 1, 11, 11, SubChildEntityA1.class );
			assertResult( resultList.get( 1 ), 2, 21, null, null );
		} );
	}

	@Test
	public void testLeftJoinExplicitTreat(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final ChildEntityA childEntityA = new SubChildEntityA1( 11 );
			s.persist( childEntityA );
			final ChildEntityB childEntityB = new ChildEntityB( 21 );
			s.persist( childEntityB );
			s.persist( new RootOne( 1, childEntityA ) );
			s.persist( new RootOne( 2, null ) );
		} );
		scope.inTransaction( s -> {
			// simulate association with ChildEntityB
			s.createNativeMutationQuery( "update root_one set child_id = 21 where id = 2" ).executeUpdate();
		} );
		scope.inTransaction( s -> {
			final List<Tuple> resultList = s.createQuery(
					"select r, ce " +
							"from RootOne r left join treat(r.child as ChildEntityA) ce " +
							"order by r.id",
					Tuple.class
			).getResultList();
			assertEquals( 2, resultList.size() );
			assertResult( resultList.get( 0 ), 1, 11, 11, SubChildEntityA1.class );
			assertResult( resultList.get( 1 ), 2, 21, null, null );
		} );
	}

	@Test
	public void testRightJoin(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final SubChildEntityA1 subChildEntityA1 = new SubChildEntityA1( 11 );
			s.persist( subChildEntityA1 );
			final SubChildEntityA2 subChildEntityA2 = new SubChildEntityA2( 12 );
			s.persist( subChildEntityA2 );
			s.persist( new ChildEntityB( 21 ) );
			s.persist( new RootOne( 1, subChildEntityA1 ) );
			s.persist( new RootOne( 2, subChildEntityA1 ) );
			s.persist( new RootOne( 3, null ) );
		} );
		scope.inTransaction( s -> {
			// simulate association with ChildEntityB
			s.createNativeMutationQuery( "update root_one set child_id = 21 where id = 3" ).executeUpdate();
		} );
		scope.inTransaction( s -> {
			final List<Tuple> resultList = s.createQuery(
					"select r, ce " +
							"from RootOne r right join r.child ce " +
							"order by r.id nulls last, ce.id",
					Tuple.class
			).getResultList();
			assertEquals( 3, resultList.size() );
			assertResult( resultList.get( 0 ), 1, 11, 11, SubChildEntityA1.class );
			assertResult( resultList.get( 1 ), 2, 11, 11, SubChildEntityA1.class );
			assertResult( resultList.get( 2 ), null, null, 12, SubChildEntityA2.class );
		} );
	}

	@Test
	public void testCrossJoin(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final SubChildEntityA1 subChildEntityA1 = new SubChildEntityA1( 11 );
			s.persist( subChildEntityA1 );
			s.persist( new ChildEntityB( 21 ) );
			s.persist( new RootOne( 1, subChildEntityA1 ) );
			s.persist( new RootOne( 2, null ) );
		} );
		scope.inTransaction( s -> {
			// simulate association with ChildEntityB
			s.createNativeMutationQuery( "update root_one set child_id = 21 where id = 2" ).executeUpdate();
		} );
		scope.inTransaction( s -> {
			final List<Tuple> resultList = s.createQuery(
					"select r, ce " +
							"from RootOne r cross join ChildEntityA ce " +
							"order by r.id nulls last, ce.id",
					Tuple.class
			).getResultList();
			assertEquals( 2, resultList.size() );
			assertResult( resultList.get( 0 ), 1, 11, 11, SubChildEntityA1.class );
			assertResult( resultList.get( 1 ), 2, 21, 11, SubChildEntityA1.class );
		} );
	}

	@Test
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsFullJoin.class )
	public void testFullJoin(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final SubChildEntityA1 subChildEntityA1 = new SubChildEntityA1( 11 );
			s.persist( subChildEntityA1 );
			final SubChildEntityA2 subChildEntityA2 = new SubChildEntityA2( 12 );
			s.persist( subChildEntityA2 );
			s.persist( new ChildEntityB( 21 ) );
			s.persist( new RootOne( 1, subChildEntityA1 ) );
			s.persist( new RootOne( 2, subChildEntityA1 ) );
			s.persist( new RootOne( 3, null ) );
			s.persist( new RootOne( 4, null ) );
		} );
		scope.inTransaction( s -> {
			// simulate association with ChildEntityB
			s.createNativeMutationQuery( "update root_one set child_id = 21 where id = 3" ).executeUpdate();
		} );
		scope.inTransaction( s -> {
			final List<Tuple> resultList = s.createQuery(
					"select r, ce " +
							"from RootOne r full join ChildEntityA ce on ce.id = r.childId " +
							"order by r.id nulls last, ce.id",
					Tuple.class
			).getResultList();
			assertEquals( 5, resultList.size() );
			assertResult( resultList.get( 0 ), 1, 11, 11, SubChildEntityA1.class );
			assertResult( resultList.get( 1 ), 2, 11, 11, SubChildEntityA1.class );
			assertResult( resultList.get( 2 ), 3, 21, null, null );
			assertResult( resultList.get( 3 ), 4, null, null, null );
			assertResult( resultList.get( 4 ), null, null, 12, SubChildEntityA2.class );
		} );
	}

	private <T extends ChildEntityA> void assertResult(
			Tuple result,
			Integer rootId,
			Integer rootChildId,
			Integer childId,
			Class<T> subClass) {
		if ( rootId != null ) {
			final RootOne root = result.get( 0, RootOne.class );
			assertEquals( rootId, root.getId() );
			assertEquals( rootChildId, root.getChildId() );
		}
		else {
			assertNull( result.get( 0 ) );
		}
		if ( subClass != null ) {
			assertInstanceOf( subClass, result.get( 1 ) );
			final ChildEntityA sub1 = result.get( 1, subClass );
			assertEquals( childId, sub1.getId() );
		}
		else {
			assertNull( result.get( 1 ) );
		}
	}

	@Entity( name = "BaseClass" )
	@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
	public static class BaseClass {
		@Id
		private Integer id;

		public BaseClass() {
		}

		public BaseClass(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}
	}

	@Entity( name = "ChildEntityA" )
	public static abstract class ChildEntityA extends BaseClass {
		public ChildEntityA() {
		}

		public ChildEntityA(Integer id) {
			super( id );
		}
	}

	@Entity( name = "SubChildEntityA1" )
	public static class SubChildEntityA1 extends ChildEntityA {
		public SubChildEntityA1() {
		}

		public SubChildEntityA1(Integer id) {
			super( id );
		}
	}

	@Entity( name = "SubChildEntityA2" )
	public static class SubChildEntityA2 extends ChildEntityA {
		public SubChildEntityA2() {
		}

		public SubChildEntityA2(Integer id) {
			super( id );
		}
	}

	@Entity( name = "ChildEntityB" )
	public static class ChildEntityB extends BaseClass {

		public ChildEntityB() {
		}

		public ChildEntityB(Integer id) {
			super( id );
		}
	}

	@Entity( name = "RootOne" )
	@Table( name = "root_one" )
	public static class RootOne {
		@Id
		private Integer id;

		@Column( name = "child_id", insertable = false, updatable = false )
		private Integer childId;

		@ManyToOne
		@JoinColumn( name = "child_id" )
		private ChildEntityA child;

		public RootOne() {
		}

		public RootOne(Integer id, ChildEntityA child) {
			this.id = id;
			this.child = child;
		}

		public Integer getId() {
			return id;
		}

		public Integer getChildId() {
			return childId;
		}
	}
}
