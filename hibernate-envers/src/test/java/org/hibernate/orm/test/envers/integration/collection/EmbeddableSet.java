/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.collection.EmbeddableSetEntity;
import org.hibernate.orm.test.envers.entities.components.Component3;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@EnversTest
@Jpa(annotatedClasses = { EmbeddableSetEntity.class })
public class EmbeddableSet {

	private Integer entityId;
	private Component3 comp1;
	private Component3 comp2;

	@BeforeClassTemplate
	@JiraKey(value = "HHH-9199")
	public void initData(EntityManagerFactoryScope scope) {
		comp1 = new Component3( "comp1", null, null );
		comp2 = new Component3( "comp2", null, null );

		EmbeddableSetEntity entity = new EmbeddableSetEntity();

		scope.inTransaction( em -> {
			entity.getComponentSet().add( comp1 );
			entity.getComponentSet().add( comp2 );

			em.persist( entity );
			entityId = entity.getId();
		} );

		scope.inTransaction( em -> {
			final EmbeddableSetEntity e = em.find( EmbeddableSetEntity.class, entityId );
			e.getComponentSet().remove( comp1 );
		} );
	}

	@Test
	public void testRemoval(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			EmbeddableSetEntity rev1 = auditReader.find( EmbeddableSetEntity.class, entityId, 1 );
			EmbeddableSetEntity rev2 = auditReader.find( EmbeddableSetEntity.class, entityId, 2 );
			assertEquals( TestTools.makeSet( comp1, comp2 ), rev1.getComponentSet(), "Unexpected components" );
			assertEquals( TestTools.makeSet( comp2 ), rev2.getComponentSet(), "Unexpected components" );
		} );
	}
}
