/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.query;

import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrIntTestEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class DeletedEntities extends BaseEnversJPAFunctionalTestCase {
	private Integer id2;

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
		StrIntTestEntity site2 = new StrIntTestEntity( "b", 11 );

		em.persist( site1 );
		em.persist( site2 );

		id2 = site2.getId();

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		site2 = em.find( StrIntTestEntity.class, id2 );
		em.remove( site2 );

		em.getTransaction().commit();
	}

	@Test
	public void testProjectionsInEntitiesAtRevision() {
		assert getAuditReader().createQuery().forEntitiesAtRevision( StrIntTestEntity.class, 1 )
				.getResultList().size() == 2;
		assert getAuditReader().createQuery().forEntitiesAtRevision( StrIntTestEntity.class, 2 )
				.getResultList().size() == 1;

		assert (Long) getAuditReader().createQuery().forEntitiesAtRevision( StrIntTestEntity.class, 1 )
				.addProjection( AuditEntity.id().count() ).getResultList().get( 0 ) == 2;
		assert (Long) getAuditReader().createQuery().forEntitiesAtRevision( StrIntTestEntity.class, 2 )
				.addProjection( AuditEntity.id().count() ).getResultList().get( 0 ) == 1;
	}

	@Test
	public void testRevisionsOfEntityWithoutDelete() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, false )
				.add( AuditEntity.id().eq( id2 ) )
				.getResultList();

		assert result.size() == 1;

		assert ((Object[]) result.get( 0 ))[0].equals( new StrIntTestEntity( "b", 11, id2 ) );
		assert ((SequenceIdRevisionEntity) ((Object[]) result.get( 0 ))[1]).getId() == 1;
		assert ((Object[]) result.get( 0 ))[2].equals( RevisionType.ADD );
	}
}
