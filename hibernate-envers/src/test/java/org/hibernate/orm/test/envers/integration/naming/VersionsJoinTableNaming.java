/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.naming;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.mapping.Column;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@DomainModel(annotatedClasses = {VersionsJoinTableTestEntity.class, StrTestEntity.class})
@SessionFactory
public class VersionsJoinTableNaming {
	private Integer uni1_id;
	private Integer str1_id;

	private static final String MIDDLE_VERSIONS_ENTITY_NAME = "VERSIONS_JOIN_TABLE_TEST";

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			VersionsJoinTableTestEntity uni1 = new VersionsJoinTableTestEntity( 1, "data1" );
			StrTestEntity str1 = new StrTestEntity( "str1" );

			uni1.setCollection( new HashSet<>() );
			em.persist( uni1 );
			em.persist( str1 );

			uni1_id = uni1.getId();
			str1_id = str1.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			VersionsJoinTableTestEntity uni1 = em.find( VersionsJoinTableTestEntity.class, uni1_id );
			StrTestEntity str1 = em.find( StrTestEntity.class, str1_id );
			uni1.getCollection().add( str1 );
		} );
	}

	@Test
	public void testRevisionsCounts(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( VersionsJoinTableTestEntity.class, uni1_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( StrTestEntity.class, str1_id ) );
		} );
	}

	@Test
	public void testHistoryOfUniId1(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			StrTestEntity str1 = em.find( StrTestEntity.class, str1_id );

			VersionsJoinTableTestEntity rev1 = auditReader.find( VersionsJoinTableTestEntity.class, uni1_id, 1 );
			VersionsJoinTableTestEntity rev2 = auditReader.find( VersionsJoinTableTestEntity.class, uni1_id, 2 );

			assertEquals( TestTools.makeSet(), rev1.getCollection() );
			assertEquals( TestTools.makeSet( str1 ), rev2.getCollection() );

			assertEquals( "data1", rev1.getData() );
			assertEquals( "data1", rev2.getData() );
		} );
	}

	@Test
	public void testTableName(DomainModelScope scope) {
		assertEquals(
				MIDDLE_VERSIONS_ENTITY_NAME,
				scope.getDomainModel().getEntityBinding( MIDDLE_VERSIONS_ENTITY_NAME ).getTable().getName()
		);
	}

	@Test
	public void testJoinColumnName(DomainModelScope scope) {
		Iterator<Column> columns = scope.getDomainModel()
				.getEntityBinding( MIDDLE_VERSIONS_ENTITY_NAME )
				.getTable()
				.getColumns()
				.iterator();

		boolean id1Found = false;
		boolean id2Found = false;

		while ( columns.hasNext() ) {
			Column column = columns.next();
			if ( "VJT_ID".equals( column.getName() ) ) {
				id1Found = true;
			}
			if ( "STR_ID".equals( column.getName() ) ) {
				id2Found = true;
			}
		}

		assertTrue( id1Found && id2Found );
	}
}
