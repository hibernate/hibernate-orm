/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.mappedsuperclass;

import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jakob Braeuchi.
 * @author Gail Badner
 */
@JiraKey(value = "HHH-9193")
public class EmbeddableWithNoDeclaredDataTest extends BaseEnversJPAFunctionalTestCase {
	private long id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EntityWithEmbeddableWithNoDeclaredData.class, AbstractEmbeddable.class, EmbeddableWithNoDeclaredData.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		EntityWithEmbeddableWithNoDeclaredData entity = new EntityWithEmbeddableWithNoDeclaredData();
		entity.setName( "Entity 1" );
		entity.setValue( new EmbeddableWithNoDeclaredData( 84 ) );

		EntityTransaction tx = em.getTransaction();
		tx.begin();
		em.persist( entity );
		tx.commit();
		em.close();

		id = entity.getId();
	}

	@Test
	public void testEmbeddableThatExtendsMappedSuperclass() {

		// Reload and Compare Revision
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		EntityWithEmbeddableWithNoDeclaredData entityLoaded = em.find( EntityWithEmbeddableWithNoDeclaredData.class, id );

		AuditReader reader = AuditReaderFactory.get( em );

		List<Number> revs = reader.getRevisions( EntityWithEmbeddableWithNoDeclaredData.class, id );
		Assert.assertEquals( 1, revs.size() );

		EntityWithEmbeddableWithNoDeclaredData entityRev1 = reader.find( EntityWithEmbeddableWithNoDeclaredData.class, id, revs.get( 0 ) );

		em.getTransaction().commit();
		Assert.assertEquals( entityLoaded.getName(), entityRev1.getName() );

		// value should be null because there is no data in EmbeddableWithNoDeclaredData
		// and the fields in AbstractEmbeddable should not be audited.
		Assert.assertNull( entityRev1.getValue() );
	}
}
