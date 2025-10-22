/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.embeddable;

import java.util.Arrays;
import java.util.Collections;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.collection.EmbeddableListEntity1;
import org.hibernate.orm.test.envers.entities.components.Component3;
import org.hibernate.orm.test.envers.entities.components.Component4;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Kristoffer Lundberg (kristoffer at cambio dot se)
 */
@JiraKey(value = "HHH-6613")
@EnversTest
@Jpa(annotatedClasses = {EmbeddableListEntity1.class})
public class EmbeddableList1 {
	private Integer ele1_id = null;

	private final Component4 c4_1 = new Component4( "c41", "c41_value", "c41_description" );
	private final Component4 c4_2 = new Component4( "c42", "c42_value2", "c42_description" );
	private final Component3 c3_1 = new Component3( "c31", c4_1, c4_2 );
	private final Component3 c3_2 = new Component3( "c32", c4_1, c4_2 );

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 (ele1: initially 1 element in both collections)
		scope.inTransaction( em -> {
			EmbeddableListEntity1 ele1 = new EmbeddableListEntity1();
			ele1.getComponentList().add( c3_1 );
			em.persist( ele1 );
			ele1_id = ele1.getId();
		} );

		// Revision (still 1) (ele1: removing non-existing element)
		scope.inTransaction( em -> {
			EmbeddableListEntity1 ele1 = em.find( EmbeddableListEntity1.class, ele1_id );
			ele1.getComponentList().remove( c3_2 );
		} );

		// Revision 2 (ele1: adding one element)
		scope.inTransaction( em -> {
			EmbeddableListEntity1 ele1 = em.find( EmbeddableListEntity1.class, ele1_id );
			ele1.getComponentList().add( c3_2 );
		} );

		// Revision 3 (ele1: adding one existing element)
		scope.inTransaction( em -> {
			EmbeddableListEntity1 ele1 = em.find( EmbeddableListEntity1.class, ele1_id );
			ele1.getComponentList().add( c3_1 );
		} );

		// Revision 4 (ele1: removing one existing element)
		scope.inTransaction( em -> {
			EmbeddableListEntity1 ele1 = em.find( EmbeddableListEntity1.class, ele1_id );
			ele1.getComponentList().remove( c3_2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals(
					Arrays.asList( 1, 2, 3, 4 ),
					auditReader.getRevisions( EmbeddableListEntity1.class, ele1_id )
			);
		} );
	}

	@Test
	public void testHistoryOfEle1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			EmbeddableListEntity1 rev1 = auditReader.find( EmbeddableListEntity1.class, ele1_id, 1 );
			EmbeddableListEntity1 rev2 = auditReader.find( EmbeddableListEntity1.class, ele1_id, 2 );
			EmbeddableListEntity1 rev3 = auditReader.find( EmbeddableListEntity1.class, ele1_id, 3 );
			EmbeddableListEntity1 rev4 = auditReader.find( EmbeddableListEntity1.class, ele1_id, 4 );

			assertEquals( Collections.singletonList( c3_1 ), rev1.getComponentList() );
			assertEquals( Arrays.asList( c3_1, c3_2 ), rev2.getComponentList() );
			assertEquals( Arrays.asList( c3_1, c3_2, c3_1 ), rev3.getComponentList() );
			assertEquals( Arrays.asList( c3_1, c3_1 ), rev4.getComponentList() );
		} );
	}
}
