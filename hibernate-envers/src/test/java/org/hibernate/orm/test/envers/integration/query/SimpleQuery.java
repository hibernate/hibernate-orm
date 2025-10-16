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
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.MatchMode;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.hibernate.orm.test.envers.entities.ids.EmbId;
import org.hibernate.orm.test.envers.entities.ids.EmbIdTestEntity;
import org.hibernate.orm.test.envers.entities.ids.MulId;
import org.hibernate.orm.test.envers.entities.ids.MulIdTestEntity;
import org.hibernate.orm.test.envers.tools.TestTools;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@SuppressWarnings("unchecked")
@Jpa(annotatedClasses = {StrIntTestEntity.class, MulIdTestEntity.class, EmbIdTestEntity.class})
@EnversTest
public class SimpleQuery {
	private Integer id1;
	private Integer id2;
	private Integer id3;
	private MulId mulId1;
	private EmbId embId1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			StrIntTestEntity site1 = new StrIntTestEntity( "a", 10 );
			StrIntTestEntity site2 = new StrIntTestEntity( "a", 10 );
			StrIntTestEntity site3 = new StrIntTestEntity( "b", 5 );

			em.persist( site1 );
			em.persist( site2 );
			em.persist( site3 );

			id1 = site1.getId();
			id2 = site2.getId();
			id3 = site3.getId();

			em.getTransaction().commit();

			// Revision 2
			em.getTransaction().begin();

			mulId1 = new MulId( 1, 2 );
			em.persist( new MulIdTestEntity( mulId1.getId1(), mulId1.getId2(), "data" ) );

			embId1 = new EmbId( 3, 4 );
			em.persist( new EmbIdTestEntity( embId1, "something" ) );

			StrIntTestEntity site1_2 = em.find( StrIntTestEntity.class, id1 );
			StrIntTestEntity site2_2 = em.find( StrIntTestEntity.class, id2 );

			site1_2.setStr1( "aBc" );
			site2_2.setNumber( 20 );

			em.getTransaction().commit();

			// Revision 3
			em.getTransaction().begin();

			StrIntTestEntity site3_3 = em.find( StrIntTestEntity.class, id3 );

			site3_3.setStr1( "a" );

			em.getTransaction().commit();

			// Revision 4
			em.getTransaction().begin();

			StrIntTestEntity site1_4 = em.find( StrIntTestEntity.class, id1 );

			em.remove( site1_4 );

			em.getTransaction().commit();
		} );
	}

	@Test
	public void testEntitiesIdQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			StrIntTestEntity ver2 = (StrIntTestEntity) AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( StrIntTestEntity.class, 2 )
					.add( AuditEntity.id().eq( id2 ) )
					.getSingleResult();

			assertEquals( new StrIntTestEntity( "a", 20, id2 ), ver2 );
		} );
	}

	@Test
	public void testEntitiesPropertyEqualsQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List ver1 = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( StrIntTestEntity.class, 1 )
					.add( AuditEntity.property( "str1" ).eq( "a" ) )
					.getResultList();

			List ver2 = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( StrIntTestEntity.class, 2 )
					.add( AuditEntity.property( "str1" ).eq( "a" ) )
					.getResultList();

			List ver3 = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( StrIntTestEntity.class, 3 )
					.add( AuditEntity.property( "str1" ).eq( "a" ) )
					.getResultList();

			assertEquals(
					TestTools.makeSet(
							new StrIntTestEntity( "a", 10, id1 ),
							new StrIntTestEntity( "a", 10, id2 )
					),
					new HashSet( ver1 )
			);
			assertEquals( TestTools.makeSet( new StrIntTestEntity( "a", 20, id2 ) ), new HashSet( ver2 ) );
			assertEquals(
					TestTools.makeSet(
							new StrIntTestEntity( "a", 20, id2 ),
							new StrIntTestEntity( "a", 5, id3 )
					),
					new HashSet( ver3 )
			);
		} );
	}

	@Test
	public void testEntitiesPropertyLeQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List ver1 = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( StrIntTestEntity.class, 1 )
					.add( AuditEntity.property( "number" ).le( 10 ) )
					.getResultList();

			List ver2 = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( StrIntTestEntity.class, 2 )
					.add( AuditEntity.property( "number" ).le( 10 ) )
					.getResultList();

			List ver3 = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( StrIntTestEntity.class, 3 )
					.add( AuditEntity.property( "number" ).le( 10 ) )
					.getResultList();

			assertEquals(
					TestTools.makeSet(
							new StrIntTestEntity( "a", 10, id1 ),
							new StrIntTestEntity( "a", 10, id2 ), new StrIntTestEntity( "b", 5, id3 )
					),
					new HashSet( ver1 )
			);
			assertEquals(
					TestTools.makeSet(
							new StrIntTestEntity( "aBc", 10, id1 ),
							new StrIntTestEntity( "b", 5, id3 )
					),
					new HashSet( ver2 )
			);
			assertEquals(
					TestTools.makeSet(
							new StrIntTestEntity( "aBc", 10, id1 ),
							new StrIntTestEntity( "a", 5, id3 )
					),
					new HashSet( ver3 )
			);
		} );
	}

	@Test
	public void testRevisionsPropertyEqQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List revs_id1 = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.addProjection( AuditEntity.revisionNumber() )
					.add( AuditEntity.property( "str1" ).le( "a" ) )
					.add( AuditEntity.id().eq( id1 ) )
					.getResultList();

			List revs_id2 = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.addProjection( AuditEntity.revisionNumber() )
					.add( AuditEntity.property( "str1" ).le( "a" ) )
					.add( AuditEntity.id().eq( id2 ) )
					.addOrder( AuditEntity.revisionNumber().asc() )
					.getResultList();

			List revs_id3 = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.addProjection( AuditEntity.revisionNumber() )
					.add( AuditEntity.property( "str1" ).le( "a" ) )
					.add( AuditEntity.id().eq( id3 ) )
					.getResultList();

			assertEquals( Arrays.asList( 1 ), revs_id1 );
			assertEquals( Arrays.asList( 1, 2 ), revs_id2 );
			assertEquals( Arrays.asList( 3 ), revs_id3 );
		} );
	}

	@Test
	public void testSelectEntitiesQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, true, false )
					.add( AuditEntity.id().eq( id1 ) )
					.getResultList();

			assertEquals( 2, result.size() );

			assertEquals( new StrIntTestEntity( "a", 10, id1 ), result.get( 0 ) );
			assertEquals( new StrIntTestEntity( "aBc", 10, id1 ), result.get( 1 ) );
		} );
	}

	@Test
	public void testSelectEntitiesAndRevisionsQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.add( AuditEntity.id().eq( id1 ) )
					.getResultList();

			assertEquals( 3, result.size() );

			assertEquals( new StrIntTestEntity( "a", 10, id1 ), ((Object[]) result.get( 0 ))[0] );
			assertEquals( new StrIntTestEntity( "aBc", 10, id1 ), ((Object[]) result.get( 1 ))[0] );
			assertEquals( new StrIntTestEntity( null, null, id1 ), ((Object[]) result.get( 2 ))[0] );

			assertEquals( 1, ((SequenceIdRevisionEntity) ((Object[]) result.get( 0 ))[1]).getId() );
			assertEquals( 2, ((SequenceIdRevisionEntity) ((Object[]) result.get( 1 ))[1]).getId() );
			assertEquals( 4, ((SequenceIdRevisionEntity) ((Object[]) result.get( 2 ))[1]).getId() );

			assertEquals( RevisionType.ADD, ((Object[]) result.get( 0 ))[2] );
			assertEquals( RevisionType.MOD, ((Object[]) result.get( 1 ))[2] );
			assertEquals( RevisionType.DEL, ((Object[]) result.get( 2 ))[2] );
		} );
	}

	@Test
	public void testSelectRevisionTypeQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.addProjection( AuditEntity.revisionType() )
					.add( AuditEntity.id().eq( id1 ) )
					.addOrder( AuditEntity.revisionNumber().asc() )
					.getResultList();

			assertEquals( 3, result.size() );

			assertEquals( RevisionType.ADD, result.get( 0 ) );
			assertEquals( RevisionType.MOD, result.get( 1 ) );
			assertEquals( RevisionType.DEL, result.get( 2 ) );
		} );
	}

	@Test
	public void testEmptyRevisionOfEntityQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.getResultList();

			assertEquals( 7, result.size() );
		} );
	}

	@Test
	public void testEmptyConjunctionRevisionOfEntityQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.add( AuditEntity.conjunction() )
					.getResultList();

			assertEquals( 7, result.size() );
		} );
	}

	@Test
	public void testEmptyDisjunctionRevisionOfEntityQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.add( AuditEntity.disjunction() )
					.getResultList();

			assertEquals( 0, result.size() );
		} );
	}

	@Test
	public void testEntitiesAddedAtRevision(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			StrIntTestEntity site1 = new StrIntTestEntity( "a", 10, id1 );
			StrIntTestEntity site2 = new StrIntTestEntity( "a", 10, id2 );
			StrIntTestEntity site3 = new StrIntTestEntity( "b", 5, id3 );

			List result = AuditReaderFactory.get( em ).createQuery().forEntitiesModifiedAtRevision(
					StrIntTestEntity.class,
					StrIntTestEntity.class.getName(),
					1
			).getResultList();
			RevisionType revisionType = (RevisionType) AuditReaderFactory.get( em ).createQuery().forEntitiesModifiedAtRevision(
					StrIntTestEntity.class,
					1
			)
					.addProjection( AuditEntity.revisionType() ).add( AuditEntity.id().eq( id1 ) )
					.getSingleResult();

			assertTrue( TestTools.checkCollection( result, site1, site2, site3 ) );
			assertEquals( RevisionType.ADD, revisionType );
		} );
	}

	@Test
	public void testEntitiesChangedAtRevision(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			StrIntTestEntity site1 = new StrIntTestEntity( "aBc", 10, id1 );
			StrIntTestEntity site2 = new StrIntTestEntity( "a", 20, id2 );

			List result = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesModifiedAtRevision( StrIntTestEntity.class, 2 )
					.getResultList();
			RevisionType revisionType = (RevisionType) AuditReaderFactory.get( em ).createQuery().forEntitiesModifiedAtRevision(
					StrIntTestEntity.class,
					2
			)
					.addProjection( AuditEntity.revisionType() ).add( AuditEntity.id().eq( id1 ) )
					.getSingleResult();

			assertTrue( TestTools.checkCollection( result, site1, site2 ) );
			assertEquals( RevisionType.MOD, revisionType );
		} );
	}

	@Test
	public void testEntitiesRemovedAtRevision(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			StrIntTestEntity site1 = new StrIntTestEntity( null, null, id1 );

			List result = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesModifiedAtRevision( StrIntTestEntity.class, 4 )
					.getResultList();
			RevisionType revisionType = (RevisionType) AuditReaderFactory.get( em ).createQuery().forEntitiesModifiedAtRevision(
					StrIntTestEntity.class,
					4
			)
					.addProjection( AuditEntity.revisionType() ).add( AuditEntity.id().eq( id1 ) )
					.getSingleResult();

			assertTrue( TestTools.checkCollection( result, site1 ) );
			assertEquals( RevisionType.DEL, revisionType );
		} );
	}

	@Test
	public void testEntityNotModifiedAtRevision(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List result = AuditReaderFactory.get( em ).createQuery().forEntitiesModifiedAtRevision( StrIntTestEntity.class, 3 )
					.add( AuditEntity.id().eq( id1 ) ).getResultList();
			assertTrue( result.isEmpty() );
		} );
	}

	@Test
	public void testNoEntitiesModifiedAtRevision(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List result = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesModifiedAtRevision( StrIntTestEntity.class, 5 )
					.getResultList();
			assertTrue( result.isEmpty() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-7800")
	public void testBetweenInsideDisjunction(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, true, true )
					.add(
							AuditEntity.disjunction()
									.add( AuditEntity.property( "number" ).between( 0, 5 ) )
									.add( AuditEntity.property( "number" ).between( 20, 100 ) )
					)
					.getResultList();

			for ( Object o : result ) {
				StrIntTestEntity entity = (StrIntTestEntity) o;
				int number = entity.getNumber();
				assertTrue( (number >= 0 && number <= 5) || (number >= 20 && number <= 100) );
			}
		} );
	}

	@Test
	@JiraKey(value = "HHH-8495")
	public void testIlike(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			StrIntTestEntity site1 = new StrIntTestEntity( "aBc", 10, id1 );

			StrIntTestEntity result = (StrIntTestEntity) AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, true, true )
					.add( AuditEntity.property( "str1" ).ilike( "abc" ) )
					.getSingleResult();

			assertEquals( site1, result );
		} );
	}

	@Test
	@JiraKey(value = "HHH-8495")
	public void testIlikeWithMatchMode(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			StrIntTestEntity site1 = new StrIntTestEntity( "aBc", 10, id1 );

			StrIntTestEntity result = (StrIntTestEntity) AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, true, true )
					.add( AuditEntity.property( "str1" ).ilike( "BC", MatchMode.ANYWHERE ) )
					.getSingleResult();

			assertEquals( site1, result );
		} );
	}

	@Test
	@JiraKey(value = "HHH-8567")
	public void testIdPropertyRestriction(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			StrIntTestEntity ver2 = (StrIntTestEntity) AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( StrIntTestEntity.class, 2 )
					.add( AuditEntity.property( "id" ).eq( id2 ) )
					.getSingleResult();

			assertEquals( new StrIntTestEntity( "a", 20, id2 ), ver2 );
		} );
	}

	@Test
	@JiraKey(value = "HHH-8567")
	public void testMultipleIdPropertyRestriction(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			MulIdTestEntity ver2 = (MulIdTestEntity) AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( MulIdTestEntity.class, 2 )
					.add( AuditEntity.property( "id1" ).eq( mulId1.getId1() ) )
					.add( AuditEntity.property( "id2" ).eq( mulId1.getId2() ) )
					.getSingleResult();

			assertEquals( new MulIdTestEntity( mulId1.getId1(), mulId1.getId2(), "data" ), ver2 );
		} );
	}

	@Test
	@JiraKey(value = "HHH-8567")
	public void testEmbeddedIdPropertyRestriction(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			EmbIdTestEntity ver2 = (EmbIdTestEntity) AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( EmbIdTestEntity.class, 2 )
					.add( AuditEntity.property( "id.x" ).eq( embId1.getX() ) )
					.add( AuditEntity.property( "id.y" ).eq( embId1.getY() ) )
					.getSingleResult();

			assertEquals( new EmbIdTestEntity( embId1, "something" ), ver2 );
		} );
	}
}
