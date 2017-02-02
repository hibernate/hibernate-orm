/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.flush;

import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.FlushMode;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ManualFlush extends AbstractFlushTest {
	private Integer id;

	public FlushMode getFlushMode() {
		return FlushMode.MANUAL;
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		StrTestEntity fe = new StrTestEntity( "x" );
		em.persist( fe );
		em.flush();

		em.getTransaction().commit();

		// No revision - we change the data, but do not flush the session
		em.getTransaction().begin();

		fe = em.find( StrTestEntity.class, fe.getId() );
		fe.setStr( "y" );

		em.getTransaction().commit();

		// Revision 2 - only the first change should be saved
		em.getTransaction().begin();

		fe = em.find( StrTestEntity.class, fe.getId() );
		fe.setStr( "z" );
		em.flush();

		fe = em.find( StrTestEntity.class, fe.getId() );
		fe.setStr( "z2" );

		em.getTransaction().commit();

		//

		id = fe.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( StrTestEntity.class, id ) );
	}

	@Test
	public void testHistoryOfId() {
		StrTestEntity ver1 = new StrTestEntity( "x", id );
		StrTestEntity ver2 = new StrTestEntity( "z", id );

		assertEquals( ver1, getAuditReader().find( StrTestEntity.class, id, 1 ) );
		assertEquals( ver2, getAuditReader().find( StrTestEntity.class, id, 2 ) );
	}

	@Test
	public void testCurrent() {
		assertEquals( new StrTestEntity( "z", id ), getEntityManager().find( StrTestEntity.class, id ) );
	}

	@Test
	public void testRevisionTypes() {
		@SuppressWarnings({"unchecked"}) List<Object[]> results =
				getAuditReader().createQuery()
						.forRevisionsOfEntity( StrTestEntity.class, false, true )
						.add( AuditEntity.id().eq( id ) )
						.getResultList();

		assertEquals( results.get( 0 )[2], RevisionType.ADD );
		assertEquals( results.get( 1 )[2], RevisionType.MOD );
	}
}
