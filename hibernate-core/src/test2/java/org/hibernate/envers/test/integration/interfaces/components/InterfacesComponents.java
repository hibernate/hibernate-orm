/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.interfaces.components;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class InterfacesComponents extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ComponentTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		ComponentTestEntity cte1 = new ComponentTestEntity( new Component1( "a" ) );

		em.persist( cte1 );

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		cte1 = em.find( ComponentTestEntity.class, cte1.getId() );

		cte1.setComp1( new Component1( "b" ) );

		em.getTransaction().commit();

		// Revision 3
		em = getEntityManager();
		em.getTransaction().begin();

		cte1 = em.find( ComponentTestEntity.class, cte1.getId() );

		cte1.getComp1().setData( "c" );

		em.getTransaction().commit();

		id1 = cte1.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( ComponentTestEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		ComponentTestEntity ver1 = new ComponentTestEntity( id1, new Component1( "a" ) );
		ComponentTestEntity ver2 = new ComponentTestEntity( id1, new Component1( "b" ) );
		ComponentTestEntity ver3 = new ComponentTestEntity( id1, new Component1( "c" ) );

		assert getAuditReader().find( ComponentTestEntity.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( ComponentTestEntity.class, id1, 2 ).equals( ver2 );
		assert getAuditReader().find( ComponentTestEntity.class, id1, 3 ).equals( ver3 );
	}
}