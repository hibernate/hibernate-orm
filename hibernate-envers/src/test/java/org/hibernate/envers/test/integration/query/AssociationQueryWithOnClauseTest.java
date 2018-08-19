/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.criteria.JoinType;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-11896")
public class AssociationQueryWithOnClauseTest extends BaseEnversJPAFunctionalTestCase {

	private EntityA a1;
	private EntityA a2;
	private EntityA a3;

	@Entity(name = "EntityA")
	@Audited
	public static class EntityA {

		@Id
		private Long id;

		@ManyToOne
		private EntityB bManyToOne;

		@OneToMany
		@AuditJoinTable(name = "entitya_onetomany_entityb_aud")
		private Set<EntityB> bOneToMany = new HashSet<>();

		@ManyToMany
		@JoinTable(name = "entitya_manytomany_entityb")
		private Set<EntityB> bManyToMany = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public EntityB getbManyToOne() {
			return bManyToOne;
		}

		public void setbManyToOne(EntityB bManyToOne) {
			this.bManyToOne = bManyToOne;
		}

		public Set<EntityB> getbOneToMany() {
			return bOneToMany;
		}

		public void setbOneToMany(Set<EntityB> bOneToMany) {
			this.bOneToMany = bOneToMany;
		}

		public Set<EntityB> getbManyToMany() {
			return bManyToMany;
		}

		public void setbManyToMany(Set<EntityB> bManyToMany) {
			this.bManyToMany = bManyToMany;
		}

	}

	@Entity(name = "EntityB")
	@Audited
	public static class EntityB {

		@Id
		private Long id;

		private String type;

		private int number;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public int getNumber() {
			return number;
		}

		public void setNumber(int number) {
			this.number = number;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ EntityA.class, EntityB.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		em.getTransaction().begin();
		final EntityB b1t1 = new EntityB();
		b1t1.setId( 21L );
		b1t1.setType( "T1" );
		b1t1.setNumber( 1 );
		em.persist( b1t1 );
		final EntityB b2t2 = new EntityB();
		b2t2.setId( 22L );
		b2t2.setType( "T2" );
		b2t2.setNumber( 2 );
		em.persist( b2t2 );
		final EntityB b3t1 = new EntityB();
		b3t1.setId( 23L );
		b3t1.setType( "T1" );
		b3t1.setNumber( 3 );
		em.persist( b3t1 );

		a1 = new EntityA();
		a1.setId( 1L );
		a1.setbManyToOne( b1t1 );
		a1.getbOneToMany().add( b1t1 );
		a1.getbOneToMany().add( b2t2 );
		a1.getbManyToMany().add( b1t1 );
		a1.getbManyToMany().add( b2t2 );
		em.persist( a1 );
		a2 = new EntityA();
		a2.setId( 2L );
		a2.setbManyToOne( b2t2 );
		a2.getbManyToMany().add( b3t1 );
		em.persist( a2 );
		a3 = new EntityA();
		a3.setId( 3L );
		a3.setbManyToOne( b3t1 );
		a3.getbOneToMany().add( b3t1 );
		a3.getbManyToMany().add( b3t1 );
		em.persist( a3 );

		em.getTransaction().commit();
	}

	@Test
	public void testManyToOne() {
		List list = getAuditReader().createQuery().forEntitiesAtRevision( EntityA.class, 1 )
				.traverseRelation( "bManyToOne", JoinType.LEFT, "b", AuditEntity.property( "b", "type" ).eq( "T1" ) )
				.addOrder( AuditEntity.property( "b", "number" ).asc() ).up().addProjection( AuditEntity.id() )
				.addProjection( AuditEntity.property( "b", "number" ) ).getResultList();
		assertArrayListEquals( list, tuple( a2.getId(), null ), tuple( a1.getId(), 1 ), tuple( a3.getId(), 3 ) );
	}

	@Test
	public void testOneToMany() {
		List list = getAuditReader().createQuery().forEntitiesAtRevision( EntityA.class, 1 )
				.traverseRelation( "bOneToMany", JoinType.LEFT, "b", AuditEntity.property( "b", "type" ).eq( "T1" ) )
				.addOrder( AuditEntity.property( "b", "number" ).asc() ).up().addOrder( AuditEntity.id().asc() ).addProjection( AuditEntity.id() )
				.addProjection( AuditEntity.property( "b", "number" ) ).getResultList();
		assertArrayListEquals( list, tuple( a1.getId(), null ), tuple( a2.getId(), null ), tuple( a1.getId(), 1 ), tuple( a3.getId(), 3 ) );
	}

	@Test
	public void testManyToMany() {
		List list = getAuditReader().createQuery().forEntitiesAtRevision( EntityA.class, 1 )
				.traverseRelation( "bManyToMany", JoinType.LEFT, "b", AuditEntity.property( "b", "type" ).eq( "T1" ) )
				.addOrder( AuditEntity.property( "b", "number" ).asc() ).up().addOrder( AuditEntity.id().asc() ).addProjection( AuditEntity.id() )
				.addProjection( AuditEntity.property( "b", "number" ) ).getResultList();
		assertArrayListEquals( list, tuple( a1.getId(), null ), tuple( a1.getId(), 1 ), tuple( a2.getId(), 3 ), tuple( a3.getId(), 3 ) );
	}

	private Object[] tuple(final Long id, final Integer number) {
		return new Object[]{ id, number };
	}

	private void assertArrayListEquals(final List actual, final Object[]... expected) {
		assertEquals( "Unexpected number of results", expected.length, actual.size() );
		for ( int i = 0; i < expected.length; i++ ) {
			final Object[] exp = expected[i];
			final Object[] act = (Object[]) actual.get( i );
			assertArrayEquals( exp, act );
		}
	}

}
