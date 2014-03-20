/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.integration.naming.ids;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.List;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@FailureExpectedWithNewMetamodel( message = " No support yet for referenced join columns unless they correspond with columns bound for an attribute binding." )

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
		SingularAttributeBinding attributeBinding = (SingularAttributeBinding) getMetadata().getEntityBinding(
				"org.hibernate.envers.test.integration.naming.ids.JoinMulIdNamingRefIngEntity_AUD"
		).locateAttributeBinding( "reference_id1" );
		List<Value> values = attributeBinding.getValues();
		assertTrue( !values.isEmpty() );
		assertEquals( "ID1_reference", ( (Column) values.get( 0 ) ).getColumnName().getText() );
		assertEquals( 1, values.size() );

		attributeBinding = (SingularAttributeBinding) getMetadata().getEntityBinding(
				"org.hibernate.envers.test.integration.naming.ids.JoinMulIdNamingRefIngEntity_AUD"
		).locateAttributeBinding( "reference_id2" );
		values = attributeBinding.getValues();
		assertTrue( !values.isEmpty() );
		assertEquals( "ID2_reference", ( (Column) values.get( 0 ) ).getColumnName().getText() );
		assertEquals( 1, values.size() );
	}
}