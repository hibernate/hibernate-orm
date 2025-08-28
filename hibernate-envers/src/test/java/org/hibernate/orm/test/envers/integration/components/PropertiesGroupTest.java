/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components;

import org.hibernate.Session;
import org.hibernate.orm.test.envers.BaseEnversFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.components.UniquePropsEntity;
import org.hibernate.orm.test.envers.entities.components.UniquePropsNotAuditedEntity;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-6636")
public class PropertiesGroupTest extends BaseEnversFunctionalTestCase {
	private PersistentClass uniquePropsAudit = null;
	private PersistentClass uniquePropsNotAuditedAudit = null;
	private UniquePropsEntity entityRev1 = null;
	private UniquePropsNotAuditedEntity entityNotAuditedRev2 = null;

	@Override
	protected String[] getMappings() {
		return new String[] {
				"mappings/components/UniquePropsEntity.hbm.xml",
				"mappings/components/UniquePropsNotAuditedEntity.hbm.xml"
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		uniquePropsAudit = metadata().getEntityBinding(
				"org.hibernate.orm.test.envers.entities.components.UniquePropsEntity_AUD"
		);
		uniquePropsNotAuditedAudit = metadata().getEntityBinding(
				"org.hibernate.orm.test.envers.entities.components.UniquePropsNotAuditedEntity_AUD"
		);

		// Revision 1
		Session session = openSession();
		session.getTransaction().begin();
		UniquePropsEntity ent = new UniquePropsEntity();
		ent.setData1( "data1" );
		ent.setData2( "data2" );
		session.persist( ent );
		session.getTransaction().commit();

		entityRev1 = new UniquePropsEntity( ent.getId(), ent.getData1(), ent.getData2() );

		// Revision 2
		session.getTransaction().begin();
		UniquePropsNotAuditedEntity entNotAud = new UniquePropsNotAuditedEntity();
		entNotAud.setData1( "data3" );
		entNotAud.setData2( "data4" );
		session.persist( entNotAud );
		session.getTransaction().commit();

		entityNotAuditedRev2 = new UniquePropsNotAuditedEntity( entNotAud.getId(), entNotAud.getData1(), null );
	}

	@Test
	public void testAuditTableColumns() {
		Assert.assertNotNull( uniquePropsAudit.getTable().getColumn( new Column( "DATA1" ) ) );
		Assert.assertNotNull( uniquePropsAudit.getTable().getColumn( new Column( "DATA2" ) ) );

		Assert.assertNotNull( uniquePropsNotAuditedAudit.getTable().getColumn( new Column( "DATA1" ) ) );
		Assert.assertNull( uniquePropsNotAuditedAudit.getTable().getColumn( new Column( "DATA2" ) ) );
	}

	@Test
	public void testHistoryOfUniquePropsEntity() {
		Assert.assertEquals( entityRev1, getAuditReader().find( UniquePropsEntity.class, entityRev1.getId(), 1 ) );
	}

	@Test
	public void testHistoryOfUniquePropsNotAuditedEntity() {
		Assert.assertEquals(
				entityNotAuditedRev2,
				getAuditReader().find( UniquePropsNotAuditedEntity.class, entityNotAuditedRev2.getId(), 2 )
		);
	}
}
