/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.superclass.auditoverride;

import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-4439")
public class MixedOverrideTest extends BaseEnversJPAFunctionalTestCase {
	private Integer mixedEntityId = null;
	private Table mixedTable = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {MixedOverrideEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		MixedOverrideEntity mixedEntity = new MixedOverrideEntity( "data 1", 1, "data 2" );
		em.persist( mixedEntity );
		em.getTransaction().commit();
		mixedEntityId = mixedEntity.getId();

		mixedTable = metadata().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.superclass.auditoverride.MixedOverrideEntity_AUD"
		).getTable();
	}

	@Test
	public void testAuditedProperty() {
		Assert.assertNotNull( mixedTable.getColumn( new Column( "number1" ) ) );
		Assert.assertNotNull( mixedTable.getColumn( new Column( "str2" ) ) );
	}

	@Test
	public void testNotAuditedProperty() {
		Assert.assertNull( mixedTable.getColumn( new Column( "str1" ) ) );
	}

	@Test
	public void testHistoryOfMixedEntity() {
		MixedOverrideEntity ver1 = new MixedOverrideEntity( null, 1, mixedEntityId, "data 2" );
		Assert.assertEquals( ver1, getAuditReader().find( MixedOverrideEntity.class, mixedEntityId, 1 ) );
	}
}
