/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.JoinType;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import static org.hibernate.envers.query.AuditEntity.disjunction;
import static org.hibernate.envers.query.AuditEntity.property;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11383")
@Jpa(annotatedClasses = {
		InheritanceAssociationToOneInnerJoinTest.EntityA.class,
		InheritanceAssociationToOneInnerJoinTest.EntityB.class,
		InheritanceAssociationToOneInnerJoinTest.EntityC.class,
		InheritanceAssociationToOneInnerJoinTest.EntityD.class
})
@EnversTest
public class InheritanceAssociationToOneInnerJoinTest {

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			final EntityC c = new EntityC();
			c.setId( 1 );
			c.setFoo( "bar" );
			em.persist( c );

			final EntityD d = new EntityD();
			d.setId( 1 );
			d.setFoo( "bar" );
			em.persist( d );

			final EntityB b1 = new EntityB();
			b1.setId( 1 );
			b1.setName( "b1" );
			b1.setRelationToC( c );
			b1.setRelationToD( d );
			em.persist( b1 );

			final EntityB b2 = new EntityB();
			b2.setId( 2 );
			b2.setName( "b2" );
			b2.setRelationToC( c );
			b2.setRelationToD( d );
			em.persist( b2 );
		} );
	}

	@Test
	public void testAuditQueryWithJoinedInheritanceUsingWithSemanticsManyToOne(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List results = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityB.class, 1 )
					.add(
							disjunction()
									.add( property( "name" ).like( "b1" ) )
									.add( property( "name" ).like( "b2" ) ) )
					.traverseRelation( "relationToC", JoinType.INNER )
					.add( property( "foo" ).like( "bar" ) )
					.getResultList();
			assertEquals( 2, results.size() );
		} );
	}

	@Test
	public void testAuditQueryWithJoinedInheritanceUsingWithSemanticsOneToOne(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List results = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityB.class, 1 )
					.add(
							disjunction()
									.add( property( "name" ).like( "b1" ) )
									.add( property( "name" ).like( "b2" ) ) )
					.traverseRelation( "relationToD", JoinType.INNER )
					.add( property( "foo" ).like( "bar" ) )
					.getResultList();
			assertEquals( 2, results.size() );
		} );
	}

	@Test
	public void testAuditQueryWithJoinedInheritanceUsingWithSemanticsToOne(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List results = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityB.class, 1 )
					.add(
							disjunction()
									.add( property( "name" ).like( "b1" ) )
									.add( property( "name" ).like( "b2" ) ) )
					.traverseRelation( "relationToC", JoinType.INNER )
					.add( property( "foo" ).like( "bar" ) )
					.up()
					.traverseRelation( "relationToD", JoinType.INNER )
					.add( property( "foo" ).like( "bar" ) )
					.getResultList();
			assertEquals( 2, results.size() );
		} );
	}

	@Test
	public void testAuditQueryWithJoinedInheritanceSubclassPropertyProjectionWithRelationTraversal(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// HHH-11383
			// This test was requested by the reporter so that we have a test that shows Hibernate is
			// automatically adding "INNER JOIN EntityA_AUD" despite the fact whether the query uses
			// the traverseRelation API or not.  This test makes sure that if the SQL generation is
			// changed in the future, Envers would properly fail if so.
			List results = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityB.class, 1 )
					.addProjection( property( "name" ) )
					.traverseRelation( "relationToC", JoinType.INNER )
					.add( property( "foo" ).like( "bar" ) )
					.getResultList();
			assertEquals( 2, results.size() );
		} );
	}

	@Entity(name = "EntityA")
	@Audited
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class EntityA {
		@Id
		private Integer id;
		@ManyToOne
		private EntityD relationToD;
		@ManyToOne
		private EntityC relationToC;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityC getRelationToC() {
			return relationToC;
		}

		public void setRelationToC(EntityC relationToC) {
			this.relationToC = relationToC;
		}

		public EntityD getRelationToD() {
			return relationToD;
		}

		public void setRelationToD(EntityD relationToD) {
			this.relationToD = relationToD;
		}
	}

	@Entity(name = "EntityB")
	@Audited
	public static class EntityB extends EntityA {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "EntityC")
	@Audited
	public static class EntityC {
		@Id
		private Integer id;
		private String foo;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}
	}

	@Entity(name = "EntityD")
	@Audited
	public static class EntityD {
		@Id
		private Integer id;
		private String foo;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}
	}
}
