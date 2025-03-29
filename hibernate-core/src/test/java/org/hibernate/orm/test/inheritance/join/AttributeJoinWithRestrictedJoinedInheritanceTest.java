/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.join;

import java.util.List;

import org.hibernate.annotations.SQLRestriction;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
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

@DomainModel( annotatedClasses = {
		AttributeJoinWithRestrictedJoinedInheritanceTest.BaseClass.class,
		AttributeJoinWithRestrictedJoinedInheritanceTest.ChildEntityA.class,
		AttributeJoinWithRestrictedJoinedInheritanceTest.SubChildEntityA1.class,
		AttributeJoinWithRestrictedJoinedInheritanceTest.SubChildEntityA2.class,
		AttributeJoinWithRestrictedJoinedInheritanceTest.ChildEntityB.class,
		AttributeJoinWithRestrictedJoinedInheritanceTest.RootOne.class
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17646" )
public class AttributeJoinWithRestrictedJoinedInheritanceTest {
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
	public void testLeftJoinWithDiscriminatorFiltering(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final ChildEntityA childEntityA1 = new SubChildEntityA1( 11 );
			s.persist( childEntityA1 );
			final ChildEntityA childEntityA2 = new SubChildEntityA2( 21 );
			s.persist( childEntityA2 );
			s.persist( new RootOne( 1, childEntityA1 ) );
			s.persist( new RootOne( 2, childEntityA2 ) );
		} );
		scope.inTransaction( s -> {
			final List<Tuple> resultList = s.createQuery(
					"select r, ce " +
							"from RootOne r left join r.child ce " +
							"order by r.id",
					Tuple.class
			).getResultList();
			assertEquals( 2, resultList.size() );
			assertResult( resultList.get( 0 ), 1, 11, 11, "child_a_1", SubChildEntityA1.class );
			assertResult( resultList.get( 1 ), 2, 21, null, null, null );
		} );
	}

	private <T extends ChildEntityA> void assertResult(
			Tuple result,
			Integer rootId,
			Integer rootChildId,
			Integer childId,
			String discValue,
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
			assertEquals( discValue, sub1.getDiscCol() );
		}
		else {
			assertNull( result.get( 1 ) );
		}
	}

	/**
	 * NOTE: We define a {@link DiscriminatorColumn} to allow multiple subclasses
	 * to share the same table name. This will need additional care when pruning
	 * the table expression, since we'll have to add the discriminator condition
	 * before joining with the subclass tables
	 */
	@Entity( name = "BaseClass" )
	@Inheritance( strategy = InheritanceType.JOINED )
	@DiscriminatorColumn( name = "disc_col" )
	@SQLRestriction( "ident < 20" )
	public static class BaseClass {
		@Id
		@Column(name = "ident")
		private Integer id;

		@Column( name = "disc_col", insertable = false, updatable = false )
		private String discCol;

		public BaseClass() {
		}

		public BaseClass(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public String getDiscCol() {
			return discCol;
		}
	}

	@Entity( name = "ChildEntityA" )
	@Table( name = "child_entity" )
	public static abstract class ChildEntityA extends BaseClass {

		public ChildEntityA() {
		}

		public ChildEntityA(Integer id) {
			super( id );
		}
	}

	@Entity( name = "SubChildEntityA1" )
	@DiscriminatorValue( "child_a_1" )
	public static class SubChildEntityA1 extends ChildEntityA {
		public SubChildEntityA1() {
		}

		public SubChildEntityA1(Integer id) {
			super( id );
		}
	}

	@Entity( name = "SubChildEntityA2" )
	@DiscriminatorValue( "child_a_2" )
	public static class SubChildEntityA2 extends ChildEntityA {
		public SubChildEntityA2() {
		}

		public SubChildEntityA2(Integer id) {
			super( id );
		}
	}

	@Entity( name = "ChildEntityB" )
	@Table( name = "child_entity" )
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
		@JoinColumn( name = "child_id")
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
