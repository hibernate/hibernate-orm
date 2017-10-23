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
import org.hibernate.envers.test.entities.ids.CompositeDateIdTestEntity;
import org.hibernate.envers.test.entities.ids.DateEmbId;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class CompositeDateId extends BaseEnversJPAFunctionalTestCase {
	private DateEmbId id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {CompositeDateIdTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		CompositeDateIdTestEntity dite = new CompositeDateIdTestEntity( new DateEmbId( new Date(), new Date() ), "x" );
		em.persist( dite );

		id1 = dite.getId();

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		dite = em.find( CompositeDateIdTestEntity.class, id1 );
		dite.setStr1( "y" );

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( CompositeDateIdTestEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		CompositeDateIdTestEntity ver1 = new CompositeDateIdTestEntity( id1, "x" );
		CompositeDateIdTestEntity ver2 = new CompositeDateIdTestEntity( id1, "y" );

		assert getAuditReader().find( CompositeDateIdTestEntity.class, id1, 1 ).getStr1().equals( "x" );
		assert getAuditReader().find( CompositeDateIdTestEntity.class, id1, 2 ).getStr1().equals( "y" );
	}
}