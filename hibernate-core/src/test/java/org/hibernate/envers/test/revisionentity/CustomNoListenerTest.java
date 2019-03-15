/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.revisionentity;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.revisionentity.CustomDataRevEntity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class CustomNoListenerTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class, CustomDataRevEntity.class };
	}

	// todo (6.0) - The AuditReader#getCurrentRevision API was removed
	//		This test can likely be removed since we expect users to use the Listener approach now.

//	@DynamicBeforeAll
//	public void prepareAuditData() throws InterruptedException {
//		EntityManager em = getEntityManager();
//
//		// Revision 1
//		em.getTransaction().begin();
//		StrTestEntity te = new StrTestEntity( "x" );
//		em.persist( te );
//		id = te.getId();
//
//		// Setting the data on the revision entity
//		CustomDataRevEntity custom = getAuditReader().getCurrentRevision( CustomDataRevEntity.class, false );
//		custom.setData( "data1" );
//
//		em.getTransaction().commit();
//
//		// Revision 2
//		em.getTransaction().begin();
//		te = em.find( StrTestEntity.class, id );
//		te.setStr( "y" );
//
//		// Setting the data on the revision entity
//		custom = getAuditReader().getCurrentRevision( CustomDataRevEntity.class, false );
//		custom.setData( "data2" );
//
//		em.getTransaction().commit();
//
//		// Revision 3 - no changes, but rev entity should be persisted
//		em.getTransaction().begin();
//
//		// Setting the data on the revision entity
//		custom = getAuditReader().getCurrentRevision( CustomDataRevEntity.class, true );
//		custom.setData( "data3" );
//
//		em.getTransaction().commit();
//
//		// No changes, rev entity won't be persisted
//		em.getTransaction().begin();
//
//		// Setting the data on the revision entity
//		custom = getAuditReader().getCurrentRevision( CustomDataRevEntity.class, false );
//		custom.setData( "data4" );
//
//		em.getTransaction().commit();
//
//		// Revision 4
//		em.getTransaction().begin();
//		te = em.find( StrTestEntity.class, id );
//		te.setStr( "z" );
//
//		// Setting the data on the revision entity
//		custom = getAuditReader().getCurrentRevision( CustomDataRevEntity.class, false );
//		custom.setData( "data5" );
//
//		custom = getAuditReader().getCurrentRevision( CustomDataRevEntity.class, false );
//		custom.setData( "data5bis" );
//
//		em.getTransaction().commit();
//	}
//
//	@DynamicTest
//	public void testFindRevision() {
//		AuditReader vr = getAuditReader();
//
//		assert "data1".equals( vr.findRevision( CustomDataRevEntity.class, 1 ).getData() );
//		assert "data2".equals( vr.findRevision( CustomDataRevEntity.class, 2 ).getData() );
//		assert "data3".equals( vr.findRevision( CustomDataRevEntity.class, 3 ).getData() );
//		assert "data5bis".equals( vr.findRevision( CustomDataRevEntity.class, 4 ).getData() );
//	}
//
//	@DynamicTest
//	public void testRevisionsCounts() {
//		assert Arrays.asList( 1, 2, 4 ).equals( getAuditReader().getRevisions( StrTestEntity.class, id ) );
//	}
//
//	@DynamicTest
//	public void testHistoryOfId1() {
//		StrTestEntity ver1 = new StrTestEntity( id, "x" );
//		StrTestEntity ver2 = new StrTestEntity( id, "y" );
//		StrTestEntity ver3 = new StrTestEntity( id, "z" );
//
//		assert getAuditReader().find( StrTestEntity.class, id, 1 ).equals( ver1 );
//		assert getAuditReader().find( StrTestEntity.class, id, 2 ).equals( ver2 );
//		assert getAuditReader().find( StrTestEntity.class, id, 3 ).equals( ver2 );
//		assert getAuditReader().find( StrTestEntity.class, id, 4 ).equals( ver3 );
//	}
}