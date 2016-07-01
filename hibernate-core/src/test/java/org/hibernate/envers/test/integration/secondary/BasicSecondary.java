/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.secondary;

import java.util.Arrays;
import java.util.Iterator;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.mapping.Join;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicSecondary extends BaseEnversJPAFunctionalTestCase {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {SecondaryTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		SecondaryTestEntity ste = new SecondaryTestEntity( "a", "1" );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		em.persist( ste );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		ste = em.find( SecondaryTestEntity.class, ste.getId() );
		ste.setS1( "b" );
		ste.setS2( "2" );

		em.getTransaction().commit();

		//

		id = ste.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( SecondaryTestEntity.class, id ) );
	}

	@Test
	public void testHistoryOfId() {
		SecondaryTestEntity ver1 = new SecondaryTestEntity( id, "a", "1" );
		SecondaryTestEntity ver2 = new SecondaryTestEntity( id, "b", "2" );

		assert getAuditReader().find( SecondaryTestEntity.class, id, 1 ).equals( ver1 );
		assert getAuditReader().find( SecondaryTestEntity.class, id, 2 ).equals( ver2 );
	}

	@SuppressWarnings({"unchecked"})
	@Test
	public void testTableNames() {
		assert "secondary_AUD".equals(
				((Iterator<Join>)
						metadata().getEntityBinding(
								"org.hibernate.envers.test.integration.secondary.SecondaryTestEntity_AUD"
						)
								.getJoinIterator())
						.next().getTable().getName()
		);
	}
}