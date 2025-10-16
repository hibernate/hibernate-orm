/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.tableperclass.abstractparent;

import org.hibernate.mapping.Table;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-5910")
@EnversTest
@DomainModel(annotatedClasses = {AbstractEntity.class, EffectiveEntity1.class})
@SessionFactory
public class AuditedAbstractParentTest {

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		// Revision 1
		scope.inTransaction( session -> {
			EffectiveEntity1 entity = new EffectiveEntity1( 1L, "commonField", "specificField1" );
			session.persist( entity );
		} );
	}

	@Test
	public void testAbstractTableExistence(DomainModelScope scope) {
		for ( Table table : scope.getDomainModel().collectTableMappings() ) {
			if ( "AbstractEntity_AUD".equals( table.getName() ) ) {
				assertFalse( table.isPhysicalTable() );
				return;
			}
		}
		fail();
	}
}
