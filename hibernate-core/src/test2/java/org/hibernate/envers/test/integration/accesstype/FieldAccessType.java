/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.integration.accesstype;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class FieldAccessType extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {FieldAccessTypeEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		FieldAccessTypeEntity fate = new FieldAccessTypeEntity( "data" );
		em.persist( fate );
		id1 = fate.readId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		fate = em.find( FieldAccessTypeEntity.class, id1 );
		fate.writeData( "data2" );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		Assert.assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( FieldAccessTypeEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		FieldAccessTypeEntity ver1 = new FieldAccessTypeEntity( id1, "data" );
		FieldAccessTypeEntity ver2 = new FieldAccessTypeEntity( id1, "data2" );
		Assert.assertEquals( ver1, getAuditReader().find( FieldAccessTypeEntity.class, id1, 1 ) );
		Assert.assertEquals( ver2, getAuditReader().find( FieldAccessTypeEntity.class, id1, 2 ) );
	}
}