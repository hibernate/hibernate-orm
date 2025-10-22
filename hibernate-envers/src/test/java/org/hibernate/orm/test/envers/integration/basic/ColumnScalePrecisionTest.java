/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7003")
@EnversTest
@DomainModel(annotatedClasses = {ScalePrecisionEntity.class})
@SessionFactory
public class ColumnScalePrecisionTest {
	private Table auditTable = null;
	private Table originalTable = null;
	private Long id = null;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope, DomainModelScope domainModelScope) {
		scope.inTransaction( em -> {
			// Revision 1
			ScalePrecisionEntity entity = new ScalePrecisionEntity( 13.0 );
			em.persist( entity );
			em.flush();
			id = entity.getId();
		} );

		final var domainModel = domainModelScope.getDomainModel();
		auditTable = domainModel.getEntityBinding( "org.hibernate.orm.test.envers.integration.basic.ScalePrecisionEntity_AUD" )
				.getTable();
		originalTable = domainModel.getEntityBinding( "org.hibernate.orm.test.envers.integration.basic.ScalePrecisionEntity" )
				.getTable();
	}

	@Test
	public void testColumnScalePrecision() {
		// runtime assertions; the table variables should have been populated in initData
		Column testColumn = new Column( "wholeNumber" );
		Column scalePrecisionAuditColumn = auditTable.getColumn( testColumn );
		Column scalePrecisionColumn = originalTable.getColumn( testColumn );

		assertNotNull( scalePrecisionAuditColumn );
		assertEquals( scalePrecisionColumn.getPrecision(), scalePrecisionAuditColumn.getPrecision() );
		assertEquals( scalePrecisionColumn.getScale(), scalePrecisionAuditColumn.getScale() );
	}

	@Test
	public void testRevisionsCounts(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertEquals( Arrays.asList( 1 ), AuditReaderFactory.get( session ).getRevisions( ScalePrecisionEntity.class, id ) );
		} );
	}

	@Test
	public void testHistoryOfScalePrecisionEntity(SessionFactoryScope scope) {
		scope.inSession( session -> {
			ScalePrecisionEntity ver1 = new ScalePrecisionEntity( 13.0, id );
			assertEquals( ver1, AuditReaderFactory.get( session ).find( ScalePrecisionEntity.class, id, 1 ) );
		} );
	}
}
