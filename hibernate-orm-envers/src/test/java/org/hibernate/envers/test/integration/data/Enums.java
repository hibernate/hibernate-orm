/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.data;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Enums extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {EnumTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		EnumTestEntity ete = new EnumTestEntity( EnumTestEntity.E1.X, EnumTestEntity.E2.A );
		em.persist( ete );
		id1 = ete.getId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		ete = em.find( EnumTestEntity.class, id1 );
		ete.setEnum1( EnumTestEntity.E1.Y );
		ete.setEnum2( EnumTestEntity.E2.B );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( EnumTestEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		EnumTestEntity ver1 = new EnumTestEntity( id1, EnumTestEntity.E1.X, EnumTestEntity.E2.A );
		EnumTestEntity ver2 = new EnumTestEntity( id1, EnumTestEntity.E1.Y, EnumTestEntity.E2.B );

		assert getAuditReader().find( EnumTestEntity.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( EnumTestEntity.class, id1, 2 ).equals( ver2 );
	}
}