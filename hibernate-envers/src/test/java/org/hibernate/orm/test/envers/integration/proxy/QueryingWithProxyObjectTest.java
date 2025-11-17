/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.proxy;

import java.util.Arrays;
import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Jpa(annotatedClasses = {StrTestEntity.class})
@EnversTest
public class QueryingWithProxyObjectTest {
	private Integer id = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		id = scope.fromTransaction( em -> {
			StrTestEntity ste = new StrTestEntity( "data" );
			em.persist( ste );
			return ste.getId();
		} );
	}

	@Test
	@JiraKey(value = "HHH-4760")
	@SuppressWarnings("unchecked")
	public void testQueryingWithProxyObject(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			StrTestEntity originalSte = new StrTestEntity( "data", id );
			// Load the proxy instance
			StrTestEntity proxySte = em.getReference( StrTestEntity.class, id );

			var auditReader = AuditReaderFactory.get( em );

			assertTrue( auditReader.isEntityClassAudited( proxySte.getClass() ) );

			StrTestEntity ste = auditReader.find( proxySte.getClass(), proxySte.getId(), 1 );
			assertEquals( originalSte, ste );

			List<Number> revisions = auditReader.getRevisions( proxySte.getClass(), proxySte.getId() );
			assertEquals( Arrays.asList( 1 ), revisions );

			List<StrTestEntity> entities = auditReader.createQuery()
					.forEntitiesAtRevision( proxySte.getClass(), 1 )
					.getResultList();
			assertEquals( Arrays.asList( originalSte ), entities );

			ste = (StrTestEntity) auditReader.createQuery()
					.forRevisionsOfEntity( proxySte.getClass(), true, false )
					.getSingleResult();
			assertEquals( originalSte, ste );

			ste = (StrTestEntity) auditReader.createQuery()
					.forEntitiesModifiedAtRevision( proxySte.getClass(), 1 )
					.getSingleResult();
			assertEquals( originalSte, ste );
		} );
	}
}
