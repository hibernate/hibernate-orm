/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.proxy;

import org.hibernate.Hibernate;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.entities.IntTestPrivSeqEntity;
import org.hibernate.orm.test.envers.entities.StrTestPrivSeqEntity;
import org.hibernate.orm.test.envers.entities.UnversionedStrTestEntity;
import org.hibernate.orm.test.envers.entities.collection.StringSetEntity;
import org.hibernate.orm.test.envers.entities.manytomany.ListOwnedEntity;
import org.hibernate.orm.test.envers.entities.manytomany.ListOwningEntity;
import org.hibernate.orm.test.envers.entities.manytomany.SetOwnedEntity;
import org.hibernate.orm.test.envers.entities.manytomany.SetOwningEntity;
import org.hibernate.orm.test.envers.entities.manytomany.unidirectional.M2MIndexedListTargetNotAuditedEntity;
import org.hibernate.orm.test.envers.entities.onetomany.CollectionRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.CollectionRefIngEntity;
import org.hibernate.orm.test.envers.entities.onetomany.SetRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.SetRefIngEntity;
import org.hibernate.orm.test.envers.integration.manytomany.ternary.TernaryMapEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-5845")
@Jpa(annotatedClasses = {
		SetRefEdEntity.class, SetRefIngEntity.class, SetOwnedEntity.class, SetOwningEntity.class,
		StringSetEntity.class, UnversionedStrTestEntity.class, M2MIndexedListTargetNotAuditedEntity.class,
		TernaryMapEntity.class, StrTestPrivSeqEntity.class, IntTestPrivSeqEntity.class,
		CollectionRefEdEntity.class, CollectionRefIngEntity.class, ListOwnedEntity.class, ListOwningEntity.class
}, integrationSettings = @Setting(name = EnversSettings.STORE_DATA_AT_DELETE, value = "true"))
@EnversTest
public class RemovedObjectQueryTest {
	private Integer stringSetId = null;
	private Integer ternaryMapId = null;
	private UnversionedStrTestEntity unversionedEntity1 = null;
	private UnversionedStrTestEntity unversionedEntity2 = null;
	private StrTestPrivSeqEntity stringEntity1 = null;
	private StrTestPrivSeqEntity stringEntity2 = null;
	private IntTestPrivSeqEntity intEntity1 = null;
	private IntTestPrivSeqEntity intEntity2 = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			SetRefEdEntity refEdEntity1 = new SetRefEdEntity( 1, "Demo Data 1" );
			SetRefIngEntity refIngEntity1 = new SetRefIngEntity( 2, "Example Data 1", refEdEntity1 );

			// Revision 1
			em.getTransaction().begin();
			em.persist( refEdEntity1 );
			em.persist( refIngEntity1 );
			em.getTransaction().commit();

			// Revision 2 - removing both object in the same revision
			em.getTransaction().begin();
			refEdEntity1 = em.find( SetRefEdEntity.class, 1 );
			refIngEntity1 = em.find( SetRefIngEntity.class, 2 );
			em.remove( refIngEntity1 );
			em.remove( refEdEntity1 );
			em.getTransaction().commit();

			SetRefEdEntity refEdEntity2 = new SetRefEdEntity( 3, "Demo Data 2" );
			SetRefIngEntity refIngEntity2 = new SetRefIngEntity( 4, "Example Data 2", refEdEntity2 );

			// Revision 3
			em.getTransaction().begin();
			em.persist( refEdEntity2 );
			em.persist( refIngEntity2 );
			em.getTransaction().commit();

			// Revision 4 - removing child object
			em.getTransaction().begin();
			refIngEntity2 = em.find( SetRefIngEntity.class, 4 );
			em.remove( refIngEntity2 );
			em.getTransaction().commit();

			// Revision 5 - removing parent object
			em.getTransaction().begin();
			refEdEntity2 = em.find( SetRefEdEntity.class, 3 );
			em.remove( refEdEntity2 );
			em.getTransaction().commit();

			SetOwningEntity setOwningEntity1 = new SetOwningEntity( 5, "Demo Data 1" );
			SetOwnedEntity setOwnedEntity1 = new SetOwnedEntity( 6, "Example Data 1" );
			Set<SetOwningEntity> owning = new HashSet<SetOwningEntity>();
			Set<SetOwnedEntity> owned = new HashSet<SetOwnedEntity>();
			owning.add( setOwningEntity1 );
			owned.add( setOwnedEntity1 );
			setOwningEntity1.setReferences( owned );
			setOwnedEntity1.setReferencing( owning );

			// Revision 6
			em.getTransaction().begin();
			em.persist( setOwnedEntity1 );
			em.persist( setOwningEntity1 );
			em.getTransaction().commit();

			// Revision 7 - removing both object in the same revision
			em.getTransaction().begin();
			setOwnedEntity1 = em.find( SetOwnedEntity.class, 6 );
			setOwningEntity1 = em.find( SetOwningEntity.class, 5 );
			em.remove( setOwningEntity1 );
			em.remove( setOwnedEntity1 );
			em.getTransaction().commit();

			SetOwningEntity setOwningEntity2 = new SetOwningEntity( 7, "Demo Data 2" );
			SetOwnedEntity setOwnedEntity2 = new SetOwnedEntity( 8, "Example Data 2" );
			owning = new HashSet<SetOwningEntity>();
			owned = new HashSet<SetOwnedEntity>();
			owning.add( setOwningEntity2 );
			owned.add( setOwnedEntity2 );
			setOwningEntity2.setReferences( owned );
			setOwnedEntity2.setReferencing( owning );

			// Revision 8
			em.getTransaction().begin();
			em.persist( setOwnedEntity2 );
			em.persist( setOwningEntity2 );
			em.getTransaction().commit();

			// Revision 9 - removing first object
			em.getTransaction().begin();
			setOwningEntity2 = em.find( SetOwningEntity.class, 7 );
			setOwnedEntity2.getReferencing().remove( setOwningEntity2 );
			em.remove( setOwningEntity2 );
			em.getTransaction().commit();

			// Revision 10 - removing second object
			em.getTransaction().begin();
			setOwnedEntity2 = em.find( SetOwnedEntity.class, 8 );
			em.remove( setOwnedEntity2 );
			em.getTransaction().commit();

			StringSetEntity stringSetEntity = new StringSetEntity();
			stringSetEntity.getStrings().add( "string 1" );
			stringSetEntity.getStrings().add( "string 2" );

			// Revision 11
			em.getTransaction().begin();
			em.persist( stringSetEntity );
			em.getTransaction().commit();

			stringSetId = stringSetEntity.getId();

			// Revision 12 - removing element collection
			em.getTransaction().begin();
			stringSetEntity = em.find( StringSetEntity.class, stringSetEntity.getId() );
			em.remove( stringSetEntity );
			em.getTransaction().commit();

			// Revision 13
			em.getTransaction().begin();
			unversionedEntity1 = new UnversionedStrTestEntity( "string 1" );
			unversionedEntity2 = new UnversionedStrTestEntity( "string 2" );
			M2MIndexedListTargetNotAuditedEntity relationNotAuditedEntity = new M2MIndexedListTargetNotAuditedEntity(
					1,
					"Parent"
			);
			relationNotAuditedEntity.getReferences().add( unversionedEntity1 );
			relationNotAuditedEntity.getReferences().add( unversionedEntity2 );
			em.persist( unversionedEntity1 );
			em.persist( unversionedEntity2 );
			em.persist( relationNotAuditedEntity );
			em.getTransaction().commit();

			// Revision 14 - removing entity with unversioned relation
			em.getTransaction().begin();
			relationNotAuditedEntity = em.find(
					M2MIndexedListTargetNotAuditedEntity.class,
					relationNotAuditedEntity.getId()
			);
			em.remove( relationNotAuditedEntity );
			em.getTransaction().commit();

			stringEntity1 = new StrTestPrivSeqEntity( "Value 1" );
			stringEntity2 = new StrTestPrivSeqEntity( "Value 2" );
			intEntity1 = new IntTestPrivSeqEntity( 1 );
			intEntity2 = new IntTestPrivSeqEntity( 2 );
			TernaryMapEntity mapEntity = new TernaryMapEntity();
			mapEntity.getMap().put( intEntity1, stringEntity1 );
			mapEntity.getMap().put( intEntity2, stringEntity2 );

			// Revision 15
			em.getTransaction().begin();
			em.persist( stringEntity1 );
			em.persist( stringEntity2 );
			em.persist( intEntity1 );
			em.persist( intEntity2 );
			em.persist( mapEntity );
			em.getTransaction().commit();

			ternaryMapId = mapEntity.getId();

			// Revision 16 - updating ternary map
			em.getTransaction().begin();
			intEntity2 = em.find( IntTestPrivSeqEntity.class, intEntity2.getId() );
			intEntity2.setNumber( 3 );
			intEntity2 = em.merge( intEntity2 );
			stringEntity2 = em.find( StrTestPrivSeqEntity.class, stringEntity2.getId() );
			stringEntity2.setStr( "Value 3" );
			stringEntity2 = em.merge( stringEntity2 );
			em.getTransaction().commit();

			// Revision 17 - removing ternary map
			em.getTransaction().begin();
			mapEntity = em.find( TernaryMapEntity.class, mapEntity.getId() );
			em.remove( mapEntity );
			em.getTransaction().commit();

			CollectionRefEdEntity collEd1 = new CollectionRefEdEntity( 1, "data_ed_1" );
			CollectionRefIngEntity collIng1 = new CollectionRefIngEntity( 2, "data_ing_1", collEd1 );
			collEd1.setReffering( new ArrayList<CollectionRefIngEntity>() );
			collEd1.getReffering().add( collIng1 );

			// Revision 18 - testing one-to-many collection
			em.getTransaction().begin();
			em.persist( collEd1 );
			em.persist( collIng1 );
			em.getTransaction().commit();

			// Revision 19
			em.getTransaction().begin();
			collIng1 = em.find( CollectionRefIngEntity.class, collIng1.getId() );
			collIng1.setData( "modified data_ing_1" );
			collIng1 = em.merge( collIng1 );
			em.getTransaction().commit();

			// Revision 20
			em.getTransaction().begin();
			collEd1 = em.find( CollectionRefEdEntity.class, collEd1.getId() );
			collIng1 = em.find( CollectionRefIngEntity.class, collIng1.getId() );
			em.remove( collIng1 );
			em.remove( collEd1 );
			em.getTransaction().commit();

			ListOwnedEntity listEd1 = new ListOwnedEntity( 1, "data_ed_1" );
			ListOwningEntity listIng1 = new ListOwningEntity( 2, "data_ing_1" );
			listEd1.setReferencing( new ArrayList<ListOwningEntity>() );
			listIng1.setReferences( new ArrayList<ListOwnedEntity>() );
			listEd1.getReferencing().add( listIng1 );
			listIng1.getReferences().add( listEd1 );

			// Revision 21 - testing many-to-many collection
			em.getTransaction().begin();
			em.persist( listEd1 );
			em.persist( listIng1 );
			em.getTransaction().commit();

			// Revision 22
			em.getTransaction().begin();
			listIng1 = em.find( ListOwningEntity.class, listIng1.getId() );
			listIng1.setData( "modified data_ing_1" );
			listIng1 = em.merge( listIng1 );
			em.getTransaction().commit();

			// Revision 23
			em.getTransaction().begin();
			listIng1 = em.find( ListOwningEntity.class, listIng1.getId() );
			listEd1 = em.find( ListOwnedEntity.class, listEd1.getId() );
			em.remove( listIng1 );
			em.remove( listEd1 );
			em.getTransaction().commit();
		} );
	}

	@Test
	@SkipForDialect(value = HSQLDialect.class, comment = "No idea why this fails. Looks like a HSQLDB bug")
	public void testTernaryMap(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final TernaryMapEntity ternaryMap = new TernaryMapEntity();
			ternaryMap.setId( ternaryMapId );
			ternaryMap.getMap().put( intEntity1, stringEntity1 );
			ternaryMap.getMap().put( new IntTestPrivSeqEntity( 2, intEntity2.getId() ),
					new StrTestPrivSeqEntity( "Value 2", stringEntity2.getId() ) );

			TernaryMapEntity entity = AuditReaderFactory.get( em ).find( TernaryMapEntity.class, ternaryMapId, 15 );

			assertEquals( ternaryMap.getMap(), entity.getMap() );

			ternaryMap.getMap().clear();
			ternaryMap.getMap().put( intEntity1, stringEntity1 );
			ternaryMap.getMap().put( intEntity2, stringEntity2 );

			entity = AuditReaderFactory.get( em ).find( TernaryMapEntity.class, ternaryMapId, 16 );

			assertEquals( ternaryMap.getMap(), entity.getMap() );

			List queryResult = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( TernaryMapEntity.class, false, true )
					.add( AuditEntity.id().eq( ternaryMapId ) )
					.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
					.getResultList();
			Object[] objArray = (Object[]) queryResult.get( 0 );

			assertEquals( 17, getRevisionNumber( objArray[1] ) );

			entity = (TernaryMapEntity) objArray[0];
			assertEquals( ternaryMap.getMap(), entity.getMap() );
		} );
	}

	@Test
	public void testUnversionedRelation(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List queryResult = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( M2MIndexedListTargetNotAuditedEntity.class, false, true )
					.add( AuditEntity.id().eq( 1 ) )
					.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
					.getResultList();
			Object[] objArray = (Object[]) queryResult.get( 0 );

			assertEquals( 14, getRevisionNumber( objArray[1] ) );

			M2MIndexedListTargetNotAuditedEntity relationNotAuditedEntity = (M2MIndexedListTargetNotAuditedEntity) objArray[0];
			assertTrue(
					TestTools.checkCollection(
							relationNotAuditedEntity.getReferences(),
							unversionedEntity1, unversionedEntity2
					)
			);
		} );
	}

	@Test
	public void testElementCollection(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List queryResult = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StringSetEntity.class, false, true )
					.add( AuditEntity.id().eq( stringSetId ) )
					.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
					.getResultList();
			Object[] objArray = (Object[]) queryResult.get( 0 );

			assertEquals( 12, getRevisionNumber( objArray[1] ) );

			StringSetEntity stringSetEntity = (StringSetEntity) objArray[0];
			assertEquals( TestTools.makeSet( "string 1", "string 2" ), stringSetEntity.getStrings() );
		} );
	}

	// One to many tests.

	@Test
	public void testOneToManyCollectionSemantics(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final CollectionRefEdEntity edVer1 = new CollectionRefEdEntity( 1, "data_ed_1" );
			final CollectionRefIngEntity ingVer1 = new CollectionRefIngEntity( 2, "data_ing_1" );
			final CollectionRefIngEntity ingVer2 = new CollectionRefIngEntity( 2, "modified data_ing_1" );

			CollectionRefEdEntity entity = AuditReaderFactory.get( em ).find( CollectionRefEdEntity.class, 1, 18 );

			assertEquals( edVer1, entity );
			assertTrue( TestTools.checkCollection( entity.getReffering(), ingVer1 ) );

			entity = AuditReaderFactory.get( em ).find( CollectionRefEdEntity.class, 1, 19 );

			assertTrue( TestTools.checkCollection( entity.getReffering(), ingVer2 ) );

			List queryResult = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( CollectionRefEdEntity.class, false, true )
					.add( AuditEntity.id().eq( 1 ) )
					.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
					.getResultList();
			Object[] objArray = (Object[]) queryResult.get( 0 );

			assertEquals( 20, getRevisionNumber( objArray[1] ) );

			entity = (CollectionRefEdEntity) objArray[0];
			assertEquals( "data_ed_1", entity.getData() );
			assertTrue( TestTools.checkCollection( entity.getReffering(), ingVer2 ) );
		} );
	}

	@Test
	public void testReferencedOneToManySameRevision(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List queryResult = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( SetRefIngEntity.class, false, true )
					.add( AuditEntity.id().eq( 2 ) )
					.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
					.getResultList();
			Object[] objArray = (Object[]) queryResult.get( 0 );

			assertEquals( 2, getRevisionNumber( objArray[1] ) );

			SetRefIngEntity refIngEntity = (SetRefIngEntity) objArray[0];
			assertEquals( "Example Data 1", refIngEntity.getData() );

			Hibernate.initialize( refIngEntity.getReference() );
			assertEquals( "Demo Data 1", refIngEntity.getReference().getData() );
		} );
	}

	@Test
	public void testReferringOneToManySameRevision(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List queryResult = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( SetRefEdEntity.class, false, true )
					.add( AuditEntity.id().eq( 1 ) )
					.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
					.getResultList();
			Object[] objArray = (Object[]) queryResult.get( 0 );

			assertEquals( 2, getRevisionNumber( objArray[1] ) );

			SetRefEdEntity refEdEntity = (SetRefEdEntity) objArray[0];
			assertEquals( "Demo Data 1", refEdEntity.getData() );

			Hibernate.initialize( refEdEntity.getReffering() );
			assertEquals(
					TestTools.makeSet( new SetRefIngEntity( 2, "Example Data 1" ) ),
					refEdEntity.getReffering()
			);
		} );
	}

	@Test
	public void testReferencedOneToManyDifferentRevisions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List queryResult = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( SetRefIngEntity.class, false, true )
					.add( AuditEntity.id().eq( 4 ) )
					.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
					.getResultList();
			Object[] objArray = (Object[]) queryResult.get( 0 );

			assertEquals( 4, getRevisionNumber( objArray[1] ) );

			SetRefIngEntity refIngEntity = (SetRefIngEntity) objArray[0];
			assertEquals( "Example Data 2", refIngEntity.getData() );

			Hibernate.initialize( refIngEntity.getReference() );
			assertEquals( "Demo Data 2", refIngEntity.getReference().getData() );
		} );
	}

	@Test
	public void testReferringOneToManyDifferentRevisions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List queryResult = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( SetRefEdEntity.class, false, true )
					.add( AuditEntity.id().eq( 3 ) )
					.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
					.getResultList();
			Object[] objArray = (Object[]) queryResult.get( 0 );

			assertEquals( 5, getRevisionNumber( objArray[1] ) );

			SetRefEdEntity refEdEntity = (SetRefEdEntity) objArray[0];
			assertEquals( "Demo Data 2", refEdEntity.getData() );

			Hibernate.initialize( refEdEntity.getReffering() );
			assertTrue( refEdEntity.getReffering().isEmpty() );

			// After commit in revision four, child entity has been removed.
			queryResult = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( SetRefEdEntity.class, false, true )
					.add( AuditEntity.id().eq( 3 ) )
					.add( AuditEntity.revisionNumber().eq( 4 ) )
					.getResultList();
			objArray = (Object[]) queryResult.get( 0 );

			refEdEntity = (SetRefEdEntity) objArray[0];
			assertEquals( "Demo Data 2", refEdEntity.getData() );

			Hibernate.initialize( refEdEntity.getReffering() );
			assertTrue( refEdEntity.getReffering().isEmpty() );
		} );
	}

	// Many to many tests.

	@Test
	public void testManyToManyCollectionSemantics(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final ListOwnedEntity edVer1 = new ListOwnedEntity( 1, "data_ed_1" );
			final ListOwningEntity ingVer1 = new ListOwningEntity( 2, "data_ing_1" );
			final ListOwningEntity ingVer2 = new ListOwningEntity( 2, "modified data_ing_1" );

			ListOwnedEntity entity = AuditReaderFactory.get( em ).find( ListOwnedEntity.class, 1, 21 );

			assertEquals( edVer1, entity );
			assertTrue( TestTools.checkCollection( entity.getReferencing(), ingVer1 ) );

			entity = AuditReaderFactory.get( em ).find( ListOwnedEntity.class, 1, 22 );

			assertTrue( TestTools.checkCollection( entity.getReferencing(), ingVer2 ) );

			List queryResult = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( ListOwnedEntity.class, false, true )
					.add( AuditEntity.id().eq( 1 ) )
					.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
					.getResultList();
			Object[] objArray = (Object[]) queryResult.get( 0 );

			assertEquals( 23, getRevisionNumber( objArray[1] ) );

			entity = (ListOwnedEntity) objArray[0];
			assertEquals( "data_ed_1", entity.getData() );
			assertTrue( TestTools.checkCollection( entity.getReferencing(), ingVer2 ) );
		} );
	}

	@Test
	public void testOwnedManyToManySameRevision(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List queryResult = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( SetOwningEntity.class, false, true )
					.add( AuditEntity.id().eq( 5 ) )
					.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
					.getResultList();
			Object[] objArray = (Object[]) queryResult.get( 0 );

			assertEquals( 7, getRevisionNumber( objArray[1] ) );

			SetOwningEntity setOwningEntity = (SetOwningEntity) objArray[0];
			assertEquals( "Demo Data 1", setOwningEntity.getData() );

			Hibernate.initialize( setOwningEntity.getReferences() );
			assertEquals(
					TestTools.makeSet( new SetOwnedEntity( 6, "Example Data 1" ) ),
					setOwningEntity.getReferences()
			);
		} );
	}

	@Test
	public void testOwningManyToManySameRevision(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List queryResult = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( SetOwnedEntity.class, false, true )
					.add( AuditEntity.id().eq( 6 ) )
					.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
					.getResultList();
			Object[] objArray = (Object[]) queryResult.get( 0 );

			assertEquals( 7, getRevisionNumber( objArray[1] ) );

			SetOwnedEntity setOwnedEntity = (SetOwnedEntity) objArray[0];
			assertEquals( "Example Data 1", setOwnedEntity.getData() );

			Hibernate.initialize( setOwnedEntity.getReferencing() );
			assertEquals(
					TestTools.makeSet( new SetOwningEntity( 5, "Demo Data 1" ) ),
					setOwnedEntity.getReferencing()
			);
		} );
	}

	@Test
	public void testOwnedManyToManyDifferentRevisions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List queryResult = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( SetOwningEntity.class, false, true )
					.add( AuditEntity.id().eq( 7 ) )
					.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
					.getResultList();
			Object[] objArray = (Object[]) queryResult.get( 0 );

			assertEquals( 9, getRevisionNumber( objArray[1] ) );

			SetOwningEntity setOwningEntity = (SetOwningEntity) objArray[0];
			assertEquals( "Demo Data 2", setOwningEntity.getData() );

			Hibernate.initialize( setOwningEntity.getReferences() );
			assertEquals(
					TestTools.makeSet( new SetOwnedEntity( 8, "Example Data 2" ) ),
					setOwningEntity.getReferences()
			);
		} );
	}

	@Test
	public void testOwningManyToManyDifferentRevisions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List queryResult = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( SetOwnedEntity.class, false, true )
					.add( AuditEntity.id().eq( 8 ) )
					.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
					.getResultList();
			Object[] objArray = (Object[]) queryResult.get( 0 );

			assertEquals( 10, getRevisionNumber( objArray[1] ) );

			SetOwnedEntity setOwnedEntity = (SetOwnedEntity) objArray[0];
			assertEquals( "Example Data 2", setOwnedEntity.getData() );

			Hibernate.initialize( setOwnedEntity.getReferencing() );
			assertTrue( setOwnedEntity.getReferencing().isEmpty() );

			// After commit in revision nine, related entity has been removed.
			queryResult = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( SetOwnedEntity.class, false, true )
					.add( AuditEntity.id().eq( 8 ) )
					.add( AuditEntity.revisionNumber().eq( 9 ) )
					.getResultList();
			objArray = (Object[]) queryResult.get( 0 );

			setOwnedEntity = (SetOwnedEntity) objArray[0];
			assertEquals( "Example Data 2", setOwnedEntity.getData() );

			Hibernate.initialize( setOwnedEntity.getReferencing() );
			assertTrue( setOwnedEntity.getReferencing().isEmpty() );
		} );
	}

	private Number getRevisionNumber(Object revisionEntity) {
		return ((SequenceIdRevisionEntity) revisionEntity).getId();
	}
}
