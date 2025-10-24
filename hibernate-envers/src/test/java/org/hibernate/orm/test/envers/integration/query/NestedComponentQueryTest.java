/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.Collections;
import java.util.List;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.JoinType;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@JiraKey(value = "HHH-11895")
@Jpa(annotatedClasses = {
		NestedComponentQueryTest.EntityOwner.class,
		NestedComponentQueryTest.Component1.class,
		NestedComponentQueryTest.Component2.class
})
@EnversTest
public class NestedComponentQueryTest {

	@Embeddable
	public static class Component1 {

		private String name1;

		@Embedded
		private Component2 component2;

		public String getName1() {
			return name1;
		}

		public void setName1(String name1) {
			this.name1 = name1;
		}

		public Component2 getComponent2() {
			return component2;
		}

		public void setComponent2(Component2 component2) {
			this.component2 = component2;
		}
	}

	@Embeddable
	public static class Component2 {

		private String name2;

		public String getName2() {
			return name2;
		}

		public void setName2(String name2) {
			this.name2 = name2;
		}
	}

	@Entity(name = "EntityOwner")
	@Audited
	public static class EntityOwner {

		@Id
		@GeneratedValue
		private Integer id;

		@Embedded
		private Component1 component1;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Component1 getComponent1() {
			return component1;
		}

		public void setComponent1(Component1 component1) {
			this.component1 = component1;
		}
	}

	private EntityOwner owner1;
	private EntityOwner owner2;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			owner1 = new EntityOwner();
			Component1 component1 = new Component1();
			component1.setName1( "X" );
			owner1.setComponent1( component1 );
			Component2 component2 = new Component2();
			component2.setName2( "Y" );
			component1.setComponent2( component2 );
			em.persist( owner1 );
			owner2 = new EntityOwner();
			Component1 component12 = new Component1();
			component12.setName1( "Z" );
			owner2.setComponent1( component12 );
			Component2 component22 = new Component2();
			component22.setName2( "Z" );
			component12.setComponent2( component22 );
			em.persist( owner2 );
		} );
	}

	@Test
	public void testQueryNestedComponent(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List actual = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( EntityOwner.class, 1 )
					.addProjection( AuditEntity.id() )
					.traverseRelation( "component1", JoinType.INNER, "c1" )
					.traverseRelation( "component2", JoinType.INNER, "c2" )
					.add( AuditEntity.property( "c2", "name2" ).eq( "Y" ) )
					.getResultList();
			assertEquals( Collections.singletonList( owner1.getId() ), actual, "Expected owner1 to be returned" );
		} );
	}

	@Test
	public void testQueryNestedComponentWithPropertyEquals(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List actual = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( EntityOwner.class, 1 )
					.addProjection( AuditEntity.id() )
					.traverseRelation( "component1", JoinType.INNER, "c1" )
					.traverseRelation( "component2", JoinType.INNER, "c2" )
					.add( AuditEntity.property( "c1", "name1" ).eqProperty( "c2", "name2" ) )
					.getResultList();
			assertEquals( Collections.singletonList( owner2.getId() ), actual, "Expected owner2 to be returned" );
		} );
	}

}
