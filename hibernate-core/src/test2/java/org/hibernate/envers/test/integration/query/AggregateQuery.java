/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.query;

import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.IntTestEntity;
import org.hibernate.envers.test.entities.ids.UnusualIdNamingEntity;
import org.hibernate.envers.test.tools.TestTools;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
public class AggregateQuery extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {IntTestEntity.class, UnusualIdNamingEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		IntTestEntity ite1 = new IntTestEntity( 2 );
		IntTestEntity ite2 = new IntTestEntity( 10 );
		em.persist( ite1 );
		em.persist( ite2 );
		Integer id1 = ite1.getId();
		Integer id2 = ite2.getId();
		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();
		IntTestEntity ite3 = new IntTestEntity( 8 );
		UnusualIdNamingEntity uine1 = new UnusualIdNamingEntity( "id1", "data1" );
		em.persist( uine1 );
		em.persist( ite3 );
		ite1 = em.find( IntTestEntity.class, id1 );
		ite1.setNumber( 0 );
		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();
		ite2 = em.find( IntTestEntity.class, id2 );
		ite2.setNumber( 52 );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testEntitiesAvgMaxQuery() {
		Object[] ver1 = (Object[]) getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 1 )
				.addProjection( AuditEntity.property( "number" ).max() )
				.addProjection( AuditEntity.property( "number" ).function( "avg" ) )
				.getSingleResult();

		Object[] ver2 = (Object[]) getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 2 )
				.addProjection( AuditEntity.property( "number" ).max() )
				.addProjection( AuditEntity.property( "number" ).function( "avg" ) )
				.getSingleResult();

		Object[] ver3 = (Object[]) getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 3 )
				.addProjection( AuditEntity.property( "number" ).max() )
				.addProjection( AuditEntity.property( "number" ).function( "avg" ) )
				.getSingleResult();

		assert (Integer) ver1[0] == 10;
		assert (Double) ver1[1] == 6.0;

		assert (Integer) ver2[0] == 10;
		assert (Double) ver2[1] == 6.0;

		assert (Integer) ver3[0] == 52;
		assert (Double) ver3[1] == 20.0;
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8036")
	public void testEntityIdProjection() {
		Integer maxId = (Integer) getAuditReader().createQuery().forRevisionsOfEntity( IntTestEntity.class, true, true )
				.addProjection( AuditEntity.id().max() )
				.add( AuditEntity.revisionNumber().gt( 2 ) )
				.getSingleResult();
		Assert.assertEquals( Integer.valueOf( 2 ), maxId );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8036")
	public void testEntityIdRestriction() {
		List<IntTestEntity> list = getAuditReader().createQuery().forRevisionsOfEntity(
				IntTestEntity.class,
				true,
				true
		)
				.add( AuditEntity.id().between( 2, 3 ) )
				.getResultList();
		Assert.assertTrue(
				TestTools.checkCollection(
						list,
						new IntTestEntity( 10, 2 ), new IntTestEntity( 8, 3 ), new IntTestEntity( 52, 2 )
				)
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8036")
	public void testEntityIdOrdering() {
		List<IntTestEntity> list = getAuditReader().createQuery().forRevisionsOfEntity(
				IntTestEntity.class,
				true,
				true
		)
				.add( AuditEntity.revisionNumber().lt( 2 ) )
				.addOrder( AuditEntity.id().desc() )
				.getResultList();
		Assert.assertEquals( Arrays.asList( new IntTestEntity( 10, 2 ), new IntTestEntity( 2, 1 ) ), list );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8036")
	public void testUnusualIdFieldName() {
		UnusualIdNamingEntity entity = (UnusualIdNamingEntity) getAuditReader().createQuery()
				.forRevisionsOfEntity( UnusualIdNamingEntity.class, true, true )
				.add( AuditEntity.id().like( "id1" ) )
				.getSingleResult();
		Assert.assertEquals( new UnusualIdNamingEntity( "id1", "data1" ), entity );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8036")
	public void testEntityIdModifiedFlagNotSupported() {
		try {
			getAuditReader().createQuery().forRevisionsOfEntity( IntTestEntity.class, true, true )
					.add( AuditEntity.id().hasChanged() )
					.getResultList();
		}
		catch (UnsupportedOperationException e1) {
			try {
				getAuditReader().createQuery().forRevisionsOfEntity( IntTestEntity.class, true, true )
						.add( AuditEntity.id().hasNotChanged() )
						.getResultList();
			}
			catch (UnsupportedOperationException e2) {
				return;
			}
			Assert.fail();
		}
		Assert.fail();
	}
}