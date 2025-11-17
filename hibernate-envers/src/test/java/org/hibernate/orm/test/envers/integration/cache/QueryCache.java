/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.cache;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.IntTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"ObjectEquality"})
@EnversTest
@Jpa(annotatedClasses = {IntTestEntity.class})
public class QueryCache {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			IntTestEntity ite = new IntTestEntity( 10 );
			em.persist( ite );
			id1 = ite.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			IntTestEntity ite = em.find( IntTestEntity.class, id1 );
			ite.setNumber( 20 );
		} );
	}

	@Test
	public void testCacheFindAfterRevisionsOfEntityQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List entsFromQuery = auditReader.createQuery()
					.forRevisionsOfEntity( IntTestEntity.class, true, false )
					.getResultList();

			IntTestEntity entFromFindRev1 = auditReader.find( IntTestEntity.class, id1, 1 );
			IntTestEntity entFromFindRev2 = auditReader.find( IntTestEntity.class, id1, 2 );

			assertSame( entsFromQuery.get( 0 ), entFromFindRev1 );
			assertSame( entsFromQuery.get( 1 ), entFromFindRev2 );
		} );
	}

	@Test
	public void testCacheFindAfterEntitiesAtRevisionQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			IntTestEntity entFromQuery = (IntTestEntity) auditReader.createQuery()
					.forEntitiesAtRevision( IntTestEntity.class, 1 )
					.getSingleResult();

			IntTestEntity entFromFind = auditReader.find( IntTestEntity.class, id1, 1 );

			assertSame( entFromQuery, entFromFind );
		} );
	}
}
