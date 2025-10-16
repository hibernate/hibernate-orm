/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.dynamic;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey("HHH-8049")
@EnversTest
@Jpa(
		xmlMappings = "mappings/dynamicComponents/mapNotAudited.hbm.xml",
		annotatedClasses = {NotAuditedDynamicMapComponent.class}
)
public class NotAuditedDynamicComponentTest {

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			NotAuditedDynamicMapComponent entity = new NotAuditedDynamicMapComponent( 1L, "static field value" );
			entity.getCustomFields().put( "prop1", 13 );
			entity.getCustomFields().put( "prop2", 0.1f );
			em.persist( entity );
		} );

		// No revision
		scope.inTransaction( em -> {
			NotAuditedDynamicMapComponent entity = em.find( NotAuditedDynamicMapComponent.class, 1L );
			entity.getCustomFields().put( "prop1", 0 );
			em.merge( entity );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			NotAuditedDynamicMapComponent entity = em.find( NotAuditedDynamicMapComponent.class, 1L );
			entity.setNote( "updated note" );
			em.merge( entity );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			NotAuditedDynamicMapComponent entity = em.getReference( NotAuditedDynamicMapComponent.class, 1L );
			em.remove( entity );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals(
					Arrays.asList( 1, 2, 3 ),
					AuditReaderFactory.get( em ).getRevisions( NotAuditedDynamicMapComponent.class, 1L )
			);
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			// Revision 1
			NotAuditedDynamicMapComponent entity = new NotAuditedDynamicMapComponent( 1L, "static field value" );
			NotAuditedDynamicMapComponent ver1 = auditReader.find(
					NotAuditedDynamicMapComponent.class,
					entity.getId(),
					1
			);
			assertEquals( entity, ver1 );
			// Assume empty NotAuditedDynamicMapComponent#customFields map, because dynamic-component is not audited.
			assertTrue( ver1.getCustomFields().isEmpty() );

			// Revision 2
			entity.setNote( "updated note" );
			NotAuditedDynamicMapComponent ver2 = auditReader.find(
					NotAuditedDynamicMapComponent.class,
					entity.getId(),
					2
			);
			assertEquals( entity, ver2 );
			// Assume empty NotAuditedDynamicMapComponent#customFields map, because dynamic-component is not audited.
			assertTrue( ver2.getCustomFields().isEmpty() );
		} );
	}
}
