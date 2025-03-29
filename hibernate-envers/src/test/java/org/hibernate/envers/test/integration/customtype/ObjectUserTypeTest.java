/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.customtype;

import java.util.Arrays;
import java.util.Map;
import jakarta.persistence.EntityManager;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7870")
public class ObjectUserTypeTest extends BaseEnversJPAFunctionalTestCase {
	private int id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {ObjectUserTypeEntity.class};
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( EnversSettings.STORE_DATA_AT_DELETE, "true" );
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1 - add
		em.getTransaction().begin();
		ObjectUserTypeEntity entity = new ObjectUserTypeEntity( "builtInType1", "stringUserType1" );
		em.persist( entity );
		em.getTransaction().commit();

		id = entity.getId();

		// Revision 2 - modify
		em.getTransaction().begin();
		entity = em.find( ObjectUserTypeEntity.class, entity.getId() );
		entity.setUserType( 2 );
		entity = em.merge( entity );
		em.getTransaction().commit();

		// Revision 3 - remove
		em.getTransaction().begin();
		entity = em.find( ObjectUserTypeEntity.class, entity.getId() );
		em.remove( entity );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testRevisionCount() {
		Assert.assertEquals(
				Arrays.asList( 1, 2, 3 ),
				getAuditReader().getRevisions( ObjectUserTypeEntity.class, id )
		);
	}

	@Test
	public void testHistory() {
		ObjectUserTypeEntity ver1 = new ObjectUserTypeEntity( id, "builtInType1", "stringUserType1" );
		ObjectUserTypeEntity ver2 = new ObjectUserTypeEntity( id, "builtInType1", 2 );

		Assert.assertEquals( ver1, getAuditReader().find( ObjectUserTypeEntity.class, id, 1 ) );
		Assert.assertEquals( ver2, getAuditReader().find( ObjectUserTypeEntity.class, id, 2 ) );
		Assert.assertEquals(
				ver2,
				getAuditReader().createQuery()
						.forRevisionsOfEntity( ObjectUserTypeEntity.class, true, true )
						.getResultList()
						.get( 2 )
		); // Checking delete state.
	}
}
