/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytomany.biowned;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.manytomany.biowned.ListBiowning1Entity;
import org.hibernate.envers.test.entities.manytomany.biowned.ListBiowning2Entity;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicBiowned extends BaseEnversJPAFunctionalTestCase {
	private Integer o1_1_id;
	private Integer o1_2_id;
	private Integer o2_1_id;
	private Integer o2_2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ListBiowning1Entity.class, ListBiowning2Entity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		ListBiowning1Entity o1_1 = new ListBiowning1Entity( "o1_1" );
		ListBiowning1Entity o1_2 = new ListBiowning1Entity( "o1_2" );
		ListBiowning2Entity o2_1 = new ListBiowning2Entity( "o2_1" );
		ListBiowning2Entity o2_2 = new ListBiowning2Entity( "o2_2" );

		// Revision 1
		em.getTransaction().begin();

		em.persist( o1_1 );
		em.persist( o1_2 );
		em.persist( o2_1 );
		em.persist( o2_2 );

		em.getTransaction().commit();
		em.clear();

		// Revision 2 (1_1 <-> 2_1; 1_2 <-> 2_2)

		em.getTransaction().begin();

		o1_1 = em.find( ListBiowning1Entity.class, o1_1.getId() );
		o1_2 = em.find( ListBiowning1Entity.class, o1_2.getId() );
		o2_1 = em.find( ListBiowning2Entity.class, o2_1.getId() );
		o2_2 = em.find( ListBiowning2Entity.class, o2_2.getId() );

		o1_1.getReferences().add( o2_1 );
		o1_2.getReferences().add( o2_2 );

		em.getTransaction().commit();
		em.clear();

		// Revision 3 (1_1 <-> 2_1, 2_2; 1_2 <-> 2_2)
		em.getTransaction().begin();

		o1_1 = em.find( ListBiowning1Entity.class, o1_1.getId() );
		o2_2 = em.find( ListBiowning2Entity.class, o2_2.getId() );

		o1_1.getReferences().add( o2_2 );

		em.getTransaction().commit();
		em.clear();

		// Revision 4 (1_2 <-> 2_1, 2_2)
		em.getTransaction().begin();

		o1_1 = em.find( ListBiowning1Entity.class, o1_1.getId() );
		o1_2 = em.find( ListBiowning1Entity.class, o1_2.getId() );
		o2_1 = em.find( ListBiowning2Entity.class, o2_1.getId() );
		o2_2 = em.find( ListBiowning2Entity.class, o2_2.getId() );

		o2_2.getReferences().remove( o1_1 );
		o2_1.getReferences().remove( o1_1 );
		o2_1.getReferences().add( o1_2 );

		em.getTransaction().commit();
		em.clear();

		// Revision 5 (1_1 <-> 2_2, 1_2 <-> 2_2)
		em.getTransaction().begin();

		o1_1 = em.find( ListBiowning1Entity.class, o1_1.getId() );
		o1_2 = em.find( ListBiowning1Entity.class, o1_2.getId() );
		o2_1 = em.find( ListBiowning2Entity.class, o2_1.getId() );
		o2_2 = em.find( ListBiowning2Entity.class, o2_2.getId() );

		o1_2.getReferences().remove( o2_1 );
		o1_1.getReferences().add( o2_2 );

		em.getTransaction().commit();
		em.clear();

		//

		o1_1_id = o1_1.getId();
		o1_2_id = o1_2.getId();
		o2_1_id = o2_1.getId();
		o2_2_id = o2_2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		// Although it would seem that when modifying references both entities should be marked as modified, because
		// ownly the owning side is notified (because of the bi-owning mapping), a revision is created only for
		// the entity where the collection was directly modified.

		assertEquals(
				Arrays.asList( 1, 2, 3, 5 ), getAuditReader().getRevisions(
				ListBiowning1Entity.class,
				o1_1_id
		)
		);
		assertEquals( Arrays.asList( 1, 2, 5 ), getAuditReader().getRevisions( ListBiowning1Entity.class, o1_2_id ) );

		assertEquals( Arrays.asList( 1, 4 ), getAuditReader().getRevisions( ListBiowning2Entity.class, o2_1_id ) );
		assertEquals( Arrays.asList( 1, 4 ), getAuditReader().getRevisions( ListBiowning2Entity.class, o2_2_id ) );
	}

	@Test
	public void testHistoryOfO1_1() {
		ListBiowning2Entity o2_1 = getEntityManager().find( ListBiowning2Entity.class, o2_1_id );
		ListBiowning2Entity o2_2 = getEntityManager().find( ListBiowning2Entity.class, o2_2_id );

		ListBiowning1Entity rev1 = getAuditReader().find( ListBiowning1Entity.class, o1_1_id, 1 );
		ListBiowning1Entity rev2 = getAuditReader().find( ListBiowning1Entity.class, o1_1_id, 2 );
		ListBiowning1Entity rev3 = getAuditReader().find( ListBiowning1Entity.class, o1_1_id, 3 );
		ListBiowning1Entity rev4 = getAuditReader().find( ListBiowning1Entity.class, o1_1_id, 4 );
		ListBiowning1Entity rev5 = getAuditReader().find( ListBiowning1Entity.class, o1_1_id, 5 );

		assert TestTools.checkCollection( rev1.getReferences() );
		assert TestTools.checkCollection( rev2.getReferences(), o2_1 );
		assert TestTools.checkCollection( rev3.getReferences(), o2_1, o2_2 );
		assert TestTools.checkCollection( rev4.getReferences() );
		assert TestTools.checkCollection( rev5.getReferences(), o2_2 );
	}

	@Test
	public void testHistoryOfO1_2() {
		ListBiowning2Entity o2_1 = getEntityManager().find( ListBiowning2Entity.class, o2_1_id );
		ListBiowning2Entity o2_2 = getEntityManager().find( ListBiowning2Entity.class, o2_2_id );

		ListBiowning1Entity rev1 = getAuditReader().find( ListBiowning1Entity.class, o1_2_id, 1 );
		ListBiowning1Entity rev2 = getAuditReader().find( ListBiowning1Entity.class, o1_2_id, 2 );
		ListBiowning1Entity rev3 = getAuditReader().find( ListBiowning1Entity.class, o1_2_id, 3 );
		ListBiowning1Entity rev4 = getAuditReader().find( ListBiowning1Entity.class, o1_2_id, 4 );
		ListBiowning1Entity rev5 = getAuditReader().find( ListBiowning1Entity.class, o1_2_id, 5 );

		assert TestTools.checkCollection( rev1.getReferences() );
		assert TestTools.checkCollection( rev2.getReferences(), o2_2 );
		assert TestTools.checkCollection( rev3.getReferences(), o2_2 );
		assert TestTools.checkCollection( rev4.getReferences(), o2_1, o2_2 );
		assert TestTools.checkCollection( rev5.getReferences(), o2_2 );
	}

	@Test
	public void testHistoryOfO2_1() {
		ListBiowning1Entity o1_1 = getEntityManager().find( ListBiowning1Entity.class, o1_1_id );
		ListBiowning1Entity o1_2 = getEntityManager().find( ListBiowning1Entity.class, o1_2_id );

		ListBiowning2Entity rev1 = getAuditReader().find( ListBiowning2Entity.class, o2_1_id, 1 );
		ListBiowning2Entity rev2 = getAuditReader().find( ListBiowning2Entity.class, o2_1_id, 2 );
		ListBiowning2Entity rev3 = getAuditReader().find( ListBiowning2Entity.class, o2_1_id, 3 );
		ListBiowning2Entity rev4 = getAuditReader().find( ListBiowning2Entity.class, o2_1_id, 4 );
		ListBiowning2Entity rev5 = getAuditReader().find( ListBiowning2Entity.class, o2_1_id, 5 );

		assert TestTools.checkCollection( rev1.getReferences() );
		assert TestTools.checkCollection( rev2.getReferences(), o1_1 );
		assert TestTools.checkCollection( rev3.getReferences(), o1_1 );
		assert TestTools.checkCollection( rev4.getReferences(), o1_2 );
		assert TestTools.checkCollection( rev5.getReferences() );
	}

	@Test
	public void testHistoryOfO2_2() {
		ListBiowning1Entity o1_1 = getEntityManager().find( ListBiowning1Entity.class, o1_1_id );
		ListBiowning1Entity o1_2 = getEntityManager().find( ListBiowning1Entity.class, o1_2_id );

		ListBiowning2Entity rev1 = getAuditReader().find( ListBiowning2Entity.class, o2_2_id, 1 );
		ListBiowning2Entity rev2 = getAuditReader().find( ListBiowning2Entity.class, o2_2_id, 2 );
		ListBiowning2Entity rev3 = getAuditReader().find( ListBiowning2Entity.class, o2_2_id, 3 );
		ListBiowning2Entity rev4 = getAuditReader().find( ListBiowning2Entity.class, o2_2_id, 4 );
		ListBiowning2Entity rev5 = getAuditReader().find( ListBiowning2Entity.class, o2_2_id, 5 );

		assert TestTools.checkCollection( rev1.getReferences() );
		assert TestTools.checkCollection( rev2.getReferences(), o1_2 );
		assert TestTools.checkCollection( rev3.getReferences(), o1_1, o1_2 );
		assert TestTools.checkCollection( rev4.getReferences(), o1_2 );
		assert TestTools.checkCollection( rev5.getReferences(), o1_1, o1_2 );
	}
}