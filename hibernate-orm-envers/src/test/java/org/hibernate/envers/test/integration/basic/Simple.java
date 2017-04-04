/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.basic;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.IntTestEntity;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Simple extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {IntTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		IntTestEntity ite = new IntTestEntity( 10 );
		em.persist( ite );
		id1 = ite.getId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		ite = em.find( IntTestEntity.class, id1 );
		ite.setNumber( 20 );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		Assert.assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( IntTestEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		IntTestEntity ver1 = new IntTestEntity( 10, id1 );
		IntTestEntity ver2 = new IntTestEntity( 20, id1 );

		Assert.assertEquals( ver1, getAuditReader().find( IntTestEntity.class, id1, 1 ) );
		Assert.assertEquals( ver2, getAuditReader().find( IntTestEntity.class, id1, 2 ) );
	}
}