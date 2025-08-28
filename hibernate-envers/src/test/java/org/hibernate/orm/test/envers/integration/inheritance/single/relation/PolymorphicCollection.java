/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.single.relation;

import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class PolymorphicCollection extends BaseEnversJPAFunctionalTestCase {
	private Integer ed_id1;
	private Integer c_id;
	private Integer p_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ChildIngEntity.class, ParentIngEntity.class, ReferencedEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Rev 1
		em.getTransaction().begin();

		ReferencedEntity re = new ReferencedEntity();
		em.persist( re );
		ed_id1 = re.getId();

		em.getTransaction().commit();

		// Rev 2
		em.getTransaction().begin();

		re = em.find( ReferencedEntity.class, ed_id1 );

		ParentIngEntity pie = new ParentIngEntity( "x" );
		pie.setReferenced( re );
		em.persist( pie );
		p_id = pie.getId();

		em.getTransaction().commit();

		// Rev 3
		em.getTransaction().begin();

		re = em.find( ReferencedEntity.class, ed_id1 );

		ChildIngEntity cie = new ChildIngEntity( "y", 1l );
		cie.setReferenced( re );
		em.persist( cie );
		c_id = cie.getId();

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( ReferencedEntity.class, ed_id1 ) );
		assert Arrays.asList( 2 ).equals( getAuditReader().getRevisions( ParentIngEntity.class, p_id ) );
		assert Arrays.asList( 3 ).equals( getAuditReader().getRevisions( ChildIngEntity.class, c_id ) );
	}

	@Test
	public void testHistoryOfReferencedCollection() {
		assert getAuditReader().find( ReferencedEntity.class, ed_id1, 1 ).getReferencing().size() == 0;
		assert getAuditReader().find( ReferencedEntity.class, ed_id1, 2 ).getReferencing().equals(
				TestTools.makeSet( new ParentIngEntity( p_id, "x" ) )
		);
		assert getAuditReader().find( ReferencedEntity.class, ed_id1, 3 ).getReferencing().equals(
				TestTools.makeSet( new ParentIngEntity( p_id, "x" ), new ChildIngEntity( c_id, "y", 1l ) )
		);
	}
}
