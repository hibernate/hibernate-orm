/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.naming.quotation;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@EnversTest
@DomainModel(annotatedClasses = {QuotedFieldsEntity.class})
@SessionFactory
public class QuotedFieldsTest {
	private Long qfeId1 = null;
	private Long qfeId2 = null;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			QuotedFieldsEntity qfe1 = new QuotedFieldsEntity( "data1", 1 );
			QuotedFieldsEntity qfe2 = new QuotedFieldsEntity( "data2", 2 );
			em.persist( qfe1 );
			em.persist( qfe2 );
			qfeId1 = qfe1.getId();
			qfeId2 = qfe2.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			QuotedFieldsEntity qfe1 = em.find( QuotedFieldsEntity.class, qfeId1 );
			qfe1.setData1( "data1 changed" );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			QuotedFieldsEntity qfe2 = em.find( QuotedFieldsEntity.class, qfeId2 );
			qfe2.setData2( 3 );
		} );
	}

	@Test
	public void testRevisionsCounts(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( QuotedFieldsEntity.class, qfeId1 ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( QuotedFieldsEntity.class, qfeId2 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			QuotedFieldsEntity ver1 = new QuotedFieldsEntity( qfeId1, "data1", 1 );
			QuotedFieldsEntity ver2 = new QuotedFieldsEntity( qfeId1, "data1 changed", 1 );

			assertEquals( ver1, auditReader.find( QuotedFieldsEntity.class, qfeId1, 1 ) );
			assertEquals( ver2, auditReader.find( QuotedFieldsEntity.class, qfeId1, 2 ) );
			assertEquals( ver2, auditReader.find( QuotedFieldsEntity.class, qfeId1, 3 ) );
		} );
	}

	@Test
	public void testHistoryOfId2(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			QuotedFieldsEntity ver1 = new QuotedFieldsEntity( qfeId2, "data2", 2 );
			QuotedFieldsEntity ver2 = new QuotedFieldsEntity( qfeId2, "data2", 3 );

			assertEquals( ver1, auditReader.find( QuotedFieldsEntity.class, qfeId2, 1 ) );
			assertEquals( ver1, auditReader.find( QuotedFieldsEntity.class, qfeId2, 2 ) );
			assertEquals( ver2, auditReader.find( QuotedFieldsEntity.class, qfeId2, 3 ) );
		} );
	}

	@Test
	public void testEscapeEntityField(DomainModelScope scope) {
		Table table = scope.getDomainModel()
				.getEntityBinding( "org.hibernate.orm.test.envers.integration.naming.quotation.QuotedFieldsEntity_AUD" )
				.getTable();

		Column column1 = getColumnByName( table, "id" );
		Column column2 = getColumnByName( table, "data1" );
		Column column3 = getColumnByName( table, "data2" );

		assertNotNull( column1 );
		assertNotNull( column2 );
		assertNotNull( column3 );
		assertTrue( column1.isQuoted() );
		assertTrue( column2.isQuoted() );
		assertTrue( column3.isQuoted() );
	}

	private Column getColumnByName(Table table, String columnName) {
		Collection<Column> columns = table.getColumns();
		for ( Column column : columns ) {
			if ( columnName.equals( column.getName() ) ) {
				return column;
			}
		}
		return null;
	}
}
