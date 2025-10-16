/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;


import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Jpa(annotatedClasses = {
		StrIntTestEntity.class
})
@EnversTest
public class DeletedEntities {
	private Integer id1;
	private Integer id2;
	private Integer id3;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			StrIntTestEntity site1 = new StrIntTestEntity( "a", 10 );
			StrIntTestEntity site2 = new StrIntTestEntity( "b", 11 );

			em.persist( site1 );
			em.persist( site2 );

			id2 = site2.getId();

			em.getTransaction().commit();

			// Revision 2
			em.getTransaction().begin();

			site2 = em.find( StrIntTestEntity.class, id2 );
			em.remove( site2 );

			em.getTransaction().commit();
		} );
	}

	@Test
	public void testProjectionsInEntitiesAtRevision(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( 2, auditReader.createQuery().forEntitiesAtRevision( StrIntTestEntity.class, 1 ).getResultList().size() );
			assertEquals( 1, auditReader.createQuery().forEntitiesAtRevision( StrIntTestEntity.class, 2 ).getResultList().size() );

			assertEquals( 2L, auditReader.createQuery().forEntitiesAtRevision( StrIntTestEntity.class, 1 )
					.addProjection( AuditEntity.id().count() ).getResultList().get( 0 ) );
			assertEquals( 1L, auditReader.createQuery().forEntitiesAtRevision( StrIntTestEntity.class, 2 )
					.addProjection( AuditEntity.id().count() ).getResultList().get( 0 ) );
		} );
	}

	@Test
	public void testRevisionsOfEntityWithoutDelete(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, false )
					.add( AuditEntity.id().eq( id2 ) )
					.getResultList();

			assertEquals( 1, result.size() );


			assertEquals( new StrIntTestEntity( "b", 11, id2 ), ((Object[]) result.get( 0 ))[0] );
			assertEquals( 1, ((SequenceIdRevisionEntity) ((Object[]) result.get( 0 ))[1]).getId() );
			assertEquals( RevisionType.ADD, ((Object[]) result.get( 0 ))[2] );
		} );
	}
}
