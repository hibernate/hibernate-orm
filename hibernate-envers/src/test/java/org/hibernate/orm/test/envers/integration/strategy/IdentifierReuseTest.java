/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.strategy;

import java.util.Arrays;
import java.util.Map;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.entities.IntNoAutoIdTestEntity;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests that reusing identifiers doesn't cause auditing misbehavior.
 *
 * @author adar
 */
@JiraKey(value = "HHH-8280")
public class IdentifierReuseTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected void addConfigOptions(Map options) {
		options.put( EnversSettings.ALLOW_IDENTIFIER_REUSE, "true" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { IntNoAutoIdTestEntity.class };
	}

	@Test
	public void testIdentifierReuse() {
		final Integer reusedId = 1;

		EntityManager entityManager = getEntityManager();
		saveUpdateAndRemoveEntity( entityManager, reusedId );
		saveUpdateAndRemoveEntity( entityManager, reusedId );
		entityManager.close();

		assertEquals(
				Arrays.asList( 1, 2, 3, 4, 5, 6 ),
				getAuditReader().getRevisions( IntNoAutoIdTestEntity.class, reusedId )
		);
	}

	private void saveUpdateAndRemoveEntity(EntityManager entityManager, Integer id) {
		EntityTransaction transaction = entityManager.getTransaction();

		transaction.begin();
		IntNoAutoIdTestEntity entity = new IntNoAutoIdTestEntity( 0, id );
		entityManager.persist( entity );
		assertEquals( id, entity.getId() );
		transaction.commit();

		transaction.begin();
		entity = entityManager.find( IntNoAutoIdTestEntity.class, id );
		entity.setNumVal( 1 );
		entity = entityManager.merge( entity );
		assertEquals( id, entity.getId() );
		transaction.commit();

		transaction.begin();
		entity = entityManager.find( IntNoAutoIdTestEntity.class, id );
		assertNotNull( entity );
		entityManager.remove( entity );
		transaction.commit();
	}
}
