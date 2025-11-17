/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection;

import java.util.Arrays;
import java.util.Collections;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.collection.StringListEntity;
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
@Jpa(annotatedClasses = {StringListEntity.class})
public class StringList {
	private Integer sle1_id;
	private Integer sle2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 (sle1: initialy empty, sle2: initialy 2 elements)
		scope.inTransaction( em -> {
			StringListEntity sle1 = new StringListEntity();
			StringListEntity sle2 = new StringListEntity();

			sle2.getStrings().add( "sle2_string1" );
			sle2.getStrings().add( "sle2_string2" );

			em.persist( sle1 );
			em.persist( sle2 );

			sle1_id = sle1.getId();
			sle2_id = sle2.getId();
		} );

		// Revision 2 (sle1: adding 2 elements, sle2: adding an existing element)
		scope.inTransaction( em -> {
			StringListEntity sle1 = em.find( StringListEntity.class, sle1_id );
			StringListEntity sle2 = em.find( StringListEntity.class, sle2_id );

			sle1.getStrings().add( "sle1_string1" );
			sle1.getStrings().add( "sle1_string2" );

			sle2.getStrings().add( "sle2_string1" );
		} );

		// Revision 3 (sle1: replacing an element at index 0, sle2: removing an element at index 0)
		scope.inTransaction( em -> {
			StringListEntity sle1 = em.find( StringListEntity.class, sle1_id );
			StringListEntity sle2 = em.find( StringListEntity.class, sle2_id );

			sle1.getStrings().set( 0, "sle1_string3" );

			sle2.getStrings().remove( 0 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( StringListEntity.class, sle1_id ) );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( StringListEntity.class, sle2_id ) );
		} );
	}

	@Test
	public void testHistoryOfSle1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			StringListEntity rev1 = auditReader.find( StringListEntity.class, sle1_id, 1 );
			StringListEntity rev2 = auditReader.find( StringListEntity.class, sle1_id, 2 );
			StringListEntity rev3 = auditReader.find( StringListEntity.class, sle1_id, 3 );

			assertEquals( Collections.EMPTY_LIST, rev1.getStrings() );
			assertEquals( TestTools.makeList( "sle1_string1", "sle1_string2" ), rev2.getStrings() );
			assertEquals( TestTools.makeList( "sle1_string3", "sle1_string2" ), rev3.getStrings() );
		} );
	}

	@Test
	public void testHistoryOfSse2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			StringListEntity rev1 = auditReader.find( StringListEntity.class, sle2_id, 1 );
			StringListEntity rev2 = auditReader.find( StringListEntity.class, sle2_id, 2 );
			StringListEntity rev3 = auditReader.find( StringListEntity.class, sle2_id, 3 );

			assertEquals( TestTools.makeList( "sle2_string1", "sle2_string2" ), rev1.getStrings() );
			assertEquals( TestTools.makeList( "sle2_string1", "sle2_string2", "sle2_string1" ), rev2.getStrings() );
			assertEquals( TestTools.makeList( "sle2_string2", "sle2_string1" ), rev3.getStrings() );
		} );
	}
}
