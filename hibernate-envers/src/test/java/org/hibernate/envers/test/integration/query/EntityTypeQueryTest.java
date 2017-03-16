/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.query;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.junit.Test;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class EntityTypeQueryTest extends BaseEnversJPAFunctionalTestCase {

	@Entity
	@Audited
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class EntityA {

		@Id
		@GeneratedValue
		private Long id;
		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@Entity
	@Audited
	public static class EntityB extends EntityA {

	}

	private EntityA a1;
	private EntityA a2;
	private EntityB b1;
	private EntityB b2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ EntityA.class, EntityB.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		a1 = new EntityA();
		a1.setName( "a1" );
		em.persist( a1 );
		a2 = new EntityA();
		a2.setName( "a2" );
		em.persist( a2 );
		b1 = new EntityB();
		b1.setName( "b1" );
		em.persist( b1 );
		b2 = new EntityB();
		b2.setName( "b2" );
		em.persist( b2 );
		em.getTransaction().commit();
	}

	@Test
	public void testRestrictToSubType() {
		List<?> list = getAuditReader().createQuery().forEntitiesAtRevision( EntityA.class, 1 )
				.add( AuditEntity.entityType( EntityB.class ) ).addProjection( AuditEntity.property( "name" ) ).getResultList();
		assertEquals( "Expected only entities of type EntityB to be selected", list( "b1", "b2" ), list );
	}

	@Test
	public void testRestrictToSuperType() {
		List<?> list = getAuditReader().createQuery().forEntitiesAtRevision( EntityA.class, 1 )
				.add( AuditEntity.entityType( EntityA.class ) ).addProjection( AuditEntity.property( "name" ) ).getResultList();
		assertEquals( "Expected only entities of type EntityA to be selected", list( "a1", "a2" ), list );
	}

	private List<String> list(final String... elements) {
		final List<String> result = new ArrayList<>();
		Collections.addAll( result, elements );
		return result;
	}

}
