/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.naming;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicNaming extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;
	private Integer id2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {NamingTestEntity1.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		NamingTestEntity1 nte1 = new NamingTestEntity1( "data1" );
		NamingTestEntity1 nte2 = new NamingTestEntity1( "data2" );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		em.persist( nte1 );
		em.persist( nte2 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		nte1 = em.find( NamingTestEntity1.class, nte1.getId() );
		nte1.setData( "data1'" );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		nte2 = em.find( NamingTestEntity1.class, nte2.getId() );
		nte2.setData( "data2'" );

		em.getTransaction().commit();

		//

		id1 = nte1.getId();
		id2 = nte2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( NamingTestEntity1.class, id1 ) );

		assert Arrays.asList( 1, 3 ).equals( getAuditReader().getRevisions( NamingTestEntity1.class, id2 ) );
	}

	@Test
	public void testHistoryOfId1() {
		NamingTestEntity1 ver1 = new NamingTestEntity1( id1, "data1" );
		NamingTestEntity1 ver2 = new NamingTestEntity1( id1, "data1'" );

		assert getAuditReader().find( NamingTestEntity1.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( NamingTestEntity1.class, id1, 2 ).equals( ver2 );
		assert getAuditReader().find( NamingTestEntity1.class, id1, 3 ).equals( ver2 );
	}

	@Test
	public void testHistoryOfId2() {
		NamingTestEntity1 ver1 = new NamingTestEntity1( id2, "data2" );
		NamingTestEntity1 ver2 = new NamingTestEntity1( id2, "data2'" );

		assert getAuditReader().find( NamingTestEntity1.class, id2, 1 ).equals( ver1 );
		assert getAuditReader().find( NamingTestEntity1.class, id2, 2 ).equals( ver1 );
		assert getAuditReader().find( NamingTestEntity1.class, id2, 3 ).equals( ver2 );
	}

	@Test
	public void testTableName() {
		assert "naming_test_entity_1_versions".equals(
				metadata().getEntityBinding( "org.hibernate.envers.test.integration.naming.NamingTestEntity1_AUD" ).getTable().getName()
		);
	}
}
