/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.hibernate.orm.test.envers.tools.TestTools;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings("unchecked")
@Jpa(annotatedClasses = {StrIntTestEntity.class})
@EnversTest
public class RevisionConstraintQuery {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			StrIntTestEntity site1 = new StrIntTestEntity( "a", 10 );
			StrIntTestEntity site2 = new StrIntTestEntity( "b", 15 );

			em.persist( site1 );
			em.persist( site2 );

			id1 = site1.getId();
			Integer id2 = site2.getId();

			em.getTransaction().commit();

			// Revision 2
			em.getTransaction().begin();

			StrIntTestEntity site1_2 = em.find( StrIntTestEntity.class, id1 );
			StrIntTestEntity site2_2 = em.find( StrIntTestEntity.class, id2 );

			site1_2.setStr1( "d" );
			site2_2.setNumber( 20 );

			em.getTransaction().commit();

			// Revision 3
			em.getTransaction().begin();

			StrIntTestEntity site1_3 = em.find( StrIntTestEntity.class, id1 );
			StrIntTestEntity site2_3 = em.find( StrIntTestEntity.class, id2 );

			site1_3.setNumber( 1 );
			site2_3.setStr1( "z" );

			em.getTransaction().commit();

			// Revision 4
			em.getTransaction().begin();

			StrIntTestEntity site1_4 = em.find( StrIntTestEntity.class, id1 );
			StrIntTestEntity site2_4 = em.find( StrIntTestEntity.class, id2 );

			site1_4.setNumber( 5 );
			site2_4.setStr1( "a" );

			em.getTransaction().commit();
		} );
	}

	@Test
	public void testRevisionsLtQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.addProjection( AuditEntity.revisionNumber().distinct() )
					.add( AuditEntity.revisionNumber().lt( 3 ) )
					.addOrder( AuditEntity.revisionNumber().asc() )
					.getResultList();

			assertEquals( Arrays.asList( 1, 2 ), result );
		} );
	}

	@Test
	public void testRevisionsGeQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.addProjection( AuditEntity.revisionNumber().distinct() )
					.add( AuditEntity.revisionNumber().ge( 2 ) )
					.addOrder( AuditEntity.revisionNumber().asc() )
					.getResultList();

			assertEquals( TestTools.makeSet( 2, 3, 4 ), new HashSet( result ) );
		} );
	}

	@Test
	public void testRevisionsLeWithPropertyQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.addProjection( AuditEntity.revisionNumber() )
					.add( AuditEntity.revisionNumber().le( 3 ) )
					.add( AuditEntity.property( "str1" ).eq( "a" ) )
					.getResultList();

			assertEquals( Arrays.asList( 1 ), result );
		} );
	}

	@Test
	public void testRevisionsGtWithPropertyQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.addProjection( AuditEntity.revisionNumber() )
					.add( AuditEntity.revisionNumber().gt( 1 ) )
					.add( AuditEntity.property( "number" ).lt( 10 ) )
					.getResultList();

			assertEquals( TestTools.makeSet( 3, 4 ), new HashSet<>( result ) );
		} );
	}

	@Test
	public void testRevisionProjectionQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			Object[] result = (Object[]) AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.addProjection( AuditEntity.revisionNumber().max() )
					.addProjection( AuditEntity.revisionNumber().count() )
					.addProjection( AuditEntity.revisionNumber().countDistinct() )
					.addProjection( AuditEntity.revisionNumber().min() )
					.add( AuditEntity.id().eq( id1 ) )
					.getSingleResult();

			assertEquals( Integer.valueOf( 4 ), result[0] );
			assertEquals( Long.valueOf( 4 ), result[1] );
			assertEquals( Long.valueOf( 4 ), result[2] );
			assertEquals( Integer.valueOf( 1 ), result[3] );
		} );
	}

	@Test
	public void testRevisionOrderQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.addProjection( AuditEntity.revisionNumber() )
					.add( AuditEntity.id().eq( id1 ) )
					.addOrder( AuditEntity.revisionNumber().desc() )
					.getResultList();

			assertEquals( Arrays.asList( 4, 3, 2, 1 ), result );
		} );
	}

	@Test
	public void testRevisionCountQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// The query shouldn't be ordered as always, otherwise - we get an exception.
			Object result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.addProjection( AuditEntity.revisionNumber().count() )
					.add( AuditEntity.id().eq( id1 ) )
					.getSingleResult();

			assertEquals( Long.valueOf( 4 ), result );
		} );
	}

	@Test
	public void testRevisionTypeEqQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// The query shouldn't be ordered as always, otherwise - we get an exception.
			List results = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, true, true )
					.add( AuditEntity.id().eq( id1 ) )
					.add( AuditEntity.revisionType().eq( RevisionType.MOD ) )
					.getResultList();

			assertEquals( 3, results.size() );
			assertEquals( new StrIntTestEntity( "d", 10, id1 ), results.get( 0 ) );
			assertEquals( new StrIntTestEntity( "d", 1, id1 ), results.get( 1 ) );
			assertEquals( new StrIntTestEntity( "d", 5, id1 ), results.get( 2 ) );
		} );
	}

	@Test
	public void testRevisionTypeNeQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// The query shouldn't be ordered as always, otherwise - we get an exception.
			List results = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, true, true )
					.add( AuditEntity.id().eq( id1 ) )
					.add( AuditEntity.revisionType().ne( RevisionType.MOD ) )
					.getResultList();

			assertEquals( 1, results.size() );
			assertEquals( new StrIntTestEntity( "a", 10, id1 ), results.get( 0 ) );
		} );
	}
}
