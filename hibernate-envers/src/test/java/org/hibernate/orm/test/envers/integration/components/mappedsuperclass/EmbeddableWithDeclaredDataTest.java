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

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jakob Braeuchi.
 * @author Gail Badner
 */
@JiraKey(value = "HHH-9193")
public class EmbeddableWithDeclaredDataTest extends BaseEnversJPAFunctionalTestCase {
	private long id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EntityWithEmbeddableWithDeclaredData.class, AbstractEmbeddable.class, EmbeddableWithDeclaredData.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		EntityWithEmbeddableWithDeclaredData entity = new EntityWithEmbeddableWithDeclaredData();
		entity.setName( "Entity 1" );
		entity.setValue( new EmbeddableWithDeclaredData( 42, "TestCodeart" ) );

		EntityTransaction tx = em.getTransaction();
		tx.begin();
		em.persist( entity );
		tx.commit();
		em.close();

		id = entity.getId();
	}

	@Test
	@FailureExpected( jiraKey = "HHH-9193" )
	public void testEmbeddableThatExtendsMappedSuperclass() {

		// Reload and Compare Revision
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		EntityWithEmbeddableWithDeclaredData entityLoaded = em.find( EntityWithEmbeddableWithDeclaredData.class, id );

		AuditReader reader = AuditReaderFactory.get( em );

		List<Number> revs = reader.getRevisions( EntityWithEmbeddableWithDeclaredData.class, id );
		Assert.assertEquals( 1, revs.size() );

		EntityWithEmbeddableWithDeclaredData entityRev1 = reader.find( EntityWithEmbeddableWithDeclaredData.class, id, revs.get( 0 ) );
		em.getTransaction().commit();
		Assert.assertEquals( entityLoaded.getName(), entityRev1.getName() );

		// only value.codeArt should be audited because it is the only audited field in EmbeddableWithDeclaredData;
		// fields in AbstractEmbeddable should not be audited.
		Assert.assertEquals( entityLoaded.getValue().getCodeart(), entityRev1.getValue().getCodeart() );
		Assert.assertEquals(0, entityRev1.getValue().getCode() );
	}
}
