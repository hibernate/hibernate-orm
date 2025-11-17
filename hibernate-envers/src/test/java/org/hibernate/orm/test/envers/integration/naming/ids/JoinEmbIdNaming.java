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
@DomainModel(annotatedClasses = {JoinEmbIdNamingRefEdEntity.class, JoinEmbIdNamingRefIngEntity.class})
@SessionFactory
public class JoinEmbIdNaming {
	private EmbIdNaming ed_id1;
	private EmbIdNaming ed_id2;
	private EmbIdNaming ing_id1;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		ed_id1 = new EmbIdNaming( 10, 20 );
		ed_id2 = new EmbIdNaming( 11, 21 );
		ing_id1 = new EmbIdNaming( 12, 22 );

		// Revision 1
		scope.inTransaction( em -> {
			JoinEmbIdNamingRefEdEntity ed1 = new JoinEmbIdNamingRefEdEntity( ed_id1, "data1" );
			JoinEmbIdNamingRefEdEntity ed2 = new JoinEmbIdNamingRefEdEntity( ed_id2, "data2" );
			JoinEmbIdNamingRefIngEntity ing1 = new JoinEmbIdNamingRefIngEntity( ing_id1, "x", ed1 );

			em.persist( ed1 );
			em.persist( ed2 );
			em.persist( ing1 );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			JoinEmbIdNamingRefEdEntity ed2 = em.find( JoinEmbIdNamingRefEdEntity.class, ed_id2 );
			JoinEmbIdNamingRefIngEntity ing1 = em.find( JoinEmbIdNamingRefIngEntity.class, ing_id1 );
			ing1.setData( "y" );
			ing1.setReference( ed2 );
		} );
	}

	@Test
	public void testRevisionsCounts(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ),
					auditReader.getRevisions( JoinEmbIdNamingRefEdEntity.class, ed_id1 ) );
			assertEquals( Arrays.asList( 1, 2 ),
					auditReader.getRevisions( JoinEmbIdNamingRefEdEntity.class, ed_id2 ) );
			assertEquals( Arrays.asList( 1, 2 ),
					auditReader.getRevisions( JoinEmbIdNamingRefIngEntity.class, ing_id1 ) );
		} );
	}

	@Test
	public void testHistoryOfEdId1(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			JoinEmbIdNamingRefEdEntity ver1 = new JoinEmbIdNamingRefEdEntity( ed_id1, "data1" );

			assertEquals( ver1, auditReader.find( JoinEmbIdNamingRefEdEntity.class, ed_id1, 1 ) );
			assertEquals( ver1, auditReader.find( JoinEmbIdNamingRefEdEntity.class, ed_id1, 2 ) );
		} );
	}

	@Test
	public void testHistoryOfEdId2(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			JoinEmbIdNamingRefEdEntity ver1 = new JoinEmbIdNamingRefEdEntity( ed_id2, "data2" );

			assertEquals( ver1, auditReader.find( JoinEmbIdNamingRefEdEntity.class, ed_id2, 1 ) );
			assertEquals( ver1, auditReader.find( JoinEmbIdNamingRefEdEntity.class, ed_id2, 2 ) );
		} );
	}

	@Test
	public void testHistoryOfIngId1(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			JoinEmbIdNamingRefIngEntity ver1 = new JoinEmbIdNamingRefIngEntity( ing_id1, "x", null );
			JoinEmbIdNamingRefIngEntity ver2 = new JoinEmbIdNamingRefIngEntity( ing_id1, "y", null );

			assertEquals( ver1, auditReader.find( JoinEmbIdNamingRefIngEntity.class, ing_id1, 1 ) );
			assertEquals( ver2, auditReader.find( JoinEmbIdNamingRefIngEntity.class, ing_id1, 2 ) );

			assertEquals( new JoinEmbIdNamingRefEdEntity( ed_id1, "data1" ),
					auditReader.find( JoinEmbIdNamingRefIngEntity.class, ing_id1, 1 ).getReference() );
			assertEquals( new JoinEmbIdNamingRefEdEntity( ed_id2, "data2" ),
					auditReader.find( JoinEmbIdNamingRefIngEntity.class, ing_id1, 2 ).getReference() );
		} );
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testJoinColumnNames(DomainModelScope scope) {
		Iterator<Selectable> columns = scope.getDomainModel().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.naming.ids.JoinEmbIdNamingRefIngEntity_AUD"
		).getProperty( "reference_x" ).getSelectables().iterator();

		assertTrue( columns.hasNext() );
		assertEquals( "XX_reference", columns.next().getText() );
		assertFalse( columns.hasNext() );

		columns = scope.getDomainModel().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.naming.ids.JoinEmbIdNamingRefIngEntity_AUD"
		).getProperty( "reference_y" ).getSelectables().iterator();

		assertTrue( columns.hasNext() );
		assertEquals( "YY_reference", columns.next().getText() );
		assertFalse( columns.hasNext() );
	}
}
