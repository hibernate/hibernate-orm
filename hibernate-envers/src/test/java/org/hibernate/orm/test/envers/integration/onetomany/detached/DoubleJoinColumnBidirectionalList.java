/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.detached;

import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.onetomany.detached.DoubleListJoinColumnBidirectionalRefEdEntity1;
import org.hibernate.orm.test.envers.entities.onetomany.detached.DoubleListJoinColumnBidirectionalRefEdEntity2;
import org.hibernate.orm.test.envers.entities.onetomany.detached.DoubleListJoinColumnBidirectionalRefIngEntity;

import org.junit.Test;

import static org.hibernate.orm.test.envers.tools.TestTools.checkCollection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for a double "fake" bidirectional mapping where one side uses @OneToMany+@JoinColumn
 * (and thus owns the relation), and the other uses a @ManyToOne(insertable=false, updatable=false).
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class DoubleJoinColumnBidirectionalList extends BaseEnversJPAFunctionalTestCase {
	private Integer ed1_1_id;
	private Integer ed2_1_id;
	private Integer ed1_2_id;
	private Integer ed2_2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				DoubleListJoinColumnBidirectionalRefEdEntity1.class,
				DoubleListJoinColumnBidirectionalRefEdEntity2.class
		};
	}

	@Test
	@Priority(10)
	public void createData() {
		EntityManager em = getEntityManager();

		DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_1 = new DoubleListJoinColumnBidirectionalRefEdEntity1(
				"ed1_1",
				null
		);
		DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_2 = new DoubleListJoinColumnBidirectionalRefEdEntity1(
				"ed1_2",
				null
		);

		DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_1 = new DoubleListJoinColumnBidirectionalRefEdEntity2(
				"ed2_1",
				null
		);
		DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_2 = new DoubleListJoinColumnBidirectionalRefEdEntity2(
				"ed2_2",
				null
		);

		DoubleListJoinColumnBidirectionalRefIngEntity ing1 = new DoubleListJoinColumnBidirectionalRefIngEntity( "coll1" );
		DoubleListJoinColumnBidirectionalRefIngEntity ing2 = new DoubleListJoinColumnBidirectionalRefIngEntity( "coll2" );

		// Revision 1 (ing1: ed1_1, ed2_1, ing2: ed1_2, ed2_2)
		em.getTransaction().begin();

		ing1.getReferences1().add( ed1_1 );
		ing1.getReferences2().add( ed2_1 );

		ing2.getReferences1().add( ed1_2 );
		ing2.getReferences2().add( ed2_2 );

		em.persist( ed1_1 );
		em.persist( ed1_2 );
		em.persist( ed2_1 );
		em.persist( ed2_2 );
		em.persist( ing1 );
		em.persist( ing2 );

		em.getTransaction().commit();

		// Revision 2 (ing1: ed1_1, ed1_2, ed2_1, ed2_2)
		em.getTransaction().begin();

		ing1 = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
		ing2 = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
		ed1_1 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1.getId() );
		ed1_2 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2.getId() );
		ed2_1 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1.getId() );
		ed2_2 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2.getId() );

		ing2.getReferences1().clear();
		ing2.getReferences2().clear();

		ing1.getReferences1().add( ed1_2 );
		ing1.getReferences2().add( ed2_2 );

		em.getTransaction().commit();
		em.clear();

		// Revision 3 (ing1: ed1_1, ed1_2, ed2_1, ed2_2)
		em.getTransaction().begin();

		ing1 = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
		ing2 = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
		ed1_1 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1.getId() );
		ed1_2 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2.getId() );
		ed2_1 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1.getId() );
		ed2_2 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2.getId() );

		ed1_1.setData( "ed1_1 bis" );
		ed2_2.setData( "ed2_2 bis" );

		em.getTransaction().commit();
		em.clear();

		// Revision 4 (ing1: ed2_2, ing2: ed2_1, ed1_1, ed1_2)
		em.getTransaction().begin();

		ing1 = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
		ing2 = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
		ed1_1 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1.getId() );
		ed1_2 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2.getId() );
		ed2_1 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1.getId() );
		ed2_2 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2.getId() );

		ing1.getReferences1().clear();
		ing2.getReferences1().add( ed1_1 );
		ing2.getReferences1().add( ed1_2 );

		ing1.getReferences2().remove( ed2_1 );
		ing2.getReferences2().add( ed2_1 );

		em.getTransaction().commit();
		em.clear();

		//

		ing1_id = ing1.getId();
		ing2_id = ing2.getId();

		ed1_1_id = ed1_1.getId();
		ed1_2_id = ed1_2.getId();
		ed2_1_id = ed2_1.getId();
		ed2_2_id = ed2_2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assertEquals(
				Arrays.asList( 1, 2, 4 ), getAuditReader().getRevisions(
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				ing1_id
		)
		);
		assertEquals(
				Arrays.asList( 1, 2, 4 ), getAuditReader().getRevisions(
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				ing2_id
		)
		);

		assertEquals(
				Arrays.asList( 1, 3, 4 ), getAuditReader().getRevisions(
				DoubleListJoinColumnBidirectionalRefEdEntity1.class,
				ed1_1_id
		)
		);
		assertEquals(
				Arrays.asList( 1, 2, 4 ), getAuditReader().getRevisions(
				DoubleListJoinColumnBidirectionalRefEdEntity1.class,
				ed1_2_id
		)
		);

		assertEquals(
				Arrays.asList( 1, 4 ), getAuditReader().getRevisions(
				DoubleListJoinColumnBidirectionalRefEdEntity2.class,
				ed2_1_id
		)
		);
		assertEquals(
				Arrays.asList( 1, 2, 3 ), getAuditReader().getRevisions(
				DoubleListJoinColumnBidirectionalRefEdEntity2.class,
				ed2_2_id
		)
		);
	}

	@Test
	public void testHistoryOfIng1() {
		DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_1_fromRev1 = new DoubleListJoinColumnBidirectionalRefEdEntity1(
				ed1_1_id,
				"ed1_1",
				null
		);
		DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_1_fromRev3 = new DoubleListJoinColumnBidirectionalRefEdEntity1(
				ed1_1_id,
				"ed1_1 bis",
				null
		);
		DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_2 = getEntityManager().find(
				DoubleListJoinColumnBidirectionalRefEdEntity1.class,
				ed1_2_id
		);
		DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_1 = getEntityManager().find(
				DoubleListJoinColumnBidirectionalRefEdEntity2.class,
				ed2_1_id
		);
		DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_2_fromRev1 = new DoubleListJoinColumnBidirectionalRefEdEntity2(
				ed2_2_id,
				"ed2_2",
				null
		);
		DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_2_fromRev3 = new DoubleListJoinColumnBidirectionalRefEdEntity2(
				ed2_2_id,
				"ed2_2 bis",
				null
		);

		DoubleListJoinColumnBidirectionalRefIngEntity rev1 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				ing1_id,
				1
		);
		DoubleListJoinColumnBidirectionalRefIngEntity rev2 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				ing1_id,
				2
		);
		DoubleListJoinColumnBidirectionalRefIngEntity rev3 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				ing1_id,
				3
		);
		DoubleListJoinColumnBidirectionalRefIngEntity rev4 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				ing1_id,
				4
		);

		assertTrue( checkCollection( rev1.getReferences1(), ed1_1_fromRev1 ) );
		assertTrue( checkCollection( rev2.getReferences1(), ed1_1_fromRev1, ed1_2 ) );
		assertTrue( checkCollection( rev3.getReferences1(), ed1_1_fromRev3, ed1_2 ) );
		assertTrue( checkCollection( rev4.getReferences1() ) );

		assertTrue( checkCollection( rev1.getReferences2(), ed2_1 ) );
		assertTrue( checkCollection( rev2.getReferences2(), ed2_1, ed2_2_fromRev1 ) );
		assertTrue( checkCollection( rev3.getReferences2(), ed2_1, ed2_2_fromRev3 ) );
		assertTrue( checkCollection( rev4.getReferences2(), ed2_2_fromRev3 ) );
	}

	@Test
	public void testHistoryOfIng2() {
		DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_1_fromRev3 = new DoubleListJoinColumnBidirectionalRefEdEntity1(
				ed1_1_id,
				"ed1_1 bis",
				null
		);
		DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_2 = getEntityManager().find(
				DoubleListJoinColumnBidirectionalRefEdEntity1.class,
				ed1_2_id
		);
		DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_1 = getEntityManager().find(
				DoubleListJoinColumnBidirectionalRefEdEntity2.class,
				ed2_1_id
		);
		DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_2_fromRev1 = new DoubleListJoinColumnBidirectionalRefEdEntity2(
				ed2_2_id,
				"ed2_2",
				null
		);

		DoubleListJoinColumnBidirectionalRefIngEntity rev1 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				ing2_id,
				1
		);
		DoubleListJoinColumnBidirectionalRefIngEntity rev2 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				ing2_id,
				2
		);
		DoubleListJoinColumnBidirectionalRefIngEntity rev3 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				ing2_id,
				3
		);
		DoubleListJoinColumnBidirectionalRefIngEntity rev4 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				ing2_id,
				4
		);

		assertTrue( checkCollection( rev1.getReferences1(), ed1_2 ) );
		assertTrue( checkCollection( rev2.getReferences1() ) );
		assertTrue( checkCollection( rev3.getReferences1() ) );
		assertTrue( checkCollection( rev4.getReferences1(), ed1_1_fromRev3, ed1_2 ) );

		assertTrue( checkCollection( rev1.getReferences2(), ed2_2_fromRev1 ) );
		assertTrue( checkCollection( rev2.getReferences2() ) );
		assertTrue( checkCollection( rev3.getReferences2() ) );
		assertTrue( checkCollection( rev4.getReferences2(), ed2_1 ) );
	}

	@Test
	public void testHistoryOfEd1_1() {
		DoubleListJoinColumnBidirectionalRefIngEntity ing1 = getEntityManager().find(
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				ing1_id
		);
		DoubleListJoinColumnBidirectionalRefIngEntity ing2 = getEntityManager().find(
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				ing2_id
		);

		DoubleListJoinColumnBidirectionalRefEdEntity1 rev1 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefEdEntity1.class,
				ed1_1_id,
				1
		);
		DoubleListJoinColumnBidirectionalRefEdEntity1 rev2 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefEdEntity1.class,
				ed1_1_id,
				2
		);
		DoubleListJoinColumnBidirectionalRefEdEntity1 rev3 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefEdEntity1.class,
				ed1_1_id,
				3
		);
		DoubleListJoinColumnBidirectionalRefEdEntity1 rev4 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefEdEntity1.class,
				ed1_1_id,
				4
		);

		assertTrue( rev1.getOwner().equals( ing1 ) );
		assertTrue( rev2.getOwner().equals( ing1 ) );
		assertTrue( rev3.getOwner().equals( ing1 ) );
		assertTrue( rev4.getOwner().equals( ing2 ) );

		assertEquals( rev1.getData(), "ed1_1" );
		assertEquals( rev2.getData(), "ed1_1" );
		assertEquals( rev3.getData(), "ed1_1 bis" );
		assertEquals( rev4.getData(), "ed1_1 bis" );
	}

	@Test
	public void testHistoryOfEd1_2() {
		DoubleListJoinColumnBidirectionalRefIngEntity ing1 = getEntityManager().find(
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				ing1_id
		);
		DoubleListJoinColumnBidirectionalRefIngEntity ing2 = getEntityManager().find(
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				ing2_id
		);

		DoubleListJoinColumnBidirectionalRefEdEntity1 rev1 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefEdEntity1.class,
				ed1_2_id,
				1
		);
		DoubleListJoinColumnBidirectionalRefEdEntity1 rev2 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefEdEntity1.class,
				ed1_2_id,
				2
		);
		DoubleListJoinColumnBidirectionalRefEdEntity1 rev3 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefEdEntity1.class,
				ed1_2_id,
				3
		);
		DoubleListJoinColumnBidirectionalRefEdEntity1 rev4 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefEdEntity1.class,
				ed1_2_id,
				4
		);

		assertTrue( rev1.getOwner().equals( ing2 ) );
		assertTrue( rev2.getOwner().equals( ing1 ) );
		assertTrue( rev3.getOwner().equals( ing1 ) );
		assertTrue( rev4.getOwner().equals( ing2 ) );

		assertEquals( rev1.getData(), "ed1_2" );
		assertEquals( rev2.getData(), "ed1_2" );
		assertEquals( rev3.getData(), "ed1_2" );
		assertEquals( rev4.getData(), "ed1_2" );
	}

	@Test
	public void testHistoryOfEd2_1() {
		DoubleListJoinColumnBidirectionalRefIngEntity ing1 = getEntityManager().find(
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				ing1_id
		);
		DoubleListJoinColumnBidirectionalRefIngEntity ing2 = getEntityManager().find(
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				ing2_id
		);

		DoubleListJoinColumnBidirectionalRefEdEntity2 rev1 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefEdEntity2.class,
				ed2_1_id,
				1
		);
		DoubleListJoinColumnBidirectionalRefEdEntity2 rev2 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefEdEntity2.class,
				ed2_1_id,
				2
		);
		DoubleListJoinColumnBidirectionalRefEdEntity2 rev3 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefEdEntity2.class,
				ed2_1_id,
				3
		);
		DoubleListJoinColumnBidirectionalRefEdEntity2 rev4 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefEdEntity2.class,
				ed2_1_id,
				4
		);

		assertTrue( rev1.getOwner().equals( ing1 ) );
		assertTrue( rev2.getOwner().equals( ing1 ) );
		assertTrue( rev3.getOwner().equals( ing1 ) );
		assertTrue( rev4.getOwner().equals( ing2 ) );

		assertEquals( rev1.getData(), "ed2_1" );
		assertEquals( rev2.getData(), "ed2_1" );
		assertEquals( rev3.getData(), "ed2_1" );
		assertEquals( rev4.getData(), "ed2_1" );
	}

	@Test
	public void testHistoryOfEd2_2() {
		DoubleListJoinColumnBidirectionalRefIngEntity ing1 = getEntityManager().find(
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				ing1_id
		);
		DoubleListJoinColumnBidirectionalRefIngEntity ing2 = getEntityManager().find(
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				ing2_id
		);

		DoubleListJoinColumnBidirectionalRefEdEntity2 rev1 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefEdEntity2.class,
				ed2_2_id,
				1
		);
		DoubleListJoinColumnBidirectionalRefEdEntity2 rev2 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefEdEntity2.class,
				ed2_2_id,
				2
		);
		DoubleListJoinColumnBidirectionalRefEdEntity2 rev3 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefEdEntity2.class,
				ed2_2_id,
				3
		);
		DoubleListJoinColumnBidirectionalRefEdEntity2 rev4 = getAuditReader().find(
				DoubleListJoinColumnBidirectionalRefEdEntity2.class,
				ed2_2_id,
				4
		);

		assertTrue( rev1.getOwner().equals( ing2 ) );
		assertTrue( rev2.getOwner().equals( ing1 ) );
		assertTrue( rev3.getOwner().equals( ing1 ) );
		assertTrue( rev4.getOwner().equals( ing1 ) );

		assertEquals( rev1.getData(), "ed2_2" );
		assertEquals( rev2.getData(), "ed2_2" );
		assertEquals( rev3.getData(), "ed2_2 bis" );
		assertEquals( rev4.getData(), "ed2_2 bis" );
	}
}
