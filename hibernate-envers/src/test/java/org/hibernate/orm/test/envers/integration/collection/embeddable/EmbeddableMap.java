/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.embeddable;

import java.util.Arrays;
import java.util.Collections;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.collection.EmbeddableMapEntity;
import org.hibernate.orm.test.envers.entities.components.Component3;
import org.hibernate.orm.test.envers.entities.components.Component4;
import org.hibernate.orm.test.envers.tools.TestTools;
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
@Jpa(annotatedClasses = {EmbeddableMapEntity.class})
public class EmbeddableMap {
	private Integer eme1_id = null;
	private Integer eme2_id = null;

	private final Component4 c4_1 = new Component4( "c41", "c41_value", "c41_description" );
	private final Component4 c4_2 = new Component4( "c42", "c42_value2", "c42_description" );
	private final Component3 c3_1 = new Component3( "c31", c4_1, c4_2 );
	private final Component3 c3_2 = new Component3( "c32", c4_1, c4_2 );

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 (eme1: initialy empty, eme2: initialy 1 mapping)
		scope.inTransaction( em -> {
			EmbeddableMapEntity eme1 = new EmbeddableMapEntity();
			EmbeddableMapEntity eme2 = new EmbeddableMapEntity();
			eme2.getComponentMap().put( "1", c3_1 );
			em.persist( eme1 );
			em.persist( eme2 );
			eme1_id = eme1.getId();
			eme2_id = eme2.getId();
		} );

		// Revision 2 (eme1: adding 2 mappings, eme2: no changes)
		scope.inTransaction( em -> {
			EmbeddableMapEntity eme1 = em.find( EmbeddableMapEntity.class, eme1_id );
			eme1.getComponentMap().put( "1", c3_1 );
			eme1.getComponentMap().put( "2", c3_2 );
		} );

		// Revision 3 (eme1: removing an existing mapping, eme2: replacing a value)
		scope.inTransaction( em -> {
			EmbeddableMapEntity eme1 = em.find( EmbeddableMapEntity.class, eme1_id );
			EmbeddableMapEntity eme2 = em.find( EmbeddableMapEntity.class, eme2_id );
			eme1.getComponentMap().remove( "1" );
			eme2.getComponentMap().put( "1", c3_2 );
		} );

		// No revision (eme1: removing a non-existing mapping, eme2: replacing with the same value)
		scope.inTransaction( em -> {
			EmbeddableMapEntity eme1 = em.find( EmbeddableMapEntity.class, eme1_id );
			EmbeddableMapEntity eme2 = em.find( EmbeddableMapEntity.class, eme2_id );
			eme1.getComponentMap().remove( "3" );
			eme2.getComponentMap().put( "1", c3_2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals(
					Arrays.asList( 1, 2, 3 ),
					auditReader.getRevisions( EmbeddableMapEntity.class, eme1_id )
			);
			assertEquals(
					Arrays.asList( 1, 3 ),
					auditReader.getRevisions( EmbeddableMapEntity.class, eme2_id )
			);
		} );
	}

	@Test
	public void testHistoryOfEme1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			EmbeddableMapEntity rev1 = auditReader.find( EmbeddableMapEntity.class, eme1_id, 1 );
			EmbeddableMapEntity rev2 = auditReader.find( EmbeddableMapEntity.class, eme1_id, 2 );
			EmbeddableMapEntity rev3 = auditReader.find( EmbeddableMapEntity.class, eme1_id, 3 );
			EmbeddableMapEntity rev4 = auditReader.find( EmbeddableMapEntity.class, eme1_id, 4 );

			assertEquals( Collections.EMPTY_MAP, rev1.getComponentMap() );
			assertEquals( TestTools.makeMap( "1", c3_1, "2", c3_2 ), rev2.getComponentMap() );
			assertEquals( TestTools.makeMap( "2", c3_2 ), rev3.getComponentMap() );
			assertEquals( TestTools.makeMap( "2", c3_2 ), rev4.getComponentMap() );
		} );
	}

	@Test
	public void testHistoryOfEme2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			EmbeddableMapEntity rev1 = auditReader.find( EmbeddableMapEntity.class, eme2_id, 1 );
			EmbeddableMapEntity rev2 = auditReader.find( EmbeddableMapEntity.class, eme2_id, 2 );
			EmbeddableMapEntity rev3 = auditReader.find( EmbeddableMapEntity.class, eme2_id, 3 );
			EmbeddableMapEntity rev4 = auditReader.find( EmbeddableMapEntity.class, eme2_id, 4 );

			assertEquals( TestTools.makeMap( "1", c3_1 ), rev1.getComponentMap() );
			assertEquals( TestTools.makeMap( "1", c3_1 ), rev2.getComponentMap() );
			assertEquals( TestTools.makeMap( "1", c3_2 ), rev3.getComponentMap() );
			assertEquals( TestTools.makeMap( "1", c3_2 ), rev4.getComponentMap() );
		} );
	}
}
