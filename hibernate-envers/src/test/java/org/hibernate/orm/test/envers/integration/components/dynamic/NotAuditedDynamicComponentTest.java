/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.dynamic;

import java.util.Arrays;

import org.hibernate.Session;
import org.hibernate.orm.test.envers.BaseEnversFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;
import junit.framework.Assert;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey("HHH-8049")
public class NotAuditedDynamicComponentTest extends BaseEnversFunctionalTestCase {
	@Override
	protected String[] getMappings() {
		return new String[] { "mappings/dynamicComponents/mapNotAudited.hbm.xml" };
	}

	@Test
	@Priority(10)
	public void initData() {
		Session session = openSession();

		// Revision 1
		session.getTransaction().begin();
		NotAuditedDynamicMapComponent entity = new NotAuditedDynamicMapComponent( 1L, "static field value" );
		entity.getCustomFields().put( "prop1", 13 );
		entity.getCustomFields().put( "prop2", 0.1f );
		session.persist( entity );
		session.getTransaction().commit();

		// No revision
		session.getTransaction().begin();
		entity = session.get( NotAuditedDynamicMapComponent.class, entity.getId() );
		entity.getCustomFields().put( "prop1", 0 );
		session.merge( entity );
		session.getTransaction().commit();

		// Revision 2
		session.getTransaction().begin();
		entity = session.get( NotAuditedDynamicMapComponent.class, entity.getId() );
		entity.setNote( "updated note" );
		session.merge( entity );
		session.getTransaction().commit();

		// Revision 3
		session.getTransaction().begin();
		entity = session.getReference( NotAuditedDynamicMapComponent.class, entity.getId() );
		session.remove( entity );
		session.getTransaction().commit();

		session.close();
	}

	@Test
	public void testRevisionsCounts() {
		Assert.assertEquals(
				Arrays.asList( 1, 2, 3 ),
				getAuditReader().getRevisions( NotAuditedDynamicMapComponent.class, 1L )
		);
	}

	@Test
	public void testHistoryOfId1() {
		// Revision 1
		NotAuditedDynamicMapComponent entity = new NotAuditedDynamicMapComponent( 1L, "static field value" );
		NotAuditedDynamicMapComponent ver1 = getAuditReader().find(
				NotAuditedDynamicMapComponent.class,
				entity.getId(),
				1
		);
		Assert.assertEquals( entity, ver1 );
		// Assume empty NotAuditedDynamicMapComponent#customFields map, because dynamic-component is not audited.
		Assert.assertTrue( ver1.getCustomFields().isEmpty() );

		// Revision 2
		entity.setNote( "updated note" );
		NotAuditedDynamicMapComponent ver2 = getAuditReader().find(
				NotAuditedDynamicMapComponent.class,
				entity.getId(),
				2
		);
		Assert.assertEquals( entity, ver2 );
		// Assume empty NotAuditedDynamicMapComponent#customFields map, because dynamic-component is not audited.
		Assert.assertTrue( ver2.getCustomFields().isEmpty() );
	}
}
