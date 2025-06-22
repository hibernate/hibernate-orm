/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany.inverseToSuperclass;

import java.util.ArrayList;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.junit.Test;

/**
 * @author Hern�n Chanfreau
 */
public class ManyToManyInverseToSuperclassTest extends BaseEnversJPAFunctionalTestCase {
	private long m1_id;

	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/manyToMany/inverseToSuperclass/mappings.hbm.xml"};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		Root m1 = new Root();
		DetailSubclass det1 = new DetailSubclass2();

		// Revision 1
		em.getTransaction().begin();

		det1.setStr2( "detail 1" );

		m1.setStr( "root" );
		m1.setItems( new ArrayList<DetailSubclass>() );
		m1.getItems().add( det1 );

		det1.setRoots( new ArrayList<Root>() );
		det1.getRoots().add( m1 );

		em.persist( m1 );
		em.getTransaction().commit();
		m1_id = m1.getId();
	}

	@Test
	public void testHistoryExists() {
		Root rev1_1 = getAuditReader().find( Root.class, m1_id, 1 );
		Root rev1_2 = getAuditReader().find( Root.class, m1_id, 2 );
		Root rev1_3 = getAuditReader().find( Root.class, m1_id, 3 );
		Root rev1_4 = getAuditReader().find( Root.class, m1_id, 4 );

		assert (rev1_1 != null);
		assert (rev1_2 != null);
		assert (rev1_3 != null);
		assert (rev1_4 != null);
	}

}
