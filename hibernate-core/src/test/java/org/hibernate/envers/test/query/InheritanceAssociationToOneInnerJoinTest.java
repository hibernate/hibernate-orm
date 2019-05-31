/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.query;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.criteria.JoinType;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.envers.query.AuditEntity.disjunction;
import static org.hibernate.envers.query.AuditEntity.property;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11383")
@Disabled("NYI - Joined Inheritance Support")
public class InheritanceAssociationToOneInnerJoinTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityA.class, EntityB.class, EntityC.class, EntityD.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransaction(
				entityManager -> {
					final EntityC c = new EntityC();
					c.setId( 1 );
					c.setFoo( "bar" );
					entityManager.persist( c );

					final EntityD d = new EntityD();
					d.setId( 1 );
					d.setFoo( "bar" );
					entityManager.persist( d );

					final EntityB b1 = new EntityB();
					b1.setId( 1 );
					b1.setName( "b1" );
					b1.setRelationToC( c );
					b1.setRelationToD( d );
					entityManager.persist( b1 );

					final EntityB b2 = new EntityB();
					b2.setId( 2 );
					b2.setName( "b2" );
					b2.setRelationToC( c );
					b2.setRelationToD( d );
					entityManager.persist( b2 );
				}
		);
	}

	@DynamicTest
	public void testAuditQueryWithJoinedInheritanceUsingWithSemanticsManyToOne() {
		List results = getAuditReader().createQuery().forEntitiesAtRevision( EntityB.class, 1 )
				.add(
						disjunction()
								.add( property( "name" ).like( "b1" ) )
								.add( property( "name" ).like( "b2" ) ) )
				.traverseRelation( "relationToC", JoinType.INNER )
				.add( property( "foo" ).like( "bar" ) )
				.getResultList();
		assertThat( results, CollectionMatchers.hasSize( 2 ) );
	}

	@DynamicTest
	public void testAuditQueryWithJoinedInheritanceUsingWithSemanticsOneToOne() {
		List results = getAuditReader().createQuery().forEntitiesAtRevision( EntityB.class, 1 )
				.add(
						disjunction()
								.add( property( "name" ).like( "b1" ) )
								.add( property( "name" ).like( "b2" ) ) )
				.traverseRelation( "relationToD", JoinType.INNER )
				.add( property( "foo" ).like( "bar" ) )
				.getResultList();
		assertThat( results, CollectionMatchers.hasSize( 2 ) );
	}

	@DynamicTest
	public void testAuditQueryWithJoinedInheritanceUsingWithSemanticsToOne() {
		List results = getAuditReader().createQuery().forEntitiesAtRevision( EntityB.class, 1 )
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
		assertThat( results, CollectionMatchers.hasSize( 2 ) );
	}

	@DynamicTest
	public void testAuditQueryWithJoinedInheritanceSubclassPropertyProjectionWithRelationTraversal() {
		// HHH-11383
		// This test was requested by the reporter so that we have a test that shows Hibernate is
		// automatically adding "INNER JOIN EntityA_AUD" despite the fact whether the query uses
		// the traverseRelation API or not.  This test makes sure that if the SQL generation is
		// changed in the future, Envers would properly fail if so.
		List results = getAuditReader().createQuery().forEntitiesAtRevision( EntityB.class, 1 )
				.addProjection( property( "name" ) )
				.traverseRelation( "relationToC", JoinType.INNER )
				.add( property( "foo" ).like( "bar" ) )
				.getResultList();
		assertThat( results, CollectionMatchers.hasSize( 2 ) );
	}

	@Entity(name = "EntityA")
	@Audited
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class EntityA {
		@Id
		private Integer id;
		@OneToOne
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
