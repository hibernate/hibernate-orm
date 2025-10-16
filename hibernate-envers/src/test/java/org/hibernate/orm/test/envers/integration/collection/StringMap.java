/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection;

import java.util.Arrays;
import java.util.Collections;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.collection.StringMapEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {StringMapEntity.class})
public class StringMap {
	private Integer sme1_id;
	private Integer sme2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		StringMapEntity sme1 = new StringMapEntity();
		StringMapEntity sme2 = new StringMapEntity();

		// Revision 1 (sme1: initialy empty, sme2: initialy 1 mapping)
		scope.inTransaction( em -> {
			sme2.getStrings().put( "1", "a" );

			em.persist( sme1 );
			em.persist( sme2 );
		} );

		// Revision 2 (sme1: adding 2 mappings, sme2: no changes)
		scope.inTransaction( em -> {
			StringMapEntity sme1Ref = em.find( StringMapEntity.class, sme1.getId() );

			sme1Ref.getStrings().put( "1", "a" );
			sme1Ref.getStrings().put( "2", "b" );
		} );

		// Revision 3 (sme1: removing an existing mapping, sme2: replacing a value)
		scope.inTransaction( em -> {
			StringMapEntity sme1Ref = em.find( StringMapEntity.class, sme1.getId() );
			StringMapEntity sme2Ref = em.find( StringMapEntity.class, sme2.getId() );

			sme1Ref.getStrings().remove( "1" );
			sme2Ref.getStrings().put( "1", "b" );
		} );

		// No revision (sme1: removing a non-existing mapping, sme2: replacing with the same value)
		scope.inTransaction( em -> {
			StringMapEntity sme1Ref = em.find( StringMapEntity.class, sme1.getId() );
			StringMapEntity sme2Ref = em.find( StringMapEntity.class, sme2.getId() );

			sme1Ref.getStrings().remove( "3" );
			sme2Ref.getStrings().put( "1", "b" );
		} );

		sme1_id = sme1.getId();
		sme2_id = sme2.getId();
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( StringMapEntity.class, sme1_id ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( StringMapEntity.class, sme2_id ) );
		} );
	}

	@Test
	public void testHistoryOfSse1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			StringMapEntity rev1 = auditReader.find( StringMapEntity.class, sme1_id, 1 );
			StringMapEntity rev2 = auditReader.find( StringMapEntity.class, sme1_id, 2 );
			StringMapEntity rev3 = auditReader.find( StringMapEntity.class, sme1_id, 3 );
			StringMapEntity rev4 = auditReader.find( StringMapEntity.class, sme1_id, 4 );

			assertEquals( Collections.EMPTY_MAP, rev1.getStrings() );
			assertEquals( TestTools.makeMap( "1", "a", "2", "b" ), rev2.getStrings() );
			assertEquals( TestTools.makeMap( "2", "b" ), rev3.getStrings() );
			assertEquals( TestTools.makeMap( "2", "b" ), rev4.getStrings() );
		} );
	}

	@Test
	public void testHistoryOfSse2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			StringMapEntity rev1 = auditReader.find( StringMapEntity.class, sme2_id, 1 );
			StringMapEntity rev2 = auditReader.find( StringMapEntity.class, sme2_id, 2 );
			StringMapEntity rev3 = auditReader.find( StringMapEntity.class, sme2_id, 3 );
			StringMapEntity rev4 = auditReader.find( StringMapEntity.class, sme2_id, 4 );

			assertEquals( TestTools.makeMap( "1", "a" ), rev1.getStrings() );
			assertEquals( TestTools.makeMap( "1", "a" ), rev2.getStrings() );
			assertEquals( TestTools.makeMap( "1", "b" ), rev3.getStrings() );
			assertEquals( TestTools.makeMap( "1", "b" ), rev4.getStrings() );
		} );
	}
}
