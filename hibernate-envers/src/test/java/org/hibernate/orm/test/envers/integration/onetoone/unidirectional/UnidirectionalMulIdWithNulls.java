/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.unidirectional;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.ids.EmbId;
import org.hibernate.orm.test.envers.entities.ids.EmbIdTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@EnversTest
@Jpa(annotatedClasses = {EmbIdTestEntity.class, UniRefIngMulIdEntity.class})
public class UnidirectionalMulIdWithNulls {
	private EmbId ei;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		ei = new EmbId( 1, 2 );

		// Revision 1
		scope.inTransaction( em -> {
			EmbIdTestEntity eite = new EmbIdTestEntity( ei, "data" );
			UniRefIngMulIdEntity notNullRef = new UniRefIngMulIdEntity( 1, "data 1", eite );
			UniRefIngMulIdEntity nullRef = new UniRefIngMulIdEntity( 2, "data 2", null );

			em.persist( eite );
			em.persist( notNullRef );
			em.persist( nullRef );
		} );
	}

	@Test
	public void testNullReference(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			UniRefIngMulIdEntity nullRef = auditReader.find( UniRefIngMulIdEntity.class, 2, 1 );
			assertNull( nullRef.getReference() );
		} );
	}

	@Test
	public void testNotNullReference(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			EmbIdTestEntity eite = auditReader.find( EmbIdTestEntity.class, ei, 1 );
			UniRefIngMulIdEntity notNullRef = auditReader.find( UniRefIngMulIdEntity.class, 1, 1 );
			assertNotNull( notNullRef.getReference() );
			assertEquals( eite, notNullRef.getReference() );
		} );
	}
}
