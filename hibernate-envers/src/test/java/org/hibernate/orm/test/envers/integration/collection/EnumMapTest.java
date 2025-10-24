/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.collection.EnumMapEntity;
import org.hibernate.orm.test.envers.entities.collection.EnumMapType;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-6374")
@EnversTest
@Jpa(annotatedClasses = {EnumMapEntity.class, EnumMapType.class})
public class EnumMapTest {
	private Integer entityId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// revision 1
		scope.inTransaction( em -> {
			EnumMapEntity entity = new EnumMapEntity();
			entity.getTypes().put( EnumMapEntity.EnumType.TYPE_A, new EnumMapType( "A" ) );
			entity.getTypes().put( EnumMapEntity.EnumType.TYPE_B, new EnumMapType( "B" ) );
			em.persist( entity );
			entityId = entity.getId();
		} );

		// revision 2
		scope.inTransaction( em -> {
			EnumMapEntity entity = em.find( EnumMapEntity.class, entityId );
			entity.getTypes().remove( EnumMapEntity.EnumType.TYPE_A );
			entity.getTypes().put( EnumMapEntity.EnumType.TYPE_C, new EnumMapType( "C" ) );
		} );
	}

	@Test
	public void testRevisionsCount(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( 2, auditReader.getRevisions( EnumMapEntity.class, entityId ).size() );
		} );
	}

	@Test
	public void testAuditEnumMapCollection(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			EnumMapEntity rev1 = auditReader.find( EnumMapEntity.class, entityId, 1 );
			assertTrue( rev1.getTypes().keySet().containsAll(
					Arrays.asList( EnumMapEntity.EnumType.TYPE_A, EnumMapEntity.EnumType.TYPE_B )
			) );
			EnumMapEntity rev2 = auditReader.find( EnumMapEntity.class, entityId, 2 );
			assertTrue( rev2.getTypes().keySet().containsAll(
					Arrays.asList( EnumMapEntity.EnumType.TYPE_B, EnumMapEntity.EnumType.TYPE_C )
			) );
		} );
	}
}
