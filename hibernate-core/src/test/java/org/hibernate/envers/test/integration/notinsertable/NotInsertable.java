/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.notinsertable;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class NotInsertable extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {NotInsertableTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		NotInsertableTestEntity dte = new NotInsertableTestEntity( "data1" );
		em.persist( dte );
		id1 = dte.getId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		dte = em.find( NotInsertableTestEntity.class, id1 );
		dte.setData( "data2" );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( NotInsertableTestEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		NotInsertableTestEntity ver1 = new NotInsertableTestEntity( id1, "data1", "data1" );
		NotInsertableTestEntity ver2 = new NotInsertableTestEntity( id1, "data2", "data2" );

		assert getAuditReader().find( NotInsertableTestEntity.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( NotInsertableTestEntity.class, id1, 2 ).equals( ver2 );
	}
}