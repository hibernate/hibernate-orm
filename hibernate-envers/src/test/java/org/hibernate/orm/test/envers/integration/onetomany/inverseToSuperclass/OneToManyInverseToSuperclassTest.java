/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.inverseToSuperclass;

import java.util.ArrayList;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.junit.Test;

/**
 * @author Hern�n Chanfreau
 */

public class OneToManyInverseToSuperclassTest extends BaseEnversJPAFunctionalTestCase {

	private long m1_id;

	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/oneToMany/inverseToSuperclass/mappings.hbm.xml"};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		Root m1 = new Root();
		DetailSubclass det1 = new DetailSubclass2();
		DetailSubclass det2 = new DetailSubclass2();

		// Revision 1
		em.getTransaction().begin();

		det1.setStr2( "detail 1" );

		m1.setStr( "root" );
		m1.setItems( new ArrayList<DetailSubclass>() );
		m1.getItems().add( det1 );
		det1.setParent( m1 );

		em.persist( m1 );
		em.getTransaction().commit();
		m1_id = m1.getId();

		// Revision 2
		em.getTransaction().begin();

		m1 = em.find( Root.class, m1_id );

		det2.setStr2( "detail 2" );
		det2.setParent( m1 );
		m1.getItems().add( det2 );
		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		m1 = em.find( Root.class, m1_id );
		m1.setStr( "new root" );

		det1 = m1.getItems().get( 0 );
		det1.setStr2( "new detail" );
		DetailSubclass det3 = new DetailSubclass2();
		det3.setStr2( "detail 3" );
		det3.setParent( m1 );

		m1.getItems().get( 1 ).setParent( null );
		// m1.getItems().remove(1);
		m1.getItems().add( det3 );

		em.persist( m1 );
		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		m1 = em.find( Root.class, m1_id );

		det1 = m1.getItems().get( 0 );
		det1.setParent( null );
		// m1.getItems().remove(det1);

		em.persist( m1 );
		em.getTransaction().commit();

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
