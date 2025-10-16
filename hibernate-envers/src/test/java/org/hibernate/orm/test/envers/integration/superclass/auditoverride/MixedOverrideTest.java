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
@DomainModel(annotatedClasses = {MixedOverrideEntity.class})
@SessionFactory
public class MixedOverrideTest {
	private Integer mixedEntityId = null;
	private Table mixedTable = null;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope, DomainModelScope dms) {
		// Revision 1
		scope.inTransaction( em -> {
			MixedOverrideEntity mixedEntity = new MixedOverrideEntity( "data 1", 1, "data 2" );
			em.persist( mixedEntity );
			mixedEntityId = mixedEntity.getId();
		} );

		mixedTable = dms.getDomainModel().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.superclass.auditoverride.MixedOverrideEntity_AUD"
		).getTable();
	}

	@Test
	public void testAuditedProperty() {
		assertNotNull( mixedTable.getColumn( new Column( "number1" ) ) );
		assertNotNull( mixedTable.getColumn( new Column( "str2" ) ) );
	}

	@Test
	public void testNotAuditedProperty() {
		assertNull( mixedTable.getColumn( new Column( "str1" ) ) );
	}

	@Test
	public void testHistoryOfMixedEntity(SessionFactoryScope scope) {
		scope.inSession( em -> {
			MixedOverrideEntity ver1 = new MixedOverrideEntity( null, 1, mixedEntityId, "data 2" );
			assertEquals( ver1, AuditReaderFactory.get( em ).find( MixedOverrideEntity.class, mixedEntityId, 1 ) );
		} );
	}
}
