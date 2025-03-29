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
public class AuditPropertyOverrideTest extends BaseEnversJPAFunctionalTestCase {
	private Integer propertyEntityId = null;
	private Integer transitiveEntityId = null;
	private Integer auditedEntityId = null;
	private Table propertyTable = null;
	private Table transitiveTable = null;
	private Table auditedTable = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {PropertyOverrideEntity.class, TransitiveOverrideEntity.class, AuditedSpecialEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		PropertyOverrideEntity propertyEntity = new PropertyOverrideEntity( "data 1", 1, "data 2" );
		em.persist( propertyEntity );
		em.getTransaction().commit();
		propertyEntityId = propertyEntity.getId();

		// Revision 2
		em.getTransaction().begin();
		TransitiveOverrideEntity transitiveEntity = new TransitiveOverrideEntity( "data 1", 1, "data 2", 2, "data 3" );
		em.persist( transitiveEntity );
		em.getTransaction().commit();
		transitiveEntityId = transitiveEntity.getId();

		// Revision 3
		em.getTransaction().begin();
		AuditedSpecialEntity auditedEntity = new AuditedSpecialEntity( "data 1", 1, "data 2" );
		em.persist( auditedEntity );
		em.getTransaction().commit();
		auditedEntityId = auditedEntity.getId();

		propertyTable = metadata().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.superclass.auditoverride.PropertyOverrideEntity_AUD"
		).getTable();
		transitiveTable = metadata().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.superclass.auditoverride.TransitiveOverrideEntity_AUD"
		).getTable();
		auditedTable = metadata().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.superclass.auditoverride.AuditedSpecialEntity_AUD"
		).getTable();
	}

	@Test
	public void testNotAuditedProperty() {
		Assert.assertNull( propertyTable.getColumn( new Column( "str1" ) ) );
	}

	@Test
	public void testAuditedProperty() {
		Assert.assertNotNull( propertyTable.getColumn( new Column( "number1" ) ) );
		Assert.assertNotNull( transitiveTable.getColumn( new Column( "number2" ) ) );
		Assert.assertNotNull( auditedTable.getColumn( new Column( "str1" ) ) );
	}

	@Test
	public void testTransitiveAuditedProperty() {
		Assert.assertNotNull( transitiveTable.getColumn( new Column( "number1" ) ) );
		Assert.assertNotNull( transitiveTable.getColumn( new Column( "str1" ) ) );
	}

	@Test
	public void testHistoryOfPropertyOverrideEntity() {
		PropertyOverrideEntity ver1 = new PropertyOverrideEntity( null, 1, propertyEntityId, "data 2" );
		Assert.assertEquals( ver1, getAuditReader().find( PropertyOverrideEntity.class, propertyEntityId, 1 ) );
	}

	@Test
	public void testHistoryOfTransitiveOverrideEntity() {
		TransitiveOverrideEntity ver1 = new TransitiveOverrideEntity(
				"data 1",
				1,
				transitiveEntityId,
				"data 2",
				2,
				"data 3"
		);
		Assert.assertEquals( ver1, getAuditReader().find( TransitiveOverrideEntity.class, transitiveEntityId, 2 ) );
	}

	@Test
	public void testHistoryOfAuditedSpecialEntity() {
		AuditedSpecialEntity ver1 = new AuditedSpecialEntity( "data 1", null, auditedEntityId, "data 2" );
		Assert.assertEquals( ver1, getAuditReader().find( AuditedSpecialEntity.class, auditedEntityId, 3 ) );
	}
}
