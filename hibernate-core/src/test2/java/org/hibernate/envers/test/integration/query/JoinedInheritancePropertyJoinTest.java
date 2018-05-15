/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.query;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToOne;
import javax.persistence.criteria.JoinType;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.transaction.TransactionUtil;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11416")
public class JoinedInheritancePropertyJoinTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityA.class, EntityB.class, EntityC.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final EntityC c1 = new EntityC();
			c1.setId( 1 );
			c1.setName( "c1" );
			c1.setFoo( "foo" );
			c1.setPropB( "propB" );
			c1.setPropC( "propC" );
			entityManager.persist( c1 );

			final EntityA a1 = new EntityA();
			a1.setId( 1 );
			a1.setRelationToC( c1 );
			a1.setPropA( "propC" );
			entityManager.persist( a1 );
		} );
	}

	@Test
	public void testAuditQueryWithJoinedInheritanceUnrelatedPropertyJoin() {
		// The problem is that this query succeeds on DefaultAuditStrategy, fails on ValidityAuditStrategy
		//
		// ValidityAuditStrategy
		// ---------------------
		// select
		// 		joinedinhe0_.id as id1_1_,
		// 		joinedinhe0_.REV as REV2_1_,
		// 		joinedinhe0_.REVTYPE as REVTYPE3_1_,
		// 		joinedinhe0_.REVEND as REVEND4_1_,
		// 		joinedinhe0_.relationToC_id as relation5_1_
		// from
		// 		EntityA_AUD joinedinhe0_
		// 	inner join EntityC_AUD joinedinhe1_
		// 		on (joinedinhe0_.relationToC_id=joinedinhe1_.id or (joinedinhe0_.relationToC_id is null)
		// 			and (joinedinhe1_.id is null))
		// where
		// 	joinedinhe0_.REV<=?
		// and
		// 	joinedinhe0_.REVTYPE<>?
		// and
		// 	(joinedinhe0_.REVEND>? or joinedinhe0_.REVEND is null)
		// and
		// 	joinedinhe1_.REV<=?
		// and
		// 	(joinedinhe1_1_.REVEND>? or joinedinhe1_1_.REVEND is null)
		//
		// Error: SQL Error: 42122, SQLState: 42S22
		// Column "JOINEDINHE1_1_.REVEND" not found
		//
		List results = getAuditReader().createQuery().forEntitiesAtRevision( EntityA.class, 1 )
				.traverseRelation( "relationToC", JoinType.INNER )
				.getResultList();
		assertEquals( 1, results.size() );
	}

	@Test
	public void testHibernateUnrelatedPropertyQuery() {
		final String queryString = "FROM EntityA a Inner Join EntityC c ON a.propA = c.propC Where c.propB = :propB";
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			List results = entityManager.createQuery( queryString ).setParameter( "propB", "propB" ).getResultList();
			assertEquals( 1, results.size() );
		} );
	}

	@Entity(name = "EntityA")
	@Audited
	public static class EntityA {
		@Id
		private Integer id;
		private String propA;
		@OneToOne
		private EntityC relationToC;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getPropA() {
			return propA;
		}

		public void setPropA(String propA) {
			this.propA = propA;
		}

		public EntityC getRelationToC() {
			return relationToC;
		}

		public void setRelationToC(EntityC relationToC) {
			this.relationToC = relationToC;
		}
	}

	@Entity(name = "EntityB")
	@Audited
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class EntityB {
		@Id
		private Integer id;
		private String name;
		private String propB;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPropB() {
			return propB;
		}

		public void setPropB(String propB) {
			this.propB = propB;
		}
	}

	@Entity(name = "EntityC")
	@Audited
	public static class EntityC extends EntityB {
		private String foo;
		private String propC;

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		public String getPropC() {
			return propC;
		}

		public void setPropC(String propC) {
			this.propC = propC;
		}
	}
}
