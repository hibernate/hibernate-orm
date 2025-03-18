/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.tools;

import java.util.Arrays;

import org.hibernate.Session;
import org.hibernate.orm.test.envers.BaseEnversFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrTestEntity;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7106")
public class SchemaExportTest extends BaseEnversFunctionalTestCase {
	private Integer id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class};
	}

	@Test
	@Priority(10)
	public void testSchemaCreation() {
		// Populate database with test data.
		Session session = getSession();
		session.getTransaction().begin();
		StrTestEntity entity = new StrTestEntity( "data" );
		session.persist( entity );
		session.getTransaction().commit();

		id = entity.getId();
	}

	@Test
	@Priority(9)
	public void testAuditDataRetrieval() {
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( StrTestEntity.class, id ) );
		Assert.assertEquals( new StrTestEntity( "data", id ), getAuditReader().find( StrTestEntity.class, id, 1 ) );
	}
}
