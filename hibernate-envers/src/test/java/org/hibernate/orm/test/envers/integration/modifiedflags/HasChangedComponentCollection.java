/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.collection.EmbeddableListEntity1;
import org.hibernate.orm.test.envers.entities.components.Component3;
import org.hibernate.orm.test.envers.entities.components.Component4;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-6613")
@Jpa(integrationSettings = @Setting(name = EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, value = "true"),
		annotatedClasses = {EmbeddableListEntity1.class})
public class HasChangedComponentCollection extends AbstractModifiedFlagsEntityTest {
	private Integer ele1_id = null;

	private final Component4 c4_1 = new Component4( "c41", "c41_value", "c41_description" );
	private final Component4 c4_2 = new Component4( "c42", "c42_value2", "c42_description" );
	private final Component3 c3_1 = new Component3( "c31", c4_1, c4_2 );
	private final Component3 c3_2 = new Component3( "c32", c4_1, c4_2 );

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 (ele1: initially 1 element in both collections)
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			EmbeddableListEntity1 ele1 = new EmbeddableListEntity1();
			ele1.setOtherData( "data" );
			ele1.getComponentList().add( c3_1 );
			em.persist( ele1 );
			em.getTransaction().commit();
			ele1_id = ele1.getId();
		} );

		// Revision (still 1) (ele1: removing non-existing element)
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			EmbeddableListEntity1 ele1 = em.find( EmbeddableListEntity1.class, ele1_id );
			ele1.getComponentList().remove( c3_2 );
			em.getTransaction().commit();
		} );

		// Revision 2 (ele1: updating singular property and removing non-existing element)
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			EmbeddableListEntity1 ele1 = em.find( EmbeddableListEntity1.class, ele1_id );
			ele1.setOtherData( "modified" );
			ele1.getComponentList().remove( c3_2 );
			ele1 = em.merge( ele1 );
			em.getTransaction().commit();
		} );

		// Revision 3 (ele1: adding one element)
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			EmbeddableListEntity1 ele1 = em.find( EmbeddableListEntity1.class, ele1_id );
			ele1.getComponentList().add( c3_2 );
			em.getTransaction().commit();
		} );

		// Revision 4 (ele1: adding one existing element)
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			EmbeddableListEntity1 ele1 = em.find( EmbeddableListEntity1.class, ele1_id );
			ele1.getComponentList().add( c3_1 );
			em.getTransaction().commit();
		} );

		// Revision 5 (ele1: removing one existing element)
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			EmbeddableListEntity1 ele1 = em.find( EmbeddableListEntity1.class, ele1_id );
			ele1.getComponentList().remove( c3_2 );
			em.getTransaction().commit();
		} );

		// Revision 6 (ele1: changing singular property only)
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			EmbeddableListEntity1 ele1 = em.find( EmbeddableListEntity1.class, ele1_id );
			ele1.setOtherData( "another modification" );
			ele1 = em.merge( ele1 );
			em.getTransaction().commit();
		} );
	}

	@Test
	public void testHasChangedEle(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged( auditReader, EmbeddableListEntity1.class, ele1_id, "componentList" );
			assertEquals( 4, list.size() );
			assertEquals( makeList( 1, 3, 4, 5 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged( auditReader, EmbeddableListEntity1.class, ele1_id, "otherData" );
			assertEquals( 3, list.size() );
			assertEquals( makeList( 1, 2, 6 ), extractRevisionNumbers( list ) );
		} );
	}
}
