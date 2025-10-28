/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = {
		TreatedSubclassSameTypeTest.MyEntity1.class,
		TreatedSubclassSameTypeTest.MyEntity2.class,
		TreatedSubclassSameTypeTest.MySubEntity1.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17299" )
public class TreatedSubclassSameTypeTest {
	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new MyEntity1() );
			var entity2 = new MyEntity2( "entity2" );
			var entity1 = new MySubEntity1( entity2 );
			session.persist( entity2 );
			session.persist( entity1 );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testJoinOnTreat(SessionFactoryScope scope) {
		executeQuery( scope, criteria -> criteria.from( MyEntity1.class ), true );
	}

	@Test
	public void testJoinOnTreatedSubtype(SessionFactoryScope scope) {
		executeQuery( scope, criteria -> criteria.from( MySubEntity1.class ), true );
	}

	@Test
	public void testJoinOnSubtype(SessionFactoryScope scope) {
		executeQuery( scope, criteria -> criteria.from( MySubEntity1.class ), false );
	}

	@SuppressWarnings( "unchecked" )
	private void executeQuery(
			SessionFactoryScope scope,
			Function<CriteriaQuery<MyEntity1>, Root<? extends MyEntity1>> rootSupplier,
			boolean treat) {
		scope.inTransaction( session -> {
			var cb = session.getCriteriaBuilder();
			var criteria = cb.createQuery( MyEntity1.class );
			var root = rootSupplier.apply( criteria );
			var subRoot = treat
					? cb.treat( ( (Root<MyEntity1>) root ), MySubEntity1.class )
					: (Root<MySubEntity1>) root;
			criteria.where( cb.equal( subRoot.join( "ref" ).get( "name" ), "entity2" ) );
			//noinspection removal
			var result = session.createQuery( criteria ).getSingleResult();
			assertThat( result ).isInstanceOf( MySubEntity1.class );
			assertThat( ( (MySubEntity1) result ).getRef().getName() ).isEqualTo( "entity2" );
		} );
	}

	@SuppressWarnings("unused")
	@Entity( name = "MyEntity1" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	public static class MyEntity1 {
		@Id
		@GeneratedValue
		private Long id;
	}

	@SuppressWarnings("unused")
	@Entity( name = "MySubEntity1" )
	public static class MySubEntity1 extends MyEntity1 {
		@ManyToOne
		private MyEntity2 ref;

		public MySubEntity1() {
		}

		public MySubEntity1(MyEntity2 ref) {
			this.ref = ref;
		}

		public MyEntity2 getRef() {
			return ref;
		}
	}

	@SuppressWarnings("unused")
	@Entity( name = "MyEntity2" )
	public static class MyEntity2 {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public MyEntity2() {
		}

		public MyEntity2(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
