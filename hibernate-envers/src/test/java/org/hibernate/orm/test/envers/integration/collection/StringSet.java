/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection;

import java.util.Arrays;
import java.util.Collections;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.collection.StringSetEntity;
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
@Jpa(annotatedClasses = {StringSetEntity.class})
public class StringSet {
	private Integer sse1_id;
	private Integer sse2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		StringSetEntity sse1 = new StringSetEntity();
		StringSetEntity sse2 = new StringSetEntity();

		// Revision 1 (sse1: initialy empty, sse2: initialy 2 elements)
		scope.inTransaction( em -> {
			sse2.getStrings().add( "sse2_string1" );
			sse2.getStrings().add( "sse2_string2" );

			em.persist( sse1 );
			em.persist( sse2 );
		} );

		// Revision 2 (sse1: adding 2 elements, sse2: adding an existing element)
		scope.inTransaction( em -> {
			StringSetEntity sse1Ref = em.find( StringSetEntity.class, sse1.getId() );
			StringSetEntity sse2Ref = em.find( StringSetEntity.class, sse2.getId() );

			sse1Ref.getStrings().add( "sse1_string1" );
			sse1Ref.getStrings().add( "sse1_string2" );

			sse2Ref.getStrings().add( "sse2_string1" );
		} );

		// Revision 3 (sse1: removing a non-existing element, sse2: removing one element)
		scope.inTransaction( em -> {
			StringSetEntity sse1Ref = em.find( StringSetEntity.class, sse1.getId() );
			StringSetEntity sse2Ref = em.find( StringSetEntity.class, sse2.getId() );

			sse1Ref.getStrings().remove( "sse1_string3" );
			sse2Ref.getStrings().remove( "sse2_string1" );
		} );

		sse1_id = sse1.getId();
		sse2_id = sse2.getId();
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( StringSetEntity.class, sse1_id ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( StringSetEntity.class, sse2_id ) );
		} );
	}

	@Test
	public void testHistoryOfSse1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			StringSetEntity rev1 = auditReader.find( StringSetEntity.class, sse1_id, 1 );
			StringSetEntity rev2 = auditReader.find( StringSetEntity.class, sse1_id, 2 );
			StringSetEntity rev3 = auditReader.find( StringSetEntity.class, sse1_id, 3 );

			assertEquals( Collections.EMPTY_SET, rev1.getStrings() );
			assertEquals( TestTools.makeSet( "sse1_string1", "sse1_string2" ), rev2.getStrings() );
			assertEquals( TestTools.makeSet( "sse1_string1", "sse1_string2" ), rev3.getStrings() );
		} );
	}

	@Test
	public void testHistoryOfSse2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			StringSetEntity rev1 = auditReader.find( StringSetEntity.class, sse2_id, 1 );
			StringSetEntity rev2 = auditReader.find( StringSetEntity.class, sse2_id, 2 );
			StringSetEntity rev3 = auditReader.find( StringSetEntity.class, sse2_id, 3 );

			assertEquals( TestTools.makeSet( "sse2_string1", "sse2_string2" ), rev1.getStrings() );
			assertEquals( TestTools.makeSet( "sse2_string1", "sse2_string2" ), rev2.getStrings() );
			assertEquals( TestTools.makeSet( "sse2_string2" ), rev3.getStrings() );
		} );
	}
}
