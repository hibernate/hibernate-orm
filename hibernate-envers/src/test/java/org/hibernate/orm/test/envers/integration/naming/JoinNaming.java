/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.naming;

import java.util.Arrays;
import java.util.Iterator;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.mapping.Selectable;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@DomainModel(annotatedClasses = {JoinNamingRefEdEntity.class, JoinNamingRefIngEntity.class})
@SessionFactory
public class JoinNaming {
	private Integer ed_id1;
	private Integer ed_id2;
	private Integer ing_id1;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			JoinNamingRefEdEntity ed1 = new JoinNamingRefEdEntity( "data1" );
			JoinNamingRefEdEntity ed2 = new JoinNamingRefEdEntity( "data2" );
			JoinNamingRefIngEntity ing1 = new JoinNamingRefIngEntity( "x", ed1 );

			em.persist( ed1 );
			em.persist( ed2 );
			em.persist( ing1 );

			ed_id1 = ed1.getId();
			ed_id2 = ed2.getId();
			ing_id1 = ing1.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			JoinNamingRefEdEntity ed2 = em.find( JoinNamingRefEdEntity.class, ed_id2 );
			JoinNamingRefIngEntity ing1 = em.find( JoinNamingRefIngEntity.class, ing_id1 );
			ing1.setData( "y" );
			ing1.setReference( ed2 );
		} );
	}

	@Test
	public void testRevisionsCounts(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( JoinNamingRefEdEntity.class, ed_id1 ) );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( JoinNamingRefEdEntity.class, ed_id2 ) );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( JoinNamingRefIngEntity.class, ing_id1 ) );
		} );
	}

	@Test
	public void testHistoryOfEdId1(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			JoinNamingRefEdEntity ver1 = new JoinNamingRefEdEntity( ed_id1, "data1" );

			assertEquals( ver1, auditReader.find( JoinNamingRefEdEntity.class, ed_id1, 1 ) );
			assertEquals( ver1, auditReader.find( JoinNamingRefEdEntity.class, ed_id1, 2 ) );
		} );
	}

	@Test
	public void testHistoryOfEdId2(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			JoinNamingRefEdEntity ver1 = new JoinNamingRefEdEntity( ed_id2, "data2" );

			assertEquals( ver1, auditReader.find( JoinNamingRefEdEntity.class, ed_id2, 1 ) );
			assertEquals( ver1, auditReader.find( JoinNamingRefEdEntity.class, ed_id2, 2 ) );
		} );
	}

	@Test
	public void testHistoryOfIngId1(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			JoinNamingRefIngEntity ver1 = new JoinNamingRefIngEntity( ing_id1, "x", null );
			JoinNamingRefIngEntity ver2 = new JoinNamingRefIngEntity( ing_id1, "y", null );

			assertEquals( ver1, auditReader.find( JoinNamingRefIngEntity.class, ing_id1, 1 ) );
			assertEquals( ver2, auditReader.find( JoinNamingRefIngEntity.class, ing_id1, 2 ) );

			assertEquals(
					new JoinNamingRefEdEntity( ed_id1, "data1" ),
					auditReader.find( JoinNamingRefIngEntity.class, ing_id1, 1 ).getReference()
			);
			assertEquals(
					new JoinNamingRefEdEntity( ed_id2, "data2" ),
					auditReader.find( JoinNamingRefIngEntity.class, ing_id1, 2 ).getReference()
			);
		} );
	}

	@Test
	public void testJoinColumnName(DomainModelScope scope) {
		Iterator<Selectable> columns = scope.getDomainModel().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.naming.JoinNamingRefIngEntity_AUD"
		).getProperty( "reference_id" ).getSelectables().iterator();
		assertTrue( columns.hasNext() );
		assertEquals( "jnree_column_reference", columns.next().getText() );
		assertFalse( columns.hasNext() );
	}
}
