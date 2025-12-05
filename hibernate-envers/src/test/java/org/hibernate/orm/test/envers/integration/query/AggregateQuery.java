/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.Arrays;
import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.entities.IntTestEntity;
import org.hibernate.orm.test.envers.entities.ids.UnusualIdNamingEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings("unchecked")
@Jpa(annotatedClasses = {
		IntTestEntity.class,
		UnusualIdNamingEntity.class
})
@EnversTest
public class AggregateQuery {
	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			IntTestEntity ite1 = new IntTestEntity( 2 );
			IntTestEntity ite2 = new IntTestEntity( 10 );
			em.persist( ite1 );
			em.persist( ite2 );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			IntTestEntity ite3 = new IntTestEntity( 8 );
			UnusualIdNamingEntity uine1 = new UnusualIdNamingEntity( "id1", "data1" );
			em.persist( uine1 );
			em.persist( ite3 );
			IntTestEntity ite1 = em.createQuery( "from IntTestEntity where number = 2", IntTestEntity.class )
					.getSingleResult();
			ite1.setNumber( 0 );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			IntTestEntity ite2 = em.createQuery( "from IntTestEntity where number = 10", IntTestEntity.class )
					.getSingleResult();
			ite2.setNumber( 52 );
		} );
	}

	@Test
	public void testEntitiesAvgMaxQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			Object[] ver1 = (Object[]) AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( IntTestEntity.class, 1 )
					.addProjection( AuditEntity.property( "number" ).max() )
					.addProjection( AuditEntity.property( "number" ).function( "avg" ) )
					.getSingleResult();

			Object[] ver2 = (Object[]) AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( IntTestEntity.class, 2 )
					.addProjection( AuditEntity.property( "number" ).max() )
					.addProjection( AuditEntity.property( "number" ).function( "avg" ) )
					.getSingleResult();

			Object[] ver3 = (Object[]) AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( IntTestEntity.class, 3 )
					.addProjection( AuditEntity.property( "number" ).max() )
					.addProjection( AuditEntity.property( "number" ).function( "avg" ) )
					.getSingleResult();

			assertEquals( 10, (Integer) ver1[0] );
			assertEquals( 6.0, (Double) ver1[1] );

			assertEquals( 10, (Integer) ver2[0] );
			assertEquals( 6.0, (Double) ver2[1] );

			assertEquals( 52, (Integer) ver3[0] );
			assertEquals( 20.0, (Double) ver3[1] );
		} );
	}

	@Test
	@JiraKey(value = "HHH-8036")
	public void testEntityIdProjection(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			Integer maxId = (Integer) AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( IntTestEntity.class, true, true )
					.addProjection( AuditEntity.id().max() )
					.add( AuditEntity.revisionNumber().gt( 2 ) )
					.getSingleResult();
			assertEquals( Integer.valueOf( 2 ), maxId );
		} );
	}

	@Test
	@JiraKey(value = "HHH-8036")
	public void testEntityIdRestriction(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<IntTestEntity> list = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( IntTestEntity.class, true, true )
					.add( AuditEntity.id().between( 2, 3 ) )
					.getResultList();
			assertTrue(
					TestTools.checkCollection(
							list,
							new IntTestEntity( 10, 2 ), new IntTestEntity( 8, 3 ), new IntTestEntity( 52, 2 )
					)
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-8036")
	public void testEntityIdOrdering(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<IntTestEntity> list = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( IntTestEntity.class, true, true )
					.add( AuditEntity.revisionNumber().lt( 2 ) )
					.addOrder( AuditEntity.id().desc() )
					.getResultList();
			assertEquals( Arrays.asList( new IntTestEntity( 10, 2 ), new IntTestEntity( 2, 1 ) ), list );
		} );
	}

	@Test
	@JiraKey(value = "HHH-8036")
	public void testUnusualIdFieldName(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			UnusualIdNamingEntity entity = (UnusualIdNamingEntity) AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( UnusualIdNamingEntity.class, true, true )
					.add( AuditEntity.id().like( "id1" ) )
					.getSingleResult();
			assertEquals( new UnusualIdNamingEntity( "id1", "data1" ), entity );
		} );
	}

	@Test
	@JiraKey(value = "HHH-8036")
	public void testEntityIdModifiedFlagNotSupported(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			try {
				AuditReaderFactory.get( em ).createQuery()
						.forRevisionsOfEntity( IntTestEntity.class, true, true )
						.add( AuditEntity.id().hasChanged() )
						.getResultList();
			}
			catch (UnsupportedOperationException e1) {
				try {
					AuditReaderFactory.get( em ).createQuery()
							.forRevisionsOfEntity( IntTestEntity.class, true, true )
							.add( AuditEntity.id().hasNotChanged() )
							.getResultList();
				}
				catch (UnsupportedOperationException e2) {
					return;
				}
				fail();
			}
			fail();
		} );
	}
}
