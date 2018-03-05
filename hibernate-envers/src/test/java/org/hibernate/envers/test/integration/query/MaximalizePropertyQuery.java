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
import java.util.Set;
import javax.persistence.EntityManager;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.AuditDisjunction;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrIntTestEntity;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@SuppressWarnings({"unchecked"})
public class MaximalizePropertyQuery extends BaseEnversJPAFunctionalTestCase {
	Integer id1;
	Integer id2;
	Integer id3;
	Integer id4;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrIntTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
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

		site1 = em.find( StrIntTestEntity.class, id1 );
		site2 = em.find( StrIntTestEntity.class, id2 );

		site1.setStr1( "d" );
		site2.setNumber( 20 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		site1 = em.find( StrIntTestEntity.class, id1 );
		site2 = em.find( StrIntTestEntity.class, id2 );

		site1.setNumber( 30 );
		site2.setStr1( "z" );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		site1 = em.find( StrIntTestEntity.class, id1 );
		site2 = em.find( StrIntTestEntity.class, id2 );

		site1.setNumber( 5 );
		site2.setStr1( "a" );

		em.getTransaction().commit();

		// Revision 5
		em.getTransaction().begin();
		site4 = em.find( StrIntTestEntity.class, id4 );
		em.remove( site4 );
		em.getTransaction().commit();
	}

	@Test
	public void testMaximizeWithIdEq() {
		List revs_id1 = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber() )
				.add(
						AuditEntity.property( "number" ).maximize()
								.add( AuditEntity.id().eq( id2 ) )
				)
				.addOrder( AuditEntity.revisionNumber().asc() )
				.getResultList();

		assert Arrays.asList( 2, 3, 4 ).equals( revs_id1 );
	}

	@Test
	public void testMinimizeWithPropertyEq() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber() )
				.add(
						AuditEntity.property( "number" ).minimize()
								.add( AuditEntity.property( "str1" ).eq( "a" ) )
				)
				.getResultList();

		assert Arrays.asList( 1 ).equals( result );
	}

	@Test
	public void testMaximizeRevision() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber() )
				.add(
						AuditEntity.revisionNumber().maximize()
								.add( AuditEntity.property( "number" ).eq( 10 ) )
				)
				.getResultList();

		assert Arrays.asList( 2 ).equals( result );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7800")
	public void testMaximizeInDisjunction() {
		List<Integer> idsToQuery = Arrays.asList( id1, id3 );

		AuditDisjunction disjunction = AuditEntity.disjunction();

		for ( Integer id : idsToQuery ) {
			disjunction.add( AuditEntity.revisionNumber().maximize().add( AuditEntity.id().eq( id ) ) );
		}
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, true, true )
				.add( disjunction )
				.getResultList();

		Set<Integer> idsSeen = new HashSet<Integer>();
		for ( Object o : result ) {
			StrIntTestEntity entity = (StrIntTestEntity) o;
			Integer id = entity.getId();
			Assert.assertTrue( "Entity with ID " + id + " returned but not queried for.", idsToQuery.contains( id ) );
			if ( !idsSeen.add( id ) ) {
				Assert.fail( "Multiple revisions returned with ID " + id + "; expected only one." );
			}
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7827")
	public void testAllLatestRevisionsOfEntityType() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.add( AuditEntity.revisionNumber().maximize().computeAggregationInInstanceContext() )
				.addOrder( AuditEntity.property( "id" ).asc() )
				.getResultList();

		Assert.assertEquals( 4, result.size() );

		Object[] result1 = (Object[]) result.get( 0 );
		Object[] result2 = (Object[]) result.get( 1 );
		Object[] result3 = (Object[]) result.get( 2 );
		Object[] result4 = (Object[]) result.get( 3 );

		checkRevisionData( result1, 4, RevisionType.MOD, new StrIntTestEntity( "d", 5, id1 ) );
		checkRevisionData( result2, 4, RevisionType.MOD, new StrIntTestEntity( "a", 20, id2 ) );
		checkRevisionData( result3, 1, RevisionType.ADD, new StrIntTestEntity( "c", 42, id3 ) );
		checkRevisionData( result4, 5, RevisionType.DEL, new StrIntTestEntity( null, null, id4 ) );
	}

	private void checkRevisionData(Object[] result, int revision, RevisionType type, StrIntTestEntity entity) {
		Assert.assertEquals( entity, result[0] );
		Assert.assertEquals( revision, ((SequenceIdRevisionEntity) result[1]).getId() );
		Assert.assertEquals( type, result[2] );
	}
}