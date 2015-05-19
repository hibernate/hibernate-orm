/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.secondary.ids;

import java.util.Arrays;
import java.util.Iterator;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.ids.MulId;
import org.hibernate.mapping.Join;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MulIdSecondary extends BaseEnversJPAFunctionalTestCase {
	private MulId id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {SecondaryMulIdTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		id = new MulId( 1, 2 );

		SecondaryMulIdTestEntity ste = new SecondaryMulIdTestEntity( id, "a", "1" );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		em.persist( ste );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		ste = em.find( SecondaryMulIdTestEntity.class, id );
		ste.setS1( "b" );
		ste.setS2( "2" );

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( SecondaryMulIdTestEntity.class, id ) );
	}

	@Test
	public void testHistoryOfId() {
		SecondaryMulIdTestEntity ver1 = new SecondaryMulIdTestEntity( id, "a", "1" );
		SecondaryMulIdTestEntity ver2 = new SecondaryMulIdTestEntity( id, "b", "2" );

		assert getAuditReader().find( SecondaryMulIdTestEntity.class, id, 1 ).equals( ver1 );
		assert getAuditReader().find( SecondaryMulIdTestEntity.class, id, 2 ).equals( ver2 );
	}

	@SuppressWarnings({"unchecked"})
	@Test
	public void testTableNames() {
		assert "sec_mulid_versions".equals(
				((Iterator<Join>)
						metadata().getEntityBinding(
								"org.hibernate.envers.test.integration.secondary.ids.SecondaryMulIdTestEntity_AUD"
						)
								.getJoinIterator())
						.next().getTable().getName()
		);
	}
}