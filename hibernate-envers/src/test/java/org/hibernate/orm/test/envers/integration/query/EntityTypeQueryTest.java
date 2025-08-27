/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@JiraKey(value = "HHH-11573")
public class EntityTypeQueryTest extends BaseEnversJPAFunctionalTestCase {

	@Entity(name = "EntityA")
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

	@Entity(name = "EntityB")
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
		doInJPA( this::entityManagerFactory, entityManager -> {
			a1 = new EntityA();
			a1.setName( "a1" );
			entityManager.persist( a1 );

			a2 = new EntityA();
			a2.setName( "a2" );
			entityManager.persist( a2 );

			b1 = new EntityB();
			b1.setName( "b1" );
			entityManager.persist( b1 );

			b2 = new EntityB();
			b2.setName( "b2" );
			entityManager.persist( b2 );
		} );
	}

	@Test
	public void testRestrictToSubType() {
		List<?> list = getAuditReader().createQuery().forEntitiesAtRevision( EntityA.class, 1 )
				.add( AuditEntity.entityType( EntityB.class ) )
				.addProjection( AuditEntity.property( "name" ) )
				.addOrder( AuditEntity.property( "name" ).asc() )
				.getResultList();
		assertEquals( "Expected only entities of type EntityB to be selected", list( "b1", "b2" ), list );
	}

	@Test
	public void testRestrictToSuperType() {
		List<?> list = getAuditReader().createQuery().forEntitiesAtRevision( EntityA.class, 1 )
				.add( AuditEntity.entityType( EntityA.class ) )
				.addProjection( AuditEntity.property( "name" ) )
				.addOrder( AuditEntity.property( "name" ).asc() )
				.getResultList();
		assertEquals( "Expected only entities of type EntityA to be selected", list( "a1", "a2" ), list );
	}

	private List<String> list(final String... elements) {
		final List<String> result = new ArrayList<>();
		Collections.addAll( result, elements );
		return result;
	}

}
