/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.joined.childrelation;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
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
@Jpa(annotatedClasses = {ChildIngEntity.class, ParentNotIngEntity.class, ReferencedEntity.class})
public class ChildReferencing {
	private Integer re_id1;
	private Integer re_id2;
	private Integer c_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		re_id1 = 1;
		re_id2 = 10;
		c_id = 100;

		// Rev 1
		scope.inTransaction( em -> {
			ReferencedEntity re1 = new ReferencedEntity( re_id1 );
			em.persist( re1 );

			ReferencedEntity re2 = new ReferencedEntity( re_id2 );
			em.persist( re2 );
		} );

		// Rev 2
		scope.inTransaction( em -> {
			ReferencedEntity re1 = em.find( ReferencedEntity.class, re_id1 );

			ChildIngEntity cie = new ChildIngEntity( c_id, "y", 1l );
			cie.setReferenced( re1 );
			em.persist( cie );
			c_id = cie.getId();
		} );

		// Rev 3
		scope.inTransaction( em -> {
			ReferencedEntity re2 = em.find( ReferencedEntity.class, re_id2 );
			ChildIngEntity cie = em.find( ChildIngEntity.class, c_id );

			cie.setReferenced( re2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( ReferencedEntity.class, re_id1 ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( ReferencedEntity.class, re_id2 ) );
			assertEquals( Arrays.asList( 2, 3 ), auditReader.getRevisions( ChildIngEntity.class, c_id ) );
		} );
	}

	@Test
	public void testHistoryOfReferencedCollection1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( 0, auditReader.find( ReferencedEntity.class, re_id1, 1 ).getReferencing().size() );
			assertEquals(
					TestTools.makeSet( new ChildIngEntity( c_id, "y", 1l ) ),
					auditReader.find( ReferencedEntity.class, re_id1, 2 ).getReferencing()
			);
			assertEquals( 0, auditReader.find( ReferencedEntity.class, re_id1, 3 ).getReferencing().size() );
		} );
	}

	@Test
	public void testHistoryOfReferencedCollection2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( 0, auditReader.find( ReferencedEntity.class, re_id2, 1 ).getReferencing().size() );
			assertEquals( 0, auditReader.find( ReferencedEntity.class, re_id2, 2 ).getReferencing().size() );
			assertEquals(
					TestTools.makeSet( new ChildIngEntity( c_id, "y", 1l ) ),
					auditReader.find( ReferencedEntity.class, re_id2, 3 ).getReferencing()
			);
		} );
	}

	@Test
	public void testChildHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( null, auditReader.find( ChildIngEntity.class, c_id, 1 ) );
			assertEquals(
					new ReferencedEntity( re_id1 ),
					auditReader.find( ChildIngEntity.class, c_id, 2 ).getReferenced()
			);
			assertEquals(
					new ReferencedEntity( re_id2 ),
					auditReader.find( ChildIngEntity.class, c_id, 3 ).getReferenced()
			);
		} );
	}
}
