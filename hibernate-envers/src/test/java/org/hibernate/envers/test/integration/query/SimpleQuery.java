/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.query;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.MatchMode;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrIntTestEntity;
import org.hibernate.envers.test.entities.ids.EmbId;
import org.hibernate.envers.test.entities.ids.EmbIdTestEntity;
import org.hibernate.envers.test.entities.ids.MulId;
import org.hibernate.envers.test.entities.ids.MulIdTestEntity;
import org.hibernate.envers.test.tools.TestTools;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@SuppressWarnings({"unchecked"})
public class SimpleQuery extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;
	private Integer id2;
	private Integer id3;
	private MulId mulId1;
	private EmbId embId1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrIntTestEntity.class, MulIdTestEntity.class, EmbIdTestEntity.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
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

		site1 = em.find( StrIntTestEntity.class, id1 );
		site2 = em.find( StrIntTestEntity.class, id2 );

		site1.setStr1( "aBc" );
		site2.setNumber( 20 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		site3 = em.find( StrIntTestEntity.class, id3 );

		site3.setStr1( "a" );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		site1 = em.find( StrIntTestEntity.class, id1 );

		em.remove( site1 );

		em.getTransaction().commit();
	}

	@Test
	public void testEntitiesIdQuery() {
		StrIntTestEntity ver2 = (StrIntTestEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 2 )
				.add( AuditEntity.id().eq( id2 ) )
				.getSingleResult();

		assert ver2.equals( new StrIntTestEntity( "a", 20, id2 ) );
	}

	@Test
	public void testEntitiesPropertyEqualsQuery() {
		List ver1 = getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 1 )
				.add( AuditEntity.property( "str1" ).eq( "a" ) )
				.getResultList();

		List ver2 = getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 2 )
				.add( AuditEntity.property( "str1" ).eq( "a" ) )
				.getResultList();

		List ver3 = getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 3 )
				.add( AuditEntity.property( "str1" ).eq( "a" ) )
				.getResultList();

		assert new HashSet( ver1 ).equals(
				TestTools.makeSet(
						new StrIntTestEntity( "a", 10, id1 ),
						new StrIntTestEntity( "a", 10, id2 )
				)
		);
		assert new HashSet( ver2 ).equals( TestTools.makeSet( new StrIntTestEntity( "a", 20, id2 ) ) );
		assert new HashSet( ver3 ).equals(
				TestTools.makeSet(
						new StrIntTestEntity( "a", 20, id2 ),
						new StrIntTestEntity( "a", 5, id3 )
				)
		);
	}

	@Test
	public void testEntitiesPropertyLeQuery() {
		List ver1 = getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 1 )
				.add( AuditEntity.property( "number" ).le( 10 ) )
				.getResultList();

		List ver2 = getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 2 )
				.add( AuditEntity.property( "number" ).le( 10 ) )
				.getResultList();

		List ver3 = getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 3 )
				.add( AuditEntity.property( "number" ).le( 10 ) )
				.getResultList();

		assert new HashSet( ver1 ).equals(
				TestTools.makeSet(
						new StrIntTestEntity( "a", 10, id1 ),
						new StrIntTestEntity( "a", 10, id2 ), new StrIntTestEntity( "b", 5, id3 )
				)
		);
		assert new HashSet( ver2 ).equals(
				TestTools.makeSet(
						new StrIntTestEntity( "aBc", 10, id1 ),
						new StrIntTestEntity( "b", 5, id3 )
				)
		);
		assert new HashSet( ver3 ).equals(
				TestTools.makeSet(
						new StrIntTestEntity( "aBc", 10, id1 ),
						new StrIntTestEntity( "a", 5, id3 )
				)
		);
	}

	@Test
	public void testRevisionsPropertyEqQuery() {
		List revs_id1 = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber() )
				.add( AuditEntity.property( "str1" ).le( "a" ) )
				.add( AuditEntity.id().eq( id1 ) )
				.getResultList();

		List revs_id2 = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber() )
				.add( AuditEntity.property( "str1" ).le( "a" ) )
				.add( AuditEntity.id().eq( id2 ) )
				.addOrder( AuditEntity.revisionNumber().asc() )
				.getResultList();

		List revs_id3 = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber() )
				.add( AuditEntity.property( "str1" ).le( "a" ) )
				.add( AuditEntity.id().eq( id3 ) )
				.getResultList();

		assert Arrays.asList( 1 ).equals( revs_id1 );
		assert Arrays.asList( 1, 2 ).equals( revs_id2 );
		assert Arrays.asList( 3 ).equals( revs_id3 );
	}

	@Test
	public void testSelectEntitiesQuery() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, true, false )
				.add( AuditEntity.id().eq( id1 ) )
				.getResultList();

		assert result.size() == 2;

		assert result.get( 0 ).equals( new StrIntTestEntity( "a", 10, id1 ) );
		assert result.get( 1 ).equals( new StrIntTestEntity( "aBc", 10, id1 ) );
	}

	@Test
	public void testSelectEntitiesAndRevisionsQuery() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.add( AuditEntity.id().eq( id1 ) )
				.getResultList();

		assert result.size() == 3;

		assert ((Object[]) result.get( 0 ))[0].equals( new StrIntTestEntity( "a", 10, id1 ) );
		assert ((Object[]) result.get( 1 ))[0].equals( new StrIntTestEntity( "aBc", 10, id1 ) );
		assert ((Object[]) result.get( 2 ))[0].equals( new StrIntTestEntity( null, null, id1 ) );

		assert ((SequenceIdRevisionEntity) ((Object[]) result.get( 0 ))[1]).getId() == 1;
		assert ((SequenceIdRevisionEntity) ((Object[]) result.get( 1 ))[1]).getId() == 2;
		assert ((SequenceIdRevisionEntity) ((Object[]) result.get( 2 ))[1]).getId() == 4;

		assert ((Object[]) result.get( 0 ))[2].equals( RevisionType.ADD );
		assert ((Object[]) result.get( 1 ))[2].equals( RevisionType.MOD );
		assert ((Object[]) result.get( 2 ))[2].equals( RevisionType.DEL );
	}

	@Test
	public void testSelectRevisionTypeQuery() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionType() )
				.add( AuditEntity.id().eq( id1 ) )
				.addOrder( AuditEntity.revisionNumber().asc() )
				.getResultList();

		assert result.size() == 3;

		assert result.get( 0 ).equals( RevisionType.ADD );
		assert result.get( 1 ).equals( RevisionType.MOD );
		assert result.get( 2 ).equals( RevisionType.DEL );
	}

	@Test
	public void testEmptyRevisionOfEntityQuery() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.getResultList();

		assert result.size() == 7;
	}

	@Test
	public void testEmptyConjunctionRevisionOfEntityQuery() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.add( AuditEntity.conjunction() )
				.getResultList();

		assert result.size() == 7;
	}

	@Test
	public void testEmptyDisjunctionRevisionOfEntityQuery() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.add( AuditEntity.disjunction() )
				.getResultList();

		assert result.size() == 0;
	}

	@Test
	public void testEntitiesAddedAtRevision() {
		StrIntTestEntity site1 = new StrIntTestEntity( "a", 10, id1 );
		StrIntTestEntity site2 = new StrIntTestEntity( "a", 10, id2 );
		StrIntTestEntity site3 = new StrIntTestEntity( "b", 5, id3 );

		List result = getAuditReader().createQuery().forEntitiesModifiedAtRevision(
				StrIntTestEntity.class,
				StrIntTestEntity.class.getName(),
				1
		).getResultList();
		RevisionType revisionType = (RevisionType) getAuditReader().createQuery().forEntitiesModifiedAtRevision(
				StrIntTestEntity.class,
				1
		)
				.addProjection( AuditEntity.revisionType() ).add( AuditEntity.id().eq( id1 ) )
				.getSingleResult();

		Assert.assertTrue( TestTools.checkCollection( result, site1, site2, site3 ) );
		Assert.assertEquals( revisionType, RevisionType.ADD );
	}

	@Test
	public void testEntitiesChangedAtRevision() {
		StrIntTestEntity site1 = new StrIntTestEntity( "aBc", 10, id1 );
		StrIntTestEntity site2 = new StrIntTestEntity( "a", 20, id2 );

		List result = getAuditReader().createQuery()
				.forEntitiesModifiedAtRevision( StrIntTestEntity.class, 2 )
				.getResultList();
		RevisionType revisionType = (RevisionType) getAuditReader().createQuery().forEntitiesModifiedAtRevision(
				StrIntTestEntity.class,
				2
		)
				.addProjection( AuditEntity.revisionType() ).add( AuditEntity.id().eq( id1 ) )
				.getSingleResult();

		Assert.assertTrue( TestTools.checkCollection( result, site1, site2 ) );
		Assert.assertEquals( revisionType, RevisionType.MOD );
	}

	@Test
	public void testEntitiesRemovedAtRevision() {
		StrIntTestEntity site1 = new StrIntTestEntity( null, null, id1 );

		List result = getAuditReader().createQuery()
				.forEntitiesModifiedAtRevision( StrIntTestEntity.class, 4 )
				.getResultList();
		RevisionType revisionType = (RevisionType) getAuditReader().createQuery().forEntitiesModifiedAtRevision(
				StrIntTestEntity.class,
				4
		)
				.addProjection( AuditEntity.revisionType() ).add( AuditEntity.id().eq( id1 ) )
				.getSingleResult();

		Assert.assertTrue( TestTools.checkCollection( result, site1 ) );
		Assert.assertEquals( revisionType, RevisionType.DEL );
	}

	@Test
	public void testEntityNotModifiedAtRevision() {
		List result = getAuditReader().createQuery().forEntitiesModifiedAtRevision( StrIntTestEntity.class, 3 )
				.add( AuditEntity.id().eq( id1 ) ).getResultList();
		Assert.assertTrue( result.isEmpty() );
	}

	@Test
	public void testNoEntitiesModifiedAtRevision() {
		List result = getAuditReader().createQuery()
				.forEntitiesModifiedAtRevision( StrIntTestEntity.class, 5 )
				.getResultList();
		Assert.assertTrue( result.isEmpty() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7800")
	public void testBetweenInsideDisjunction() {
		List result = getAuditReader().createQuery()
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
			Assert.assertTrue( (number >= 0 && number <= 5) || (number >= 20 && number <= 100) );
		}
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-8495")
	public void testIlike() {
		StrIntTestEntity site1 = new StrIntTestEntity( "aBc", 10, id1 );
		
		StrIntTestEntity result = (StrIntTestEntity) getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, true, true )
				.add( AuditEntity.property( "str1" ).ilike( "abc" ) )
				.getSingleResult();
		
		Assert.assertEquals( site1, result );
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-8495")
	public void testIlikeWithMatchMode() {
		StrIntTestEntity site1 = new StrIntTestEntity( "aBc", 10, id1 );
		
		StrIntTestEntity result = (StrIntTestEntity) getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, true, true )
				.add( AuditEntity.property( "str1" ).ilike( "BC", MatchMode.ANYWHERE ) )
				.getSingleResult();
		
		Assert.assertEquals( site1, result );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8567")
	public void testIdPropertyRestriction() {
		StrIntTestEntity ver2 = (StrIntTestEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 2 )
				.add( AuditEntity.property( "id" ).eq( id2 ) )
				.getSingleResult();

		Assert.assertEquals( new StrIntTestEntity( "a", 20, id2 ), ver2 );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8567")
	public void testMultipleIdPropertyRestriction() {
		MulIdTestEntity ver2 = (MulIdTestEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( MulIdTestEntity.class, 2 )
				.add( AuditEntity.property( "id1" ).eq( mulId1.getId1() ) )
				.add( AuditEntity.property( "id2" ).eq( mulId1.getId2() ) )
				.getSingleResult();

		Assert.assertEquals( new MulIdTestEntity( mulId1.getId1(), mulId1.getId2(), "data" ), ver2 );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8567")
	public void testEmbeddedIdPropertyRestriction() {
		EmbIdTestEntity ver2 = (EmbIdTestEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( EmbIdTestEntity.class, 2 )
				.add( AuditEntity.property( "id.x" ).eq( embId1.getX() ) )
				.add( AuditEntity.property( "id.y" ).eq( embId1.getY() ) )
				.getSingleResult();

		Assert.assertEquals( new EmbIdTestEntity( embId1, "something" ), ver2 );
	}
}
