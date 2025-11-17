/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.AuditDisjunction;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;

import org.hibernate.testing.envers.junit.EnversTest;
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
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@SuppressWarnings("unchecked")
@Jpa(annotatedClasses = {StrIntTestEntity.class})
@EnversTest
public class MaximalizePropertyQuery {
	Integer id1;
	Integer id2;
	Integer id3;
	Integer id4;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			StrIntTestEntity site1 = new StrIntTestEntity( "a", 10 );
			StrIntTestEntity site2 = new StrIntTestEntity( "b", 15 );
			StrIntTestEntity site3 = new StrIntTestEntity( "c", 42 );
			StrIntTestEntity site4 = new StrIntTestEntity( "d", 52 );

			em.persist( site1 );
			em.persist( site2 );
			em.persist( site3 );
			em.persist( site4 );

			id1 = site1.getId();
			id2 = site2.getId();
			id3 = site3.getId();
			id4 = site4.getId();

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

			site1_3.setNumber( 30 );
			site2_3.setStr1( "z" );

			em.getTransaction().commit();

			// Revision 4
			em.getTransaction().begin();

			StrIntTestEntity site1_4 = em.find( StrIntTestEntity.class, id1 );
			StrIntTestEntity site2_4 = em.find( StrIntTestEntity.class, id2 );

			site1_4.setNumber( 5 );
			site2_4.setStr1( "a" );

			em.getTransaction().commit();

			// Revision 5
			em.getTransaction().begin();
			StrIntTestEntity site4_5 = em.find( StrIntTestEntity.class, id4 );
			em.remove( site4_5 );
			em.getTransaction().commit();
		} );
	}

	@Test
	public void testMaximizeWithIdEq(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List revs_id1 = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.addProjection( AuditEntity.revisionNumber() )
					.add(
							AuditEntity.property( "number" ).maximize()
									.add( AuditEntity.id().eq( id2 ) )
					)
					.addOrder( AuditEntity.revisionNumber().asc() )
					.getResultList();

			assertEquals( Arrays.asList( 2, 3, 4 ), revs_id1 );
		} );
	}

	@Test
	public void testMinimizeWithPropertyEq(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.addProjection( AuditEntity.revisionNumber() )
					.add(
							AuditEntity.property( "number" ).minimize()
									.add( AuditEntity.property( "str1" ).eq( "a" ) )
					)
					.getResultList();

			assertEquals( Arrays.asList( 1 ), result );
		} );
	}

	@Test
	public void testMaximizeRevision(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.addProjection( AuditEntity.revisionNumber() )
					.add(
							AuditEntity.revisionNumber().maximize()
									.add( AuditEntity.property( "number" ).eq( 10 ) )
					)
					.getResultList();

			assertEquals( Arrays.asList( 2 ), result );
		} );
	}

	@Test
	@JiraKey(value = "HHH-7800")
	public void testMaximizeInDisjunction(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<Integer> idsToQuery = Arrays.asList( id1, id3 );

			AuditDisjunction disjunction = AuditEntity.disjunction();

			for ( Integer id : idsToQuery ) {
				disjunction.add( AuditEntity.revisionNumber().maximize().add( AuditEntity.id().eq( id ) ) );
			}
			List result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, true, true )
					.add( disjunction )
					.getResultList();

			Set<Integer> idsSeen = new HashSet<Integer>();
			for ( Object o : result ) {
				StrIntTestEntity entity = (StrIntTestEntity) o;
				Integer id = entity.getId();
				assertTrue( idsToQuery.contains( id ), "Entity with ID " + id + " returned but not queried for." );
				if ( !idsSeen.add( id ) ) {
					fail( "Multiple revisions returned with ID " + id + "; expected only one." );
				}
			}
		} );
	}

	@Test
	@JiraKey(value = "HHH-7827")
	public void testAllLatestRevisionsOfEntityType(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.add( AuditEntity.revisionNumber().maximize().computeAggregationInInstanceContext() )
					.addOrder( AuditEntity.property( "id" ).asc() )
					.getResultList();

			assertEquals( 4, result.size() );

			Object[] result1 = (Object[]) result.get( 0 );
			Object[] result2 = (Object[]) result.get( 1 );
			Object[] result3 = (Object[]) result.get( 2 );
			Object[] result4 = (Object[]) result.get( 3 );

			checkRevisionData( result1, 4, RevisionType.MOD, new StrIntTestEntity( "d", 5, id1 ) );
			checkRevisionData( result2, 4, RevisionType.MOD, new StrIntTestEntity( "a", 20, id2 ) );
			checkRevisionData( result3, 1, RevisionType.ADD, new StrIntTestEntity( "c", 42, id3 ) );
			checkRevisionData( result4, 5, RevisionType.DEL, new StrIntTestEntity( null, null, id4 ) );
		} );
	}

	private void checkRevisionData(Object[] result, int revision, RevisionType type, StrIntTestEntity entity) {
		assertEquals( entity, result[0] );
		assertEquals( revision, ((SequenceIdRevisionEntity) result[1]).getId() );
		assertEquals( type, result[2] );
	}
}
