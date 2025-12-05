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
@DomainModel(annotatedClasses = {PropertyOverrideEntity.class, TransitiveOverrideEntity.class, AuditedSpecialEntity.class})
@SessionFactory
public class AuditPropertyOverrideTest {
	private Integer propertyEntityId = null;
	private Integer transitiveEntityId = null;
	private Integer auditedEntityId = null;
	private Table propertyTable = null;
	private Table transitiveTable = null;
	private Table auditedTable = null;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope, DomainModelScope dms) {
		// Revision 1
		scope.inTransaction( em -> {
			PropertyOverrideEntity propertyEntity = new PropertyOverrideEntity( "data 1", 1, "data 2" );
			em.persist( propertyEntity );
			propertyEntityId = propertyEntity.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			TransitiveOverrideEntity transitiveEntity = new TransitiveOverrideEntity( "data 1", 1, "data 2", 2, "data 3" );
			em.persist( transitiveEntity );
			transitiveEntityId = transitiveEntity.getId();
		} );

		// Revision 3
		scope.inTransaction( em -> {
			AuditedSpecialEntity auditedEntity = new AuditedSpecialEntity( "data 1", 1, "data 2" );
			em.persist( auditedEntity );
			auditedEntityId = auditedEntity.getId();
		} );

		propertyTable = dms.getDomainModel().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.superclass.auditoverride.PropertyOverrideEntity_AUD"
		).getTable();
		transitiveTable = dms.getDomainModel().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.superclass.auditoverride.TransitiveOverrideEntity_AUD"
		).getTable();
		auditedTable = dms.getDomainModel().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.superclass.auditoverride.AuditedSpecialEntity_AUD"
		).getTable();
	}

	@Test
	public void testNotAuditedProperty() {
		assertNull( propertyTable.getColumn( new Column( "str1" ) ) );
	}

	@Test
	public void testAuditedProperty() {
		assertNotNull( propertyTable.getColumn( new Column( "number1" ) ) );
		assertNotNull( transitiveTable.getColumn( new Column( "number2" ) ) );
		assertNotNull( auditedTable.getColumn( new Column( "str1" ) ) );
	}

	@Test
	public void testTransitiveAuditedProperty() {
		assertNotNull( transitiveTable.getColumn( new Column( "number1" ) ) );
		assertNotNull( transitiveTable.getColumn( new Column( "str1" ) ) );
	}

	@Test
	public void testHistoryOfPropertyOverrideEntity(SessionFactoryScope scope) {
		scope.inSession( em -> {
			PropertyOverrideEntity ver1 = new PropertyOverrideEntity( null, 1, propertyEntityId, "data 2" );
			assertEquals( ver1, AuditReaderFactory.get( em ).find( PropertyOverrideEntity.class, propertyEntityId, 1 ) );
		} );
	}

	@Test
	public void testHistoryOfTransitiveOverrideEntity(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TransitiveOverrideEntity ver1 = new TransitiveOverrideEntity(
					"data 1",
					1,
					transitiveEntityId,
					"data 2",
					2,
					"data 3"
			);
			assertEquals( ver1, AuditReaderFactory.get( em ).find( TransitiveOverrideEntity.class, transitiveEntityId, 2 ) );
		} );
	}

	@Test
	public void testHistoryOfAuditedSpecialEntity(SessionFactoryScope scope) {
		scope.inSession( em -> {
			AuditedSpecialEntity ver1 = new AuditedSpecialEntity( "data 1", null, auditedEntityId, "data 2" );
			assertEquals( ver1, AuditReaderFactory.get( em ).find( AuditedSpecialEntity.class, auditedEntityId, 3 ) );
		} );
	}
}
