/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.reventity.CustomDataRevEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class CustomNoListener extends BaseEnversJPAFunctionalTestCase {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class, CustomDataRevEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() throws InterruptedException {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		StrTestEntity te = new StrTestEntity( "x" );
		em.persist( te );
		id = te.getId();

		// Setting the data on the revision entity
		CustomDataRevEntity custom = getAuditReader().getCurrentRevision( CustomDataRevEntity.class, false );
		custom.setData( "data1" );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();
		te = em.find( StrTestEntity.class, id );
		te.setStr( "y" );

		// Setting the data on the revision entity
		custom = getAuditReader().getCurrentRevision( CustomDataRevEntity.class, false );
		custom.setData( "data2" );

		em.getTransaction().commit();

		// Revision 3 - no changes, but rev entity should be persisted
		em.getTransaction().begin();

		// Setting the data on the revision entity
		custom = getAuditReader().getCurrentRevision( CustomDataRevEntity.class, true );
		custom.setData( "data3" );

		em.getTransaction().commit();

		// No changes, rev entity won't be persisted
		em.getTransaction().begin();

		// Setting the data on the revision entity
		custom = getAuditReader().getCurrentRevision( CustomDataRevEntity.class, false );
		custom.setData( "data4" );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();
		te = em.find( StrTestEntity.class, id );
		te.setStr( "z" );

		// Setting the data on the revision entity
		custom = getAuditReader().getCurrentRevision( CustomDataRevEntity.class, false );
		custom.setData( "data5" );

		custom = getAuditReader().getCurrentRevision( CustomDataRevEntity.class, false );
		custom.setData( "data5bis" );

		em.getTransaction().commit();
	}

	@Test
	public void testFindRevision() {
		AuditReader vr = getAuditReader();

		assert "data1".equals( vr.findRevision( CustomDataRevEntity.class, 1 ).getData() );
		assert "data2".equals( vr.findRevision( CustomDataRevEntity.class, 2 ).getData() );
		assert "data3".equals( vr.findRevision( CustomDataRevEntity.class, 3 ).getData() );
		assert "data5bis".equals( vr.findRevision( CustomDataRevEntity.class, 4 ).getData() );
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 4 ).equals( getAuditReader().getRevisions( StrTestEntity.class, id ) );
	}

	@Test
	public void testHistoryOfId1() {
		StrTestEntity ver1 = new StrTestEntity( "x", id );
		StrTestEntity ver2 = new StrTestEntity( "y", id );
		StrTestEntity ver3 = new StrTestEntity( "z", id );

		assert getAuditReader().find( StrTestEntity.class, id, 1 ).equals( ver1 );
		assert getAuditReader().find( StrTestEntity.class, id, 2 ).equals( ver2 );
		assert getAuditReader().find( StrTestEntity.class, id, 3 ).equals( ver2 );
		assert getAuditReader().find( StrTestEntity.class, id, 4 ).equals( ver3 );
	}
}
