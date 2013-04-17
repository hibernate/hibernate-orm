package org.hibernate.envers.test.integration.proxy;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.IntTestPrivSeqEntity;
import org.hibernate.envers.test.entities.StrTestPrivSeqEntity;
import org.hibernate.envers.test.entities.UnversionedStrTestEntity;
import org.hibernate.envers.test.entities.collection.StringSetEntity;
import org.hibernate.envers.test.entities.manytomany.SetOwnedEntity;
import org.hibernate.envers.test.entities.manytomany.SetOwningEntity;
import org.hibernate.envers.test.entities.manytomany.unidirectional.M2MIndexedListTargetNotAuditedEntity;
import org.hibernate.envers.test.entities.onetomany.SetRefEdEntity;
import org.hibernate.envers.test.entities.onetomany.SetRefIngEntity;
import org.hibernate.envers.test.integration.manytomany.ternary.TernaryMapEntity;
import org.hibernate.envers.test.tools.TestTools;
import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-5845")
public class RemovedObjectQueryTest extends BaseEnversJPAFunctionalTestCase {
	private Integer stringSetId = null;
	private Integer ternaryMapId = null;
	private UnversionedStrTestEntity unversionedEntity1 = null;
	private UnversionedStrTestEntity unversionedEntity2 = null;
	private StrTestPrivSeqEntity stringEntity1 = null;
	private StrTestPrivSeqEntity stringEntity2 = null;
	private IntTestPrivSeqEntity intEntity1 = null;
	private IntTestPrivSeqEntity intEntity2 = null;

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( "org.hibernate.envers.store_data_at_delete", "true" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				SetRefEdEntity.class, SetRefIngEntity.class, SetOwnedEntity.class, SetOwningEntity.class,
				StringSetEntity.class, UnversionedStrTestEntity.class, M2MIndexedListTargetNotAuditedEntity.class,
				TernaryMapEntity.class, StrTestPrivSeqEntity.class, IntTestPrivSeqEntity.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

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
		M2MIndexedListTargetNotAuditedEntity relationNotAuditedEntity = new M2MIndexedListTargetNotAuditedEntity( 1, "Parent" );
		relationNotAuditedEntity.getReferences().add( unversionedEntity1 );
		relationNotAuditedEntity.getReferences().add( unversionedEntity2 );
		em.persist( unversionedEntity1 );
		em.persist( unversionedEntity2 );
		em.persist( relationNotAuditedEntity );
		em.getTransaction().commit();

		// Revision 14 - removing entity with unversioned relation
		em.getTransaction().begin();
		relationNotAuditedEntity = em.find( M2MIndexedListTargetNotAuditedEntity.class, relationNotAuditedEntity.getId() );
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

		// Revision 16 - removing ternary map
		em.getTransaction().begin();
		mapEntity = em.find( TernaryMapEntity.class, mapEntity.getId() );
		em.remove( mapEntity );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testTernaryMap() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( TernaryMapEntity.class, false, true )
				.add( AuditEntity.id().eq( ternaryMapId ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );

		Assert.assertEquals( 16, getRevisionNumber( objArray[1] ) );

		TernaryMapEntity mapEntity = (TernaryMapEntity) objArray[0];
		Assert.assertEquals( TestTools.makeMap( intEntity1, stringEntity1, intEntity2, stringEntity2 ), mapEntity.getMap() );
	}

	@Test
	public void testUnversionedRelation() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( M2MIndexedListTargetNotAuditedEntity.class, false, true )
				.add( AuditEntity.id().eq( 1 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );

		Assert.assertEquals( 14, getRevisionNumber( objArray[1] ) );

		M2MIndexedListTargetNotAuditedEntity relationNotAuditedEntity = (M2MIndexedListTargetNotAuditedEntity) objArray[0];
		Assert.assertTrue(
				TestTools.checkList(
						relationNotAuditedEntity.getReferences(),
						unversionedEntity1, unversionedEntity2
				)
		);
	}

	@Test
	public void testElementCollection() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( StringSetEntity.class, false, true )
				.add( AuditEntity.id().eq( stringSetId ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );

		Assert.assertEquals( 12, getRevisionNumber( objArray[1] ) );

		StringSetEntity stringSetEntity = (StringSetEntity) objArray[0];
		Assert.assertEquals( TestTools.makeSet( "string 1", "string 2" ), stringSetEntity.getStrings() );
	}

	// One to many tests.

	@Test
	public void testReferencedOneToManySameRevision() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetRefIngEntity.class, false, true )
				.add( AuditEntity.id().eq( 2 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );

		Assert.assertEquals( 2, getRevisionNumber( objArray[1] ) );

		SetRefIngEntity refIngEntity = (SetRefIngEntity) objArray[0];
		Assert.assertEquals( "Example Data 1", refIngEntity.getData() );

		Hibernate.initialize( refIngEntity.getReference() );
		Assert.assertEquals( "Demo Data 1", refIngEntity.getReference().getData() );
	}

	@Test
	public void testReferringOneToManySameRevision() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetRefEdEntity.class, false, true )
				.add( AuditEntity.id().eq( 1 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );

		Assert.assertEquals( 2, getRevisionNumber( objArray[1] ) );

		SetRefEdEntity refEdEntity = (SetRefEdEntity) objArray[0];
		Assert.assertEquals( "Demo Data 1", refEdEntity.getData() );

		Hibernate.initialize( refEdEntity.getReffering() );
		Assert.assertEquals(
				TestTools.makeSet( new SetRefIngEntity( 2, "Example Data 1" ) ),
				refEdEntity.getReffering()
		);
	}

	@Test
	public void testReferencedOneToManyDifferentRevisions() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetRefIngEntity.class, false, true )
				.add( AuditEntity.id().eq( 4 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );

		Assert.assertEquals( 4, getRevisionNumber( objArray[1] ) );

		SetRefIngEntity refIngEntity = (SetRefIngEntity) objArray[0];
		Assert.assertEquals( "Example Data 2", refIngEntity.getData() );

		Hibernate.initialize( refIngEntity.getReference() );
		Assert.assertEquals( "Demo Data 2", refIngEntity.getReference().getData() );
	}

	@Test
	public void testReferringOneToManyDifferentRevisions() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetRefEdEntity.class, false, true )
				.add( AuditEntity.id().eq( 3 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );

		Assert.assertEquals( 5, getRevisionNumber( objArray[1] ) );

		SetRefEdEntity refEdEntity = (SetRefEdEntity) objArray[0];
		Assert.assertEquals( "Demo Data 2", refEdEntity.getData() );

		Hibernate.initialize( refEdEntity.getReffering() );
		Assert.assertTrue( refEdEntity.getReffering().isEmpty() );

		// After commit in revision four, child entity has been removed.
		queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetRefEdEntity.class, false, true )
				.add( AuditEntity.id().eq( 3 ) )
				.add( AuditEntity.revisionNumber().eq( 4 ) )
				.getResultList();
		objArray = (Object[]) queryResult.get( 0 );

		refEdEntity = (SetRefEdEntity) objArray[0];
		Assert.assertEquals( "Demo Data 2", refEdEntity.getData() );

		Hibernate.initialize( refEdEntity.getReffering() );
		Assert.assertTrue( refEdEntity.getReffering().isEmpty() );
	}

	// Many to many tests.

	@Test
	public void testOwnedManyToManySameRevision() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetOwningEntity.class, false, true )
				.add( AuditEntity.id().eq( 5 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );

		Assert.assertEquals( 7, getRevisionNumber( objArray[1] ) );

		SetOwningEntity setOwningEntity = (SetOwningEntity) objArray[0];
		Assert.assertEquals( "Demo Data 1", setOwningEntity.getData() );

		Hibernate.initialize( setOwningEntity.getReferences() );
		Assert.assertEquals(
				TestTools.makeSet( new SetOwnedEntity( 6, "Example Data 1" ) ),
				setOwningEntity.getReferences()
		);
	}

	@Test
	public void testOwningManyToManySameRevision() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetOwnedEntity.class, false, true )
				.add( AuditEntity.id().eq( 6 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );

		Assert.assertEquals( 7, getRevisionNumber( objArray[1] ) );

		SetOwnedEntity setOwnedEntity = (SetOwnedEntity) objArray[0];
		Assert.assertEquals( "Example Data 1", setOwnedEntity.getData() );

		Hibernate.initialize( setOwnedEntity.getReferencing() );
		Assert.assertEquals(
				TestTools.makeSet( new SetOwningEntity( 5, "Demo Data 1" ) ),
				setOwnedEntity.getReferencing()
		);
	}

	@Test
	public void testOwnedManyToManyDifferentRevisions() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetOwningEntity.class, false, true )
				.add( AuditEntity.id().eq( 7 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );

		Assert.assertEquals( 9, getRevisionNumber( objArray[1] ) );

		SetOwningEntity setOwningEntity = (SetOwningEntity) objArray[0];
		Assert.assertEquals( "Demo Data 2", setOwningEntity.getData() );

		Hibernate.initialize( setOwningEntity.getReferences() );
		Assert.assertEquals(
				TestTools.makeSet( new SetOwnedEntity( 8, "Example Data 2" ) ),
				setOwningEntity.getReferences()
		);
	}

	@Test
	public void testOwningManyToManyDifferentRevisions() {
		List queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetOwnedEntity.class, false, true )
				.add( AuditEntity.id().eq( 8 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();
		Object[] objArray = (Object[]) queryResult.get( 0 );

		Assert.assertEquals( 10, getRevisionNumber( objArray[1] ) );

		SetOwnedEntity setOwnedEntity = (SetOwnedEntity) objArray[0];
		Assert.assertEquals( "Example Data 2", setOwnedEntity.getData() );

		Hibernate.initialize( setOwnedEntity.getReferencing() );
		Assert.assertTrue( setOwnedEntity.getReferencing().isEmpty() );

		// After commit in revision nine, related entity has been removed.
		queryResult = getAuditReader().createQuery().forRevisionsOfEntity( SetOwnedEntity.class, false, true )
				.add( AuditEntity.id().eq( 8 ) )
				.add( AuditEntity.revisionNumber().eq( 9 ) )
				.getResultList();
		objArray = (Object[]) queryResult.get( 0 );

		setOwnedEntity = (SetOwnedEntity) objArray[0];
		Assert.assertEquals( "Example Data 2", setOwnedEntity.getData() );

		Hibernate.initialize( setOwnedEntity.getReferencing() );
		Assert.assertTrue( setOwnedEntity.getReferencing().isEmpty() );
	}

	private Number getRevisionNumber(Object revisionEntity) {
		return ( (SequenceIdRevisionEntity) revisionEntity ).getId();
	}
}