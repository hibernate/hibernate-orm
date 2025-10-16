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
import org.hibernate.envers.AuditReaderFactory;
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
@JiraKey(value = "HHH-11573")
@Jpa(annotatedClasses = {
		EntityTypeQueryTest.EntityA.class,
		EntityTypeQueryTest.EntityB.class
})
@EnversTest
public class EntityTypeQueryTest {

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

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
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
		} );
	}

	@Test
	public void testRestrictToSubType(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<?> list = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 1 )
					.add( AuditEntity.entityType( EntityB.class ) )
					.addProjection( AuditEntity.property( "name" ) )
					.addOrder( AuditEntity.property( "name" ).asc() )
					.getResultList();
			assertEquals( list( "b1", "b2" ), list, "Expected only entities of type EntityB to be selected" );
		} );
	}

	@Test
	public void testRestrictToSuperType(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<?> list = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 1 )
					.add( AuditEntity.entityType( EntityA.class ) )
					.addProjection( AuditEntity.property( "name" ) )
					.addOrder( AuditEntity.property( "name" ).asc() )
					.getResultList();
			assertEquals( list( "a1", "a2" ), list, "Expected only entities of type EntityA to be selected" );
		} );
	}

	private List<String> list(final String... elements) {
		final List<String> result = new ArrayList<>();
		Collections.addAll( result, elements );
		return result;
	}

}
