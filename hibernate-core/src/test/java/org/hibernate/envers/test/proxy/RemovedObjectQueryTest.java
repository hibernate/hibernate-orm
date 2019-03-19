/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.proxy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.IntTestPrivSeqEntity;
import org.hibernate.envers.test.support.domains.basic.StrTestPrivSeqEntity;
import org.hibernate.envers.test.support.domains.basic.UnversionedStrTestEntity;
import org.hibernate.envers.test.support.domains.collections.StringSetEntity;
import org.hibernate.envers.test.support.domains.manytomany.ListOwnedEntity;
import org.hibernate.envers.test.support.domains.manytomany.ListOwningEntity;
import org.hibernate.envers.test.support.domains.manytomany.SetOwnedEntity;
import org.hibernate.envers.test.support.domains.manytomany.SetOwningEntity;
import org.hibernate.envers.test.support.domains.manytomany.ternary.TernaryMapEntity;
import org.hibernate.envers.test.support.domains.manytomany.unidirectional.M2MIndexedListTargetNotAuditedEntity;
import org.hibernate.envers.test.support.domains.onetomany.CollectionRefEdEntity;
import org.hibernate.envers.test.support.domains.onetomany.CollectionRefIngEntity;
import org.hibernate.envers.test.support.domains.onetomany.SetRefEdEntity;
import org.hibernate.envers.test.support.domains.onetomany.SetRefIngEntity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-5845")
public class RemovedObjectQueryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer stringSetId = null;
	private Integer ternaryMapId = null;
	private Integer relationNotAuditedId = null;
	private Integer collectionRefEdId = null;
	private Integer collectionRefingId = null;
	private Integer listOwningId = null;
	private Integer listOwnedId = null;
	private UnversionedStrTestEntity unversionedEntity1 = null;
	private UnversionedStrTestEntity unversionedEntity2 = null;
	private StrTestPrivSeqEntity stringEntity1 = null;
	private StrTestPrivSeqEntity stringEntity2 = null;
	private IntTestPrivSeqEntity intEntity1 = null;
	private IntTestPrivSeqEntity intEntity2 = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				SetRefEdEntity.class,
				SetRefIngEntity.class,
				SetOwnedEntity.class,
				SetOwningEntity.class,
				StringSetEntity.class,
				UnversionedStrTestEntity.class,
				M2MIndexedListTargetNotAuditedEntity.class,
				TernaryMapEntity.class,
				StrTestPrivSeqEntity.class,
				IntTestPrivSeqEntity.class,
				CollectionRefEdEntity.class,
				CollectionRefIngEntity.class,
				ListOwnedEntity.class,
				ListOwningEntity.class
		};
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.STORE_DATA_AT_DELETE, "true" );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					SetRefEdEntity refEdEntity1 = new SetRefEdEntity( 1, "Demo Data 1" );
					SetRefIngEntity refIngEntity1 = new SetRefIngEntity( 2, "Example Data 1", refEdEntity1 );

					entityManager.persist( refEdEntity1 );
					entityManager.persist( refIngEntity1 );
				},

				// Revision 2 - removing both object in the same revision
				entityManager -> {
					SetRefEdEntity refEdEntity1 = entityManager.find( SetRefEdEntity.class, 1 );
					SetRefIngEntity refIngEntity1 = entityManager.find( SetRefIngEntity.class, 2 );
					entityManager.remove( refIngEntity1 );
					entityManager.remove( refEdEntity1 );
				},

				// Revision 3
				entityManager -> {
					SetRefEdEntity refEdEntity2 = new SetRefEdEntity( 3, "Demo Data 2" );
					SetRefIngEntity refIngEntity2 = new SetRefIngEntity( 4, "Example Data 2", refEdEntity2 );

					entityManager.persist( refEdEntity2 );
					entityManager.persist( refIngEntity2 );
				},

				// Revision 4 - removing child object
				entityManager -> {
					SetRefIngEntity refIngEntity2 = entityManager.find( SetRefIngEntity.class, 4 );
					entityManager.remove( refIngEntity2 );
				},

				// Revision 5 - removing parent object
				entityManager -> {
					SetRefEdEntity refEdEntity2 = entityManager.find( SetRefEdEntity.class, 3 );
					entityManager.remove( refEdEntity2 );
				},

				// Revision 6
				entityManager -> {
					SetOwningEntity setOwningEntity1 = new SetOwningEntity( 5, "Demo Data 1" );
					SetOwnedEntity setOwnedEntity1 = new SetOwnedEntity( 6, "Example Data 1" );
					Set<SetOwningEntity> owning = new HashSet<>();
					Set<SetOwnedEntity> owned = new HashSet<>();
					owning.add( setOwningEntity1 );
					owned.add( setOwnedEntity1 );
					setOwningEntity1.setReferences( owned );
					setOwnedEntity1.setReferencing( owning );

					entityManager.persist( setOwnedEntity1 );
					entityManager.persist( setOwningEntity1 );
				},

				// Revision 7 - removing both object in the same revision
				entityManager -> {
					SetOwnedEntity setOwnedEntity1 = entityManager.find( SetOwnedEntity.class, 6 );
					SetOwningEntity setOwningEntity1 = entityManager.find( SetOwningEntity.class, 5 );
					entityManager.remove( setOwningEntity1 );
					entityManager.remove( setOwnedEntity1 );
				},

				// Revision 8
				entityManager -> {
					SetOwningEntity setOwningEntity2 = new SetOwningEntity( 7, "Demo Data 2" );
					SetOwnedEntity setOwnedEntity2 = new SetOwnedEntity( 8, "Example Data 2" );
					Set<SetOwningEntity>  owning = new HashSet<>();
					Set<SetOwnedEntity> owned = new HashSet<>();
					owning.add( setOwningEntity2 );
					owned.add( setOwnedEntity2 );
					setOwningEntity2.setReferences( owned );
					setOwnedEntity2.setReferencing( owning );

					entityManager.persist( setOwnedEntity2 );
					entityManager.persist( setOwningEntity2 );
				},

				// Revision 9 - removing first object
				entityManager -> {
					SetOwningEntity setOwningEntity2 = entityManager.find( SetOwningEntity.class, 7 );
					entityManager.remove( setOwningEntity2 );
				},

				// Revision 10 - removing second object
				entityManager -> {
					SetOwnedEntity setOwnedEntity2 = entityManager.find( SetOwnedEntity.class, 8 );
					entityManager.remove( setOwnedEntity2 );
				},

				// Revision 11
				entityManager -> {
					StringSetEntity stringSetEntity = new StringSetEntity();
					stringSetEntity.getStrings().add( "string 1" );
					stringSetEntity.getStrings().add( "string 2" );

					entityManager.persist( stringSetEntity );

					this.stringSetId = stringSetEntity.getId();
				},

				// Revision 12 - removing element collection
				entityManager -> {
					StringSetEntity stringSetEntity = entityManager.find( StringSetEntity.class, stringSetId );
					entityManager.remove( stringSetEntity );
				},

				// Revision 13
				entityManager -> {
					unversionedEntity1 = new UnversionedStrTestEntity( "string 1" );
					unversionedEntity2 = new UnversionedStrTestEntity( "string 2" );

					final M2MIndexedListTargetNotAuditedEntity relationNotAuditedEntity =
							new M2MIndexedListTargetNotAuditedEntity( 1, "Parent" );

					relationNotAuditedEntity.getReferences().add( unversionedEntity1 );
					relationNotAuditedEntity.getReferences().add( unversionedEntity2 );

					entityManager.persist( unversionedEntity1 );
					entityManager.persist( unversionedEntity2 );
					entityManager.persist( relationNotAuditedEntity );

					this.relationNotAuditedId = relationNotAuditedEntity.getId();
				},

				// Revision 14 - removing entity with non-audited relation
				entityManager -> {
					M2MIndexedListTargetNotAuditedEntity relationNotAuditedEntity = entityManager.find(
							M2MIndexedListTargetNotAuditedEntity.class,
							relationNotAuditedId
					);
					entityManager.remove( relationNotAuditedEntity );
				},

				// Revision 15
				entityManager -> {
					stringEntity1 = new StrTestPrivSeqEntity( "Value 1" );
					stringEntity2 = new StrTestPrivSeqEntity( "Value 2" );
					intEntity1 = new IntTestPrivSeqEntity( 1 );
					intEntity2 = new IntTestPrivSeqEntity( 2 );
					TernaryMapEntity mapEntity = new TernaryMapEntity();
					mapEntity.getMap().put( intEntity1, stringEntity1 );
					mapEntity.getMap().put( intEntity2, stringEntity2 );

					entityManager.persist( stringEntity1 );
					entityManager.persist( stringEntity2 );
					entityManager.persist( intEntity1 );
					entityManager.persist( intEntity2 );
					entityManager.persist( mapEntity );

					this.ternaryMapId = mapEntity.getId();
				},

				// Revision 16 - updating ternary map
				entityManager -> {
					intEntity2 = entityManager.find( IntTestPrivSeqEntity.class, intEntity2.getId() );
					intEntity2.setNumber( 3 );
					intEntity2 = entityManager.merge( intEntity2 );
					stringEntity2 = entityManager.find( StrTestPrivSeqEntity.class, stringEntity2.getId() );
					stringEntity2.setStr( "Value 3" );
					stringEntity2 = entityManager.merge( stringEntity2 );
				},

				// Revision 17 - removing ternary map
				entityManager -> {
					TernaryMapEntity mapEntity = entityManager.find( TernaryMapEntity.class, ternaryMapId );
					entityManager.remove( mapEntity );
				},

				// Revision 18 - Testing one-to-many collection
				entityManager -> {
					CollectionRefEdEntity collEd1 = new CollectionRefEdEntity( 1, "data_ed_1" );
					CollectionRefIngEntity collIng1 = new CollectionRefIngEntity( 2, "data_ing_1", collEd1 );
					collEd1.setReffering( new ArrayList<CollectionRefIngEntity>() );
					collEd1.getReffering().add( collIng1 );

					entityManager.persist( collEd1 );
					entityManager.persist( collIng1 );

					this.collectionRefEdId = collEd1.getId();
					this.collectionRefingId = collIng1.getId();
				},

				// Revision 19
				entityManager -> {
					CollectionRefIngEntity collIng1 = entityManager.find( CollectionRefIngEntity.class, collectionRefingId );
					collIng1.setData( "modified data_ing_1" );
					entityManager.merge( collIng1 );
				},

				// Revision 20
				entityManager -> {
					CollectionRefEdEntity collEd1 = entityManager.find( CollectionRefEdEntity.class, collectionRefEdId );
					CollectionRefIngEntity collIng1 = entityManager.find( CollectionRefIngEntity.class, collectionRefingId );
					entityManager.remove( collIng1 );
					entityManager.remove( collEd1 );
				},

				// Revision 21 - testing many-to-many collection
				entityManager -> {
					ListOwnedEntity listEd1 = new ListOwnedEntity( 1, "data_ed_1" );
					ListOwningEntity listIng1 = new ListOwningEntity( 2, "data_ing_1" );
					listEd1.setReferencing( new ArrayList<>() );
					listIng1.setReferences( new ArrayList<>() );
					listEd1.getReferencing().add( listIng1 );
					listIng1.getReferences().add( listEd1 );

					entityManager.persist( listEd1 );
					entityManager.persist( listIng1 );

					this.listOwnedId = listEd1.getId();
					this.listOwningId = listIng1.getId();
				},

				// Revision 22
				entityManager -> {
					ListOwningEntity listIng1 = entityManager.find( ListOwningEntity.class, listOwningId );
					listIng1.setData( "modified data_ing_1" );
					entityManager.merge( listIng1 );
				},

				// Revision 23
				entityManager -> {
					ListOwningEntity listIng1 = entityManager.find( ListOwningEntity.class, listOwningId );
					ListOwnedEntity listEd1 = entityManager.find( ListOwnedEntity.class, listOwnedId );
					entityManager.remove( listIng1 );
					entityManager.remove( listEd1 );
				}
		);
	}

	@DynamicTest
	public void testTernaryMap() {
		final TernaryMapEntity ternaryMap = new TernaryMapEntity();
		ternaryMap.setId( ternaryMapId );
		ternaryMap.getMap().put( intEntity1, stringEntity1 );
		ternaryMap.getMap().put( new IntTestPrivSeqEntity( 2, intEntity2.getId() ) , new StrTestPrivSeqEntity( "Value 2", stringEntity2.getId() ) );

		TernaryMapEntity entity = getAuditReader().find( TernaryMapEntity.class, ternaryMapId, 15 );
		assertThat( entity.getMap(), equalTo( ternaryMap.getMap() ) );

		ternaryMap.getMap().clear();
		ternaryMap.getMap().put( intEntity1, stringEntity1 );
		ternaryMap.getMap().put( intEntity2, stringEntity2 );

		entity = getAuditReader().find( TernaryMapEntity.class, ternaryMapId, 16 );
		assertThat( entity.getMap(), equalTo( ternaryMap.getMap() ) );

		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( TernaryMapEntity.class, false, true )
				.add( AuditEntity.id().eq( ternaryMapId ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );
		assertRevisionNumber( objArray[1], 17 );

		entity = (TernaryMapEntity) objArray[0];
		assertThat( entity.getMap(), equalTo( ternaryMap.getMap() ) );
	}

	@DynamicTest
	public void testUnversionedRelation() {
		List queryResult = getAuditReader().createQuery()
				.forRevisionsOfEntity( M2MIndexedListTargetNotAuditedEntity.class, false, true )
				.add( AuditEntity.id().eq( 1 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );
		assertRevisionNumber( objArray[1], 14 );

		M2MIndexedListTargetNotAuditedEntity relationNotAuditedEntity = (M2MIndexedListTargetNotAuditedEntity) objArray[0];
		assertThat( relationNotAuditedEntity.getReferences(), containsInAnyOrder( unversionedEntity1, unversionedEntity2 ) );
	}

	@DynamicTest
	public void testElementCollection() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( StringSetEntity.class, false, true )
				.add( AuditEntity.id().eq( stringSetId ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );
		assertRevisionNumber( objArray[1], 12 );

		StringSetEntity stringSetEntity = (StringSetEntity) objArray[0];
		assertThat( stringSetEntity.getStrings(), containsInAnyOrder( "string 1", "string 2" ) );
	}

	// One to many tests.

	@DynamicTest
	public void testOneToManyCollectionSemantics() {
		final CollectionRefEdEntity edVer1 = new CollectionRefEdEntity( 1, "data_ed_1" );
		final CollectionRefIngEntity ingVer1 = new CollectionRefIngEntity( 2, "data_ing_1" );
		final CollectionRefIngEntity ingVer2 = new CollectionRefIngEntity( 2, "modified data_ing_1" );

		CollectionRefEdEntity entity = getAuditReader().find( CollectionRefEdEntity.class, 1, 18 );
		assertThat( entity, equalTo( edVer1 ) );
		assertThat( entity.getReffering(), contains( ingVer1 ) );

		entity = getAuditReader().find( CollectionRefEdEntity.class, 1, 19 );
		assertThat( entity.getReffering(), contains( ingVer2 ) );

		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( CollectionRefEdEntity.class, false, true )
				.add( AuditEntity.id().eq( 1 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );
		assertRevisionNumber( objArray[1], 20 );

		entity = (CollectionRefEdEntity) objArray[0];
		assertThat( entity.getData(), equalTo( "data_ed_1" ) );
		assertThat( entity.getReffering(), contains( ingVer2 ) );
	}

	@DynamicTest
	public void testReferencedOneToManySameRevision() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetRefIngEntity.class, false, true )
				.add( AuditEntity.id().eq( 2 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );
		assertRevisionNumber( objArray[1], 2 );

		SetRefIngEntity refIngEntity = (SetRefIngEntity) objArray[0];
		assertThat( refIngEntity.getData(), equalTo( "Example Data 1" ) );

		Hibernate.initialize( refIngEntity.getReference() );
		assertThat( refIngEntity.getReference().getData(), equalTo( "Demo Data 1" ) );
	}

	@DynamicTest
	public void testReferringOneToManySameRevision() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetRefEdEntity.class, false, true )
				.add( AuditEntity.id().eq( 1 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );
		assertRevisionNumber( objArray[1], 2 );

		SetRefEdEntity refEdEntity = (SetRefEdEntity) objArray[0];
		assertThat( refEdEntity.getData(), equalTo( "Demo Data 1" ) );

		Hibernate.initialize( refEdEntity.getReffering() );
		assertThat( refEdEntity.getReffering(), contains( new SetRefIngEntity( 2, "Example Data 1" ) ) );
	}

	@DynamicTest
	public void testReferencedOneToManyDifferentRevisions() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetRefIngEntity.class, false, true )
				.add( AuditEntity.id().eq( 4 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );
		assertRevisionNumber( objArray[1], 4 );

		SetRefIngEntity refIngEntity = (SetRefIngEntity) objArray[0];
		assertThat( refIngEntity.getData(), equalTo( "Example Data 2" ) );

		Hibernate.initialize( refIngEntity.getReference() );
		assertThat( refIngEntity.getReference().getData(), equalTo( "Demo Data 2" ) );
	}

	@DynamicTest
	public void testReferringOneToManyDifferentRevisions() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetRefEdEntity.class, false, true )
				.add( AuditEntity.id().eq( 3 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );
		assertRevisionNumber( objArray[1], 5 );

		SetRefEdEntity refEdEntity = (SetRefEdEntity) objArray[0];
		assertThat( refEdEntity.getData(), equalTo( "Demo Data 2" ) );

		Hibernate.initialize( refEdEntity.getReffering() );
		assertThat( refEdEntity.getReffering(), CollectionMatchers.isEmpty() );

		// After commit in revision four, child entity has been removed.
		queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetRefEdEntity.class, false, true )
				.add( AuditEntity.id().eq( 3 ) )
				.add( AuditEntity.revisionNumber().eq( 4 ) )
				.getResultList();
		objArray = (Object[]) queryResult.get( 0 );

		refEdEntity = (SetRefEdEntity) objArray[0];
		assertThat( refEdEntity.getData(), equalTo( "Demo Data 2" ) );

		Hibernate.initialize( refEdEntity.getReffering() );
		assertThat( refEdEntity.getReffering(), CollectionMatchers.isEmpty() );
	}

	// Many to many tests.

	@DynamicTest
	public void testManyToManyCollectionSemantics() {
		final ListOwnedEntity edVer1 = new ListOwnedEntity( 1, "data_ed_1" );
		final ListOwningEntity ingVer1 = new ListOwningEntity( 2, "data_ing_1" );
		final ListOwningEntity ingVer2 = new ListOwningEntity( 2, "modified data_ing_1" );

		ListOwnedEntity entity = getAuditReader().find( ListOwnedEntity.class, 1, 21 );

		assertThat( entity, equalTo( edVer1 ) );
		assertThat( entity.getReferencing(), contains( ingVer1 ) );

		entity = getAuditReader().find( ListOwnedEntity.class, 1, 22 );
		assertThat( entity.getReferencing(), contains( ingVer2 ) );

		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( ListOwnedEntity.class, false, true )
				.add( AuditEntity.id().eq( 1 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );
		assertRevisionNumber( objArray[1], 23 );

		entity = (ListOwnedEntity) objArray[0];
		assertThat( entity.getData(), equalTo( "data_ed_1") );
		assertThat( entity.getReferencing(), contains( ingVer2 ) );
	}

	@DynamicTest
	public void testOwnedManyToManySameRevision() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetOwningEntity.class, false, true )
				.add( AuditEntity.id().eq( 5 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );
		assertRevisionNumber( objArray[1], 7 );

		SetOwningEntity setOwningEntity = (SetOwningEntity) objArray[0];
		assertThat( setOwningEntity.getData(), equalTo( "Demo Data 1" ) );

		Hibernate.initialize( setOwningEntity.getReferences() );
		assertThat( setOwningEntity.getReferences(), contains( new SetOwnedEntity( 6, "Example Data 1" ) ) );
	}

	@DynamicTest
	public void testOwningManyToManySameRevision() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetOwnedEntity.class, false, true )
				.add( AuditEntity.id().eq( 6 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );
		assertRevisionNumber( objArray[1], 7 );

		SetOwnedEntity setOwnedEntity = (SetOwnedEntity) objArray[0];
		assertThat( setOwnedEntity.getData(), equalTo( "Example Data 1" ) );

		Hibernate.initialize( setOwnedEntity.getReferencing() );
		assertThat( setOwnedEntity.getReferencing(), contains( new SetOwningEntity( 5, "Demo Data 1" ) ) );
	}

	@DynamicTest
	public void testOwnedManyToManyDifferentRevisions() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetOwningEntity.class, false, true )
				.add( AuditEntity.id().eq( 7 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );
		assertRevisionNumber( objArray[1], 9 );

		SetOwningEntity setOwningEntity = (SetOwningEntity) objArray[0];
		assertThat( setOwningEntity.getData(), equalTo( "Demo Data 2" ) );

		Hibernate.initialize( setOwningEntity.getReferences() );
		assertThat( setOwningEntity.getReferences(), contains( new SetOwnedEntity( 8, "Example Data 2" ) ) );
	}

	@DynamicTest
	public void testOwningManyToManyDifferentRevisions() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetOwnedEntity.class, false, true )
				.add( AuditEntity.id().eq( 8 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );

		assertRevisionNumber( objArray[1], 10 );

		SetOwnedEntity setOwnedEntity = (SetOwnedEntity) objArray[0];
		assertThat( setOwnedEntity.getData(), equalTo( "Example Data 2" ) );

		Hibernate.initialize( setOwnedEntity.getReferencing() );
		assertThat( setOwnedEntity.getReferencing(), CollectionMatchers.isEmpty() );

		// After commit in revision nine, related entity has been removed.
		queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetOwnedEntity.class, false, true )
				.add( AuditEntity.id().eq( 8 ) )
				.add( AuditEntity.revisionNumber().eq( 9 ) )
				.getResultList();
		objArray = (Object[]) queryResult.get( 0 );

		setOwnedEntity = (SetOwnedEntity) objArray[0];
		assertThat( setOwnedEntity.getData(), equalTo( "Example Data 2" ) );

		Hibernate.initialize( setOwnedEntity.getReferencing() );
		assertThat( setOwnedEntity.getReferencing(), CollectionMatchers.isEmpty() );
	}

	/**
	 * Asserts that the specified revision entity object's identifier matches the specified revision number.
	 *
	 * @param revisionEntity The revision number entity instance.
	 * @param revisionNumber The revision number.
	 */
	private static void assertRevisionNumber(Object revisionEntity, Object revisionNumber) {
		assertThat( revisionEntity, instanceOf( SequenceIdRevisionEntity.class ) );
		assertThat( revisionNumber, equalTo( ( (SequenceIdRevisionEntity) revisionEntity ).getId() ) );
	}
}