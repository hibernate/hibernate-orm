/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.tableperclass.abstractparent;

import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.mapping.Table;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-5910")
public class AuditedAbstractParentTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {AbstractEntity.class, EffectiveEntity1.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		EffectiveEntity1 entity = new EffectiveEntity1( 1L, "commonField", "specificField1" );
		em.persist( entity );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testAbstractTableExistence() {
		for ( Table table : metadata().collectTableMappings() ) {
			if ( "AbstractEntity_AUD".equals( table.getName() ) ) {
				Assert.assertFalse( table.isPhysicalTable() );
				return;
			}
		}
		Assert.fail();
	}
}
