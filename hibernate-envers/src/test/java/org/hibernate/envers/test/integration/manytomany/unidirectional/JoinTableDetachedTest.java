package org.hibernate.envers.test.integration.manytomany.unidirectional;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.HashSet;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.manytomany.unidirectional.JoinTableEntity;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-8087")
public class JoinTableDetachedTest extends BaseEnversJPAFunctionalTestCase {
	private Long collectionEntityId = null;
	private Integer element1Id = null;
	private Integer element2Id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {JoinTableEntity.class, StrTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1 - addition
		em.getTransaction().begin();
		JoinTableEntity collectionEntity = new JoinTableEntity( "some data" );
		StrTestEntity element1 = new StrTestEntity( "str1" );
		StrTestEntity element2 = new StrTestEntity( "str2" );
		collectionEntity.getReferences().add( element1 );
		collectionEntity.getReferences().add( element2 );
		em.persist( element1 );
		em.persist( element2 );
		em.persist( collectionEntity );
		em.getTransaction().commit();

		collectionEntityId = collectionEntity.getId();
		element1Id = element1.getId();
		element2Id = element2.getId();

		em.close();
		em = getEntityManager();

		// Revision 2 - simple modification
		em.getTransaction().begin();
		collectionEntity = em.find( JoinTableEntity.class, collectionEntity.getId() );
		collectionEntity.setData( "some other data" );
		collectionEntity = em.merge( collectionEntity );
		em.getTransaction().commit();

		em.close();
		em = getEntityManager();

		// Revision 3 - remove detached object from collection
		em.getTransaction().begin();
		collectionEntity = em.find( JoinTableEntity.class, collectionEntity.getId() );
		collectionEntity.getReferences().remove( element1 );
		collectionEntity = em.merge( collectionEntity );
		em.getTransaction().commit();

		em.close();
		em = getEntityManager();

		// Revision 4 - replace the collection
		em.getTransaction().begin();
		collectionEntity = em.find( JoinTableEntity.class, collectionEntity.getId() );
		collectionEntity.setReferences( new HashSet<StrTestEntity>() );
		collectionEntity = em.merge( collectionEntity );
		em.getTransaction().commit();

		em.close();
		em = getEntityManager();

		// Revision 5 - add to collection
		em.getTransaction().begin();
		collectionEntity = em.find( JoinTableEntity.class, collectionEntity.getId() );
		collectionEntity.getReferences().add( element1 );
		collectionEntity = em.merge( collectionEntity );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testRevisionsCounts() {
		Assert.assertEquals(
				Arrays.asList( 1, 2, 3, 4, 5 ), getAuditReader().getRevisions(
				JoinTableEntity.class,
				collectionEntityId
		)
		);
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( StrTestEntity.class, element1Id ) );
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( StrTestEntity.class, element2Id ) );
	}

	@Test
	public void testHistoryOfCollectionEntity() {
		// Revision 1
		JoinTableEntity collectionEntity = new JoinTableEntity( collectionEntityId, "some data" );
		StrTestEntity element1 = new StrTestEntity( "str1", element1Id );
		StrTestEntity element2 = new StrTestEntity( "str2", element2Id );
		collectionEntity.getReferences().add( element1 );
		collectionEntity.getReferences().add( element2 );
		JoinTableEntity ver1 = getAuditReader().find( JoinTableEntity.class, collectionEntityId, 1 );
		Assert.assertEquals( collectionEntity, ver1 );
		Assert.assertEquals( collectionEntity.getReferences(), ver1.getReferences() );

		// Revision 2
		collectionEntity.setData( "some other data" );
		JoinTableEntity ver2 = getAuditReader().find( JoinTableEntity.class, collectionEntityId, 2 );
		Assert.assertEquals( collectionEntity, ver2 );
		Assert.assertEquals( collectionEntity.getReferences(), ver2.getReferences() );

		// Revision 3
		collectionEntity.getReferences().remove( element1 );
		JoinTableEntity ver3 = getAuditReader().find( JoinTableEntity.class, collectionEntityId, 3 );
		Assert.assertEquals( collectionEntity, ver3 );
		Assert.assertEquals( collectionEntity.getReferences(), ver3.getReferences() );

		// Revision 4
		collectionEntity.setReferences( new HashSet<StrTestEntity>() );
		JoinTableEntity ver4 = getAuditReader().find( JoinTableEntity.class, collectionEntityId, 4 );
		Assert.assertEquals( collectionEntity, ver4 );
		Assert.assertEquals( collectionEntity.getReferences(), ver4.getReferences() );

		// Revision 5
		collectionEntity.getReferences().add( element1 );
		JoinTableEntity ver5 = getAuditReader().find( JoinTableEntity.class, collectionEntityId, 5 );
		Assert.assertEquals( collectionEntity, ver5 );
		Assert.assertEquals( collectionEntity.getReferences(), ver5.getReferences() );
	}
}
