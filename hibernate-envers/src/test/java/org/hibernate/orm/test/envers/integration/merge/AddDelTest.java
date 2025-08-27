/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.merge;

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
@JiraKey(value = "HHH-6753")
public class AddDelTest extends BaseEnversFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class, GivenIdStrEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		Session session = openSession();
		session.getTransaction().begin();
		GivenIdStrEntity entity = new GivenIdStrEntity( 1, "data" );
		session.persist( entity );
		session.getTransaction().commit();

		// Revision 2
		session.getTransaction().begin();
		session.persist( new StrTestEntity( "another data" ) ); // Just to create second revision.
		entity = session.get( GivenIdStrEntity.class, 1 );
		session.remove( entity ); // First try to remove the entity.
		session.persist( entity ); // Then save it.
		session.getTransaction().commit();

		// Revision 3
		session.getTransaction().begin();
		entity = session.get( GivenIdStrEntity.class, 1 );
		session.remove( entity ); // First try to remove the entity.
		entity.setData( "modified data" ); // Then change it's state.
		session.persist( entity ); // Finally save it.
		session.getTransaction().commit();

		session.close();
	}

	@Test
	public void testRevisionsCountOfGivenIdStrEntity() {
		// Revision 2 has not changed entity's state.
		Assert.assertEquals( Arrays.asList( 1, 3 ), getAuditReader().getRevisions( GivenIdStrEntity.class, 1 ) );
	}

	@Test
	public void testHistoryOfGivenIdStrEntity() {
		Assert.assertEquals( new GivenIdStrEntity( 1, "data" ), getAuditReader().find( GivenIdStrEntity.class, 1, 1 ) );
		Assert.assertEquals(
				new GivenIdStrEntity( 1, "modified data" ), getAuditReader().find(
				GivenIdStrEntity.class,
				1,
				3
		)
		);
	}
}
