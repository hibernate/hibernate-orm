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
package org.hibernate.envers.test.integration.naming;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.relational.Column;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class JoinNaming extends BaseEnversJPAFunctionalTestCase {
	private Integer ed_id1;
	private Integer ed_id2;
	private Integer ing_id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {JoinNamingRefEdEntity.class, JoinNamingRefIngEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		JoinNamingRefEdEntity ed1 = new JoinNamingRefEdEntity( "data1" );
		JoinNamingRefEdEntity ed2 = new JoinNamingRefEdEntity( "data2" );

		JoinNamingRefIngEntity ing1 = new JoinNamingRefIngEntity( "x", ed1 );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		em.persist( ed1 );
		em.persist( ed2 );
		em.persist( ing1 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		ed2 = em.find( JoinNamingRefEdEntity.class, ed2.getId() );

		ing1 = em.find( JoinNamingRefIngEntity.class, ing1.getId() );
		ing1.setData( "y" );
		ing1.setReference( ed2 );

		em.getTransaction().commit();

		//

		ed_id1 = ed1.getId();
		ed_id2 = ed2.getId();
		ing_id1 = ing1.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( JoinNamingRefEdEntity.class, ed_id1 ) );
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( JoinNamingRefEdEntity.class, ed_id2 ) );
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( JoinNamingRefIngEntity.class, ing_id1 ) );
	}

	@Test
	public void testHistoryOfEdId1() {
		JoinNamingRefEdEntity ver1 = new JoinNamingRefEdEntity( ed_id1, "data1" );

		assert getAuditReader().find( JoinNamingRefEdEntity.class, ed_id1, 1 ).equals( ver1 );
		assert getAuditReader().find( JoinNamingRefEdEntity.class, ed_id1, 2 ).equals( ver1 );
	}

	@Test
	public void testHistoryOfEdId2() {
		JoinNamingRefEdEntity ver1 = new JoinNamingRefEdEntity( ed_id2, "data2" );

		assert getAuditReader().find( JoinNamingRefEdEntity.class, ed_id2, 1 ).equals( ver1 );
		assert getAuditReader().find( JoinNamingRefEdEntity.class, ed_id2, 2 ).equals( ver1 );
	}

	@Test
	public void testHistoryOfIngId1() {
		JoinNamingRefIngEntity ver1 = new JoinNamingRefIngEntity( ing_id1, "x", null );
		JoinNamingRefIngEntity ver2 = new JoinNamingRefIngEntity( ing_id1, "y", null );

		assert getAuditReader().find( JoinNamingRefIngEntity.class, ing_id1, 1 ).equals( ver1 );
		assert getAuditReader().find( JoinNamingRefIngEntity.class, ing_id1, 2 ).equals( ver2 );

		assert getAuditReader().find( JoinNamingRefIngEntity.class, ing_id1, 1 ).getReference().equals(
				new JoinNamingRefEdEntity( ed_id1, "data1" )
		);
		assert getAuditReader().find( JoinNamingRefIngEntity.class, ing_id1, 2 ).getReference().equals(
				new JoinNamingRefEdEntity( ed_id2, "data2" )
		);
	}

	@SuppressWarnings({"unchecked"})
	@Test
	public void testJoinColumnName() {
		final AttributeBinding attributeBinding = getMetadata().getEntityBinding(
				"org.hibernate.envers.test.integration.naming.JoinNamingRefIngEntity_AUD"
		).locateAttributeBinding( "reference_id" );
		assertTrue( attributeBinding.getAttribute().isSingular() );
		final SingularAttributeBinding singularAttributeBinding = (SingularAttributeBinding) attributeBinding;
		assertTrue( !singularAttributeBinding.getValues().isEmpty() );

		assertEquals(
				"jnree_column_reference",
				( (Column) singularAttributeBinding.getValues().get( 0 ) ).getColumnName().getText()
		);
		assertEquals( singularAttributeBinding.getValues().size(), 1 );
	}
}