/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.query;

import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrIntTestEntity;
import org.hibernate.envers.test.entities.reventity.CustomRevEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
public class CustomRevEntityQuery extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;
	private Integer id2;
	private Long timestamp;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrIntTestEntity.class, CustomRevEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() throws InterruptedException {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		StrIntTestEntity site1 = new StrIntTestEntity( "a", 10 );
		StrIntTestEntity site2 = new StrIntTestEntity( "b", 15 );

		em.persist( site1 );
		em.persist( site2 );

		id1 = site1.getId();
		id2 = site2.getId();

		em.getTransaction().commit();

		Thread.sleep( 100 );

		timestamp = System.currentTimeMillis();

		Thread.sleep( 100 );

		// Revision 2
		em.getTransaction().begin();

		site1 = em.find( StrIntTestEntity.class, id1 );

		site1.setStr1( "c" );

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsOfId1Query() {
		List<Object[]> result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.add( AuditEntity.id().eq( id1 ) )
				.getResultList();

		assert result.get( 0 )[0].equals( new StrIntTestEntity( "a", 10, id1 ) );
		assert result.get( 0 )[1] instanceof CustomRevEntity;
		assert ((CustomRevEntity) result.get( 0 )[1]).getCustomId() == 1;

		assert result.get( 1 )[0].equals( new StrIntTestEntity( "c", 10, id1 ) );
		assert result.get( 1 )[1] instanceof CustomRevEntity;
		assert ((CustomRevEntity) result.get( 1 )[1]).getCustomId() == 2;
	}

	@Test
	public void testRevisionsOfId2Query() {
		List<Object[]> result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.add( AuditEntity.id().eq( id2 ) )
				.getResultList();

		assert result.get( 0 )[0].equals( new StrIntTestEntity( "b", 15, id2 ) );
		assert result.get( 0 )[1] instanceof CustomRevEntity;
		assert ((CustomRevEntity) result.get( 0 )[1]).getCustomId() == 1;
	}

	@Test
	public void testRevisionPropertyRestriction() {
		List<Object[]> result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.add( AuditEntity.id().eq( id1 ) )
				.add( AuditEntity.revisionProperty( "customTimestamp" ).ge( timestamp ) )
				.getResultList();

		assert result.get( 0 )[0].equals( new StrIntTestEntity( "c", 10, id1 ) );
		assert result.get( 0 )[1] instanceof CustomRevEntity;
		assert ((CustomRevEntity) result.get( 0 )[1]).getCustomId() == 2;
		assert ((CustomRevEntity) result.get( 0 )[1]).getCustomTimestamp() >= timestamp;
	}
}