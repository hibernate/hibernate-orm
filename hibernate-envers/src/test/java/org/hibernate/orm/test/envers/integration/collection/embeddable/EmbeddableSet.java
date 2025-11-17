/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.embeddable;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.collection.EmbeddableSetEntity;
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
@Jpa(annotatedClasses = {EmbeddableSetEntity.class})
public class EmbeddableSet {
	private Integer ese1_id = null;

	private final Component4 c4_1 = new Component4( "c41", "c41_value", "c41_description" );
	private final Component4 c4_2 = new Component4( "c42", "c42_value2", "c42_description" );
	private final Component3 c3_1 = new Component3( "c31", c4_1, c4_2 );
	private final Component3 c3_2 = new Component3( "c32", c4_1, c4_2 );
	private final Component3 c3_3 = new Component3( "c33", c4_1, c4_2 );
	private final Component3 c3_4 = new Component3( "c34", c4_1, c4_2 );

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 (ese1: initially two elements)
		scope.inTransaction( em -> {
			EmbeddableSetEntity ese1 = new EmbeddableSetEntity();
			ese1.getComponentSet().add( c3_1 );
			ese1.getComponentSet().add( c3_3 );
			em.persist( ese1 );
			ese1_id = ese1.getId();
		} );

		// Revision (still 1) (ese1: removing non-existing element)
		scope.inTransaction( em -> {
			EmbeddableSetEntity ese1 = em.find( EmbeddableSetEntity.class, ese1_id );
			ese1.getComponentSet().remove( c3_2 );
		} );

		// Revision 2 (ese1: adding one element)
		scope.inTransaction( em -> {
			EmbeddableSetEntity ese1 = em.find( EmbeddableSetEntity.class, ese1_id );
			ese1.getComponentSet().add( c3_2 );
		} );

		// Revision (still 2) (ese1: adding one existing element)
		scope.inTransaction( em -> {
			EmbeddableSetEntity ese1 = em.find( EmbeddableSetEntity.class, ese1_id );
			ese1.getComponentSet().add( c3_1 );
		} );

		// Revision 3 (ese1: removing one existing element)
		scope.inTransaction( em -> {
			EmbeddableSetEntity ese1 = em.find( EmbeddableSetEntity.class, ese1_id );
			ese1.getComponentSet().remove( c3_2 );
		} );

		// Revision 4 (ese1: adding two elements)
		scope.inTransaction( em -> {
			EmbeddableSetEntity ese1 = em.find( EmbeddableSetEntity.class, ese1_id );
			ese1.getComponentSet().add( c3_2 );
			ese1.getComponentSet().add( c3_4 );
		} );

		// Revision 5 (ese1: removing two elements)
		scope.inTransaction( em -> {
			EmbeddableSetEntity ese1 = em.find( EmbeddableSetEntity.class, ese1_id );
			ese1.getComponentSet().remove( c3_2 );
			ese1.getComponentSet().remove( c3_4 );
		} );

		// Revision 6 (ese1: removing and adding two elements)
		scope.inTransaction( em -> {
			EmbeddableSetEntity ese1 = em.find( EmbeddableSetEntity.class, ese1_id );
			ese1.getComponentSet().remove( c3_1 );
			ese1.getComponentSet().remove( c3_3 );
			ese1.getComponentSet().add( c3_2 );
			ese1.getComponentSet().add( c3_4 );
		} );

		// Revision 7 (ese1: adding one element)
		scope.inTransaction( em -> {
			EmbeddableSetEntity ese1 = em.find( EmbeddableSetEntity.class, ese1_id );
			ese1.getComponentSet().add( c3_1 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals(
					Arrays.asList( 1, 2, 3, 4, 5, 6, 7 ),
					auditReader.getRevisions( EmbeddableSetEntity.class, ese1_id )
			);
		} );
	}

	@Test
	public void testHistoryOfEse1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			EmbeddableSetEntity rev1 = auditReader.find( EmbeddableSetEntity.class, ese1_id, 1 );
			EmbeddableSetEntity rev2 = auditReader.find( EmbeddableSetEntity.class, ese1_id, 2 );
			EmbeddableSetEntity rev3 = auditReader.find( EmbeddableSetEntity.class, ese1_id, 3 );
			EmbeddableSetEntity rev4 = auditReader.find( EmbeddableSetEntity.class, ese1_id, 4 );
			EmbeddableSetEntity rev5 = auditReader.find( EmbeddableSetEntity.class, ese1_id, 5 );
			EmbeddableSetEntity rev6 = auditReader.find( EmbeddableSetEntity.class, ese1_id, 6 );
			EmbeddableSetEntity rev7 = auditReader.find( EmbeddableSetEntity.class, ese1_id, 7 );

			assertEquals( TestTools.makeSet( c3_1, c3_3 ), rev1.getComponentSet() );
			assertEquals( TestTools.makeSet( c3_1, c3_2, c3_3 ), rev2.getComponentSet() );
			assertEquals( TestTools.makeSet( c3_1, c3_3 ), rev3.getComponentSet() );
			assertEquals( TestTools.makeSet( c3_1, c3_2, c3_3, c3_4 ), rev4.getComponentSet() );
			assertEquals( TestTools.makeSet( c3_1, c3_3 ), rev5.getComponentSet() );
			assertEquals( TestTools.makeSet( c3_2, c3_4 ), rev6.getComponentSet() );
			assertEquals( TestTools.makeSet( c3_2, c3_4, c3_1 ), rev7.getComponentSet() );
		} );
	}
}
