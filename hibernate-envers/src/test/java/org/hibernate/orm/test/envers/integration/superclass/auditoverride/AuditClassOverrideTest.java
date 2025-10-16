/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.superclass.auditoverride;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-4439")
@EnversTest
@DomainModel(annotatedClasses = {ClassOverrideAuditedEntity.class, ClassOverrideNotAuditedEntity.class})
@SessionFactory
public class AuditClassOverrideTest {
	private Integer classAuditedEntityId = null;
	private Integer classNotAuditedEntityId = null;
	private Table classAuditedTable = null;
	private Table classNotAuditedTable = null;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope, DomainModelScope dms) {
		// Revision 1
		scope.inTransaction( em -> {
			ClassOverrideAuditedEntity classOverrideAuditedEntity = new ClassOverrideAuditedEntity( "data 1", 1, "data 2" );
			em.persist( classOverrideAuditedEntity );
			classAuditedEntityId = classOverrideAuditedEntity.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			ClassOverrideNotAuditedEntity classOverrideNotAuditedEntity = new ClassOverrideNotAuditedEntity(
					"data 1",
					1,
					"data 2"
			);
			em.persist( classOverrideNotAuditedEntity );
			classNotAuditedEntityId = classOverrideNotAuditedEntity.getId();
		} );

		classAuditedTable = dms.getDomainModel().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.superclass.auditoverride.ClassOverrideAuditedEntity_AUD"
		).getTable();
		classNotAuditedTable = dms.getDomainModel().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.superclass.auditoverride.ClassOverrideNotAuditedEntity_AUD"
		).getTable();
	}

	@Test
	public void testAuditedProperty() {
		assertNotNull( classAuditedTable.getColumn( new Column( "number1" ) ) );
		assertNotNull( classAuditedTable.getColumn( new Column( "str1" ) ) );
		assertNotNull( classAuditedTable.getColumn( new Column( "str2" ) ) );
		assertNotNull( classNotAuditedTable.getColumn( new Column( "str2" ) ) );
	}

	@Test
	public void testNotAuditedProperty() {
		assertNull( classNotAuditedTable.getColumn( new Column( "number1" ) ) );
		assertNull( classNotAuditedTable.getColumn( new Column( "str1" ) ) );
	}

	@Test
	public void testHistoryOfClassOverrideAuditedEntity(SessionFactoryScope scope) {
		scope.inSession( em -> {
			ClassOverrideAuditedEntity ver1 = new ClassOverrideAuditedEntity( "data 1", 1, classAuditedEntityId, "data 2" );
			assertEquals( ver1, AuditReaderFactory.get( em ).find( ClassOverrideAuditedEntity.class, classAuditedEntityId, 1 ) );
		} );
	}

	@Test
	public void testHistoryOfClassOverrideNotAuditedEntity(SessionFactoryScope scope) {
		scope.inSession( em -> {
			ClassOverrideNotAuditedEntity ver1 = new ClassOverrideNotAuditedEntity(
					null,
					null,
					classNotAuditedEntityId,
					"data 2"
			);
			assertEquals(
					ver1, AuditReaderFactory.get( em ).find(
							ClassOverrideNotAuditedEntity.class,
							classNotAuditedEntityId,
							2
					)
			);
		} );
	}
}
