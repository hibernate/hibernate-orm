/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.naming.ids;

import java.util.Arrays;
import java.util.Iterator;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.mapping.Selectable;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.hibernate.testing.orm.junit.SessionFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@DomainModel(annotatedClasses = {JoinMulIdNamingRefEdEntity.class, JoinMulIdNamingRefIngEntity.class})
@SessionFactory
public class JoinMulIdNaming {
	private MulIdNaming ed_id1;
	private MulIdNaming ed_id2;
	private MulIdNaming ing_id1;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		ed_id1 = new MulIdNaming( 10, 20 );
		ed_id2 = new MulIdNaming( 11, 21 );
		ing_id1 = new MulIdNaming( 12, 22 );

		// Revision 1
		scope.inTransaction( em -> {
			JoinMulIdNamingRefEdEntity ed1 = new JoinMulIdNamingRefEdEntity( ed_id1, "data1" );
			JoinMulIdNamingRefEdEntity ed2 = new JoinMulIdNamingRefEdEntity( ed_id2, "data2" );
			JoinMulIdNamingRefIngEntity ing1 = new JoinMulIdNamingRefIngEntity( ing_id1, "x", ed1 );

			em.persist( ed1 );
			em.persist( ed2 );
			em.persist( ing1 );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			JoinMulIdNamingRefEdEntity ed2 = em.find( JoinMulIdNamingRefEdEntity.class, ed_id2 );
			JoinMulIdNamingRefIngEntity ing1 = em.find( JoinMulIdNamingRefIngEntity.class, ing_id1 );
			ing1.setData( "y" );
			ing1.setReference( ed2 );
		} );
	}

	@Test
	public void testRevisionsCounts(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ),
					auditReader.getRevisions( JoinMulIdNamingRefEdEntity.class, ed_id1 ) );
			assertEquals( Arrays.asList( 1, 2 ),
					auditReader.getRevisions( JoinMulIdNamingRefEdEntity.class, ed_id2 ) );
			assertEquals( Arrays.asList( 1, 2 ),
					auditReader.getRevisions( JoinMulIdNamingRefIngEntity.class, ing_id1 ) );
		} );
	}

	@Test
	public void testHistoryOfEdId1(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			JoinMulIdNamingRefEdEntity ver1 = new JoinMulIdNamingRefEdEntity( ed_id1, "data1" );

			assertEquals( ver1, auditReader.find( JoinMulIdNamingRefEdEntity.class, ed_id1, 1 ) );
			assertEquals( ver1, auditReader.find( JoinMulIdNamingRefEdEntity.class, ed_id1, 2 ) );
		} );
	}

	@Test
	public void testHistoryOfEdId2(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			JoinMulIdNamingRefEdEntity ver1 = new JoinMulIdNamingRefEdEntity( ed_id2, "data2" );

			assertEquals( ver1, auditReader.find( JoinMulIdNamingRefEdEntity.class, ed_id2, 1 ) );
			assertEquals( ver1, auditReader.find( JoinMulIdNamingRefEdEntity.class, ed_id2, 2 ) );
		} );
	}

	@Test
	public void testHistoryOfIngId1(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			JoinMulIdNamingRefIngEntity ver1 = new JoinMulIdNamingRefIngEntity( ing_id1, "x", null );
			JoinMulIdNamingRefIngEntity ver2 = new JoinMulIdNamingRefIngEntity( ing_id1, "y", null );

			assertEquals( ver1, auditReader.find( JoinMulIdNamingRefIngEntity.class, ing_id1, 1 ) );
			assertEquals( ver2, auditReader.find( JoinMulIdNamingRefIngEntity.class, ing_id1, 2 ) );

			assertEquals( new JoinMulIdNamingRefEdEntity( ed_id1, "data1" ),
					auditReader.find( JoinMulIdNamingRefIngEntity.class, ing_id1, 1 ).getReference() );
			assertEquals( new JoinMulIdNamingRefEdEntity( ed_id2, "data2" ),
					auditReader.find( JoinMulIdNamingRefIngEntity.class, ing_id1, 2 ).getReference() );
		} );
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testJoinColumnNames(DomainModelScope scope) {
		Iterator<Selectable> columns = scope.getDomainModel().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.naming.ids.JoinMulIdNamingRefIngEntity_AUD"
		).getProperty( "reference_id1" ).getSelectables().iterator();

		assertTrue( columns.hasNext() );
		assertEquals( "ID1_reference", columns.next().getText() );
		assertFalse( columns.hasNext() );

		columns = scope.getDomainModel().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.naming.ids.JoinMulIdNamingRefIngEntity_AUD"
		).getProperty( "reference_id2" ).getSelectables().iterator();

		assertTrue( columns.hasNext() );
		assertEquals( "ID2_reference", columns.next().getText() );
		assertFalse( columns.hasNext() );
	}
}
