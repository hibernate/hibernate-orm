/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.ids;

import java.util.Arrays;
import java.util.Date;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.ids.DateIdTestEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class DateId extends BaseEnversJPAFunctionalTestCase {
	private Date id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {DateIdTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		DateIdTestEntity dite = new DateIdTestEntity( new Date(), "x" );
		em.persist( dite );

		id1 = dite.getId();

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		dite = em.find( DateIdTestEntity.class, id1 );
		dite.setStr1( "y" );

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( DateIdTestEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		DateIdTestEntity ver1 = new DateIdTestEntity( id1, "x" );
		DateIdTestEntity ver2 = new DateIdTestEntity( id1, "y" );

		assert getAuditReader().find( DateIdTestEntity.class, id1, 1 ).getStr1().equals( "x" );
		assert getAuditReader().find( DateIdTestEntity.class, id1, 2 ).getStr1().equals( "y" );
	}
}