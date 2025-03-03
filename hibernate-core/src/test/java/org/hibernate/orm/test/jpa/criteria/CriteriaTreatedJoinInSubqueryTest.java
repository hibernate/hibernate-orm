/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.List;

import org.hibernate.dialect.MySQLDialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@Jpa( annotatedClasses = {
		CriteriaTreatedJoinInSubqueryTest.MyEntity1.class,
		CriteriaTreatedJoinInSubqueryTest.MyEntity2.class,
		CriteriaTreatedJoinInSubqueryTest.MyEntity3.class,
		CriteriaTreatedJoinInSubqueryTest.MySubEntity2.class,
		CriteriaTreatedJoinInSubqueryTest.MyOtherSubEntity2.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16642" )
public class CriteriaTreatedJoinInSubqueryTest {
	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final MyEntity3 entity3 = new MyEntity3( 1L );
			entityManager.persist( entity3 );
			final MySubEntity2 subentity2 = new MySubEntity2();
			subentity2.setRef3( entity3 );
			subentity2.setStringProp( "test" );
			entityManager.persist( subentity2 );
			final MyEntity1 entity1b = new MyEntity1();
			entity1b.setRef2( subentity2 );
			entityManager.persist( entity1b );
			final MyOtherSubEntity2 otherSubEntity2 = new MyOtherSubEntity2();
			otherSubEntity2.setAnotherProp( "another" );
			entityManager.persist( otherSubEntity2 );
		} );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "delete from MyEntity1" ).executeUpdate();
			entityManager.createQuery( "delete from MyEntity2" ).executeUpdate();
			entityManager.createQuery( "delete from MyEntity3" ).executeUpdate();
		} );
	}

	@Test
	public void testRootQueryAssociationProp(EntityManagerFactoryScope scope) {
		testCriteriaRootQuery(
				scope,
				"ref3",
				scope.fromTransaction( entityManager -> entityManager.find( MyEntity3.class, 1L ) )
		);
	}

	@Test
	public void testRootQueryStringProp(EntityManagerFactoryScope scope) {
		testCriteriaRootQuery( scope, "stringProp", "test" );
	}

	@Test
	public void testQueryAssociationProp(EntityManagerFactoryScope scope) {
		testCriteriaQuery(
				scope,
				"ref3",
				scope.fromTransaction( entityManager -> entityManager.find( MyEntity3.class, 1L ) )
		);
	}

	@Test
	public void testQueryStringProp(EntityManagerFactoryScope scope) {
		testCriteriaQuery( scope, "stringProp", "test" );
	}

	@Test
	public void testSubqueryAssociationProp(EntityManagerFactoryScope scope) {
		testCriteriaSubquery(
				scope,
				"ref3",
				scope.fromTransaction( entityManager -> entityManager.find( MyEntity3.class, 1L ) )
		);
	}

	@Test
	public void testSubqueryStringProp(EntityManagerFactoryScope scope) {
		testCriteriaSubquery( scope, "stringProp", "test" );
	}

	@Test
	@SkipForDialect( dialectClass = MySQLDialect.class, reason = "MySQL doesn't support updating the same table used in a select subquery" )
	public void testUpdateAssociationProp(EntityManagerFactoryScope scope) {
		testCriteriaUpdate(
				scope,
				"ref3",
				scope.fromTransaction( entityManager -> entityManager.find( MyEntity3.class, 1L ) )
		);
	}

	@Test
	@SkipForDialect( dialectClass = MySQLDialect.class, reason = "MySQL doesn't support updating the same table used in a select subquery" )
	public void testUpdateBasicProp(EntityManagerFactoryScope scope) {
		testCriteriaUpdate( scope, "stringProp", "test" );
	}

	private void testCriteriaRootQuery(EntityManagerFactoryScope scope, String propName, Object propValue) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<MyEntity2> cq = cb.createQuery( MyEntity2.class );
			final Root<MyEntity2> root = cq.from( MyEntity2.class );
			final Root<MySubEntity2> treatedRoot = cb.treat( root, MySubEntity2.class );
			cq.select( root ).where( cb.equal( treatedRoot.get( propName ), propValue ) );
			final List<MyEntity2> resultList = entityManager.createQuery( cq ).getResultList();
			assertThat( resultList ).hasSize( 1 );
		} );
	}

	private void testCriteriaQuery(EntityManagerFactoryScope scope, String propName, Object propValue) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<MyEntity1> cq = cb.createQuery( MyEntity1.class );
			final Root<MyEntity1> root = cq.from( MyEntity1.class );
			final Join<MyEntity1, MySubEntity2> treatedJoin = cb.treat( root.join( "ref2" ), MySubEntity2.class );
			cq.select( root ).where( cb.equal( treatedJoin.get( propName ), propValue ) );
			final List<MyEntity1> resultList = entityManager.createQuery( cq ).getResultList();
			assertThat( resultList ).hasSize( 1 );
		} );
	}

	private void testCriteriaSubquery(EntityManagerFactoryScope scope, String propName, Object propValue) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<MyEntity1> cq = cb.createQuery( MyEntity1.class );
			final Root<MyEntity1> root = cq.from( MyEntity1.class );
			final Subquery<Long> subquery = getSubquery( cb, cq, propName, propValue );
			cq.select( root ).where( root.get( "id" ).in( subquery ) );
			final List<MyEntity1> resultList = entityManager.createQuery( cq ).getResultList();
			assertThat( resultList ).hasSize( 1 );
		} );
	}

	private void testCriteriaUpdate(EntityManagerFactoryScope scope, String propName, Object propValue) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaUpdate<MyEntity1> cu = cb.createCriteriaUpdate( MyEntity1.class );
			final Root<MyEntity1> root = cu.from( MyEntity1.class );
			cu.set( root.get( "data" ), "updated_data" );
			final Subquery<Long> subquery = getSubquery( cb, cu, propName, propValue );
			cu.where( root.get( "id" ).in( subquery ) );
			assertEquals( 1, entityManager.createQuery( cu ).executeUpdate() );
		} );
	}

	private Subquery<Long> getSubquery(
			CriteriaBuilder cb,
			CommonAbstractCriteria criteria,
			String propName,
			Object propValue) {
		final Subquery<Long> subquery = criteria.subquery( Long.class );
		final Root<MyEntity1> subroot = subquery.from( MyEntity1.class );
		final Join<MyEntity1, MySubEntity2> treatedJoin = cb.treat( subroot.join( "ref2" ), MySubEntity2.class );
		return subquery.select( subroot.get( "id" ) ).where( cb.equal( treatedJoin.get( propName ), propValue ) );
	}

	@Entity( name = "MyEntity1" )
	public static class MyEntity1 {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		@JoinColumn( name = "ref2" )
		private MyEntity2 ref2;

		private String data;

		public void setRef2(MyEntity2 ref2) {
			this.ref2 = ref2;
		}

		public MyEntity2 getRef2() {
			return ref2;
		}
	}

	@Entity( name = "MyEntity2" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	public static class MyEntity2 {
		@Id
		@GeneratedValue
		private Long id;
	}

	@Entity( name = "MySubEntity2" )
	public static class MySubEntity2 extends MyEntity2 {
		@ManyToOne
		@JoinColumn( name = "ref3" )
		private MyEntity3 ref3;

		private String stringProp;

		public MyEntity3 getRef3() {
			return ref3;
		}

		public void setRef3(MyEntity3 ref3) {
			this.ref3 = ref3;
		}

		public void setStringProp(String stringProp) {
			this.stringProp = stringProp;
		}
	}

	@Entity( name = "MyOtherSubEntity2" )
	public static class MyOtherSubEntity2 extends MyEntity2 {
		private String anotherProp;

		public String getAnotherProp() {
			return anotherProp;
		}

		public void setAnotherProp(String anotherProp) {
			this.anotherProp = anotherProp;
		}
	}

	@Entity( name = "MyEntity3" )
	public static class MyEntity3 {
		@Id
		private Long id;

		public MyEntity3() {
		}

		public MyEntity3(Long id) {
			this.id = id;
		}
	}
}
