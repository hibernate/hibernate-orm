/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.naming.ids;

import java.util.Arrays;
import java.util.Iterator;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.mapping.Column;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class JoinMulIdNaming extends BaseEnversJPAFunctionalTestCase {
	private MulIdNaming ed_id1;
	private MulIdNaming ed_id2;
	private MulIdNaming ing_id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {JoinMulIdNamingRefEdEntity.class, JoinMulIdNamingRefIngEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		ed_id1 = new MulIdNaming( 10, 20 );
		ed_id2 = new MulIdNaming( 11, 21 );
		ing_id1 = new MulIdNaming( 12, 22 );

		JoinMulIdNamingRefEdEntity ed1 = new JoinMulIdNamingRefEdEntity( ed_id1, "data1" );
		JoinMulIdNamingRefEdEntity ed2 = new JoinMulIdNamingRefEdEntity( ed_id2, "data2" );

		JoinMulIdNamingRefIngEntity ing1 = new JoinMulIdNamingRefIngEntity( ing_id1, "x", ed1 );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		em.persist( ed1 );
		em.persist( ed2 );
		em.persist( ing1 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		ed2 = em.find( JoinMulIdNamingRefEdEntity.class, ed_id2 );

		ing1 = em.find( JoinMulIdNamingRefIngEntity.class, ing_id1 );
		ing1.setData( "y" );
		ing1.setReference( ed2 );

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals(
				getAuditReader().getRevisions(
						JoinMulIdNamingRefEdEntity.class,
						ed_id1
				)
		);
		assert Arrays.asList( 1, 2 ).equals(
				getAuditReader().getRevisions(
						JoinMulIdNamingRefEdEntity.class,
						ed_id2
				)
		);
		assert Arrays.asList( 1, 2 ).equals(
				getAuditReader().getRevisions(
						JoinMulIdNamingRefIngEntity.class,
						ing_id1
				)
		);
	}

	@Test
	public void testHistoryOfEdId1() {
		JoinMulIdNamingRefEdEntity ver1 = new JoinMulIdNamingRefEdEntity( ed_id1, "data1" );

		assert getAuditReader().find( JoinMulIdNamingRefEdEntity.class, ed_id1, 1 ).equals( ver1 );
		assert getAuditReader().find( JoinMulIdNamingRefEdEntity.class, ed_id1, 2 ).equals( ver1 );
	}

	@Test
	public void testHistoryOfEdId2() {
		JoinMulIdNamingRefEdEntity ver1 = new JoinMulIdNamingRefEdEntity( ed_id2, "data2" );

		assert getAuditReader().find( JoinMulIdNamingRefEdEntity.class, ed_id2, 1 ).equals( ver1 );
		assert getAuditReader().find( JoinMulIdNamingRefEdEntity.class, ed_id2, 2 ).equals( ver1 );
	}

	@Test
	public void testHistoryOfIngId1() {
		JoinMulIdNamingRefIngEntity ver1 = new JoinMulIdNamingRefIngEntity( ing_id1, "x", null );
		JoinMulIdNamingRefIngEntity ver2 = new JoinMulIdNamingRefIngEntity( ing_id1, "y", null );

		assert getAuditReader().find( JoinMulIdNamingRefIngEntity.class, ing_id1, 1 ).equals( ver1 );
		assert getAuditReader().find( JoinMulIdNamingRefIngEntity.class, ing_id1, 2 ).equals( ver2 );

		assert getAuditReader().find( JoinMulIdNamingRefIngEntity.class, ing_id1, 1 ).getReference().equals(
				new JoinMulIdNamingRefEdEntity( ed_id1, "data1" )
		);
		assert getAuditReader().find( JoinMulIdNamingRefIngEntity.class, ing_id1, 2 ).getReference().equals(
				new JoinMulIdNamingRefEdEntity( ed_id2, "data2" )
		);
	}

	@SuppressWarnings({"unchecked"})
	@Test
	public void testJoinColumnNames() {
		Iterator<Column> columns = metadata().getEntityBinding(
				"org.hibernate.envers.test.integration.naming.ids.JoinMulIdNamingRefIngEntity_AUD"
		).getProperty( "reference_id1" ).getColumnIterator();

		assertTrue( columns.hasNext() );
		assertEquals( "ID1_reference", columns.next().getName() );
		assertFalse( columns.hasNext() );

		columns = metadata().getEntityBinding(
				"org.hibernate.envers.test.integration.naming.ids.JoinMulIdNamingRefIngEntity_AUD"
		).getProperty( "reference_id2" ).getColumnIterator();

		assertTrue( columns.hasNext() );
		assertEquals( "ID2_reference", columns.next().getName() );
		assertFalse( columns.hasNext() );
	}
}