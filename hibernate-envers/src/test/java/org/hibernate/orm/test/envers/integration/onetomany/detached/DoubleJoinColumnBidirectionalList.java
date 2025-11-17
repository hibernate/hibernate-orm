/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.detached;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.onetomany.detached.DoubleListJoinColumnBidirectionalRefEdEntity1;
import org.hibernate.orm.test.envers.entities.onetomany.detached.DoubleListJoinColumnBidirectionalRefEdEntity2;
import org.hibernate.orm.test.envers.entities.onetomany.detached.DoubleListJoinColumnBidirectionalRefIngEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.envers.tools.TestTools.checkCollection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for a double "fake" bidirectional mapping where one side uses @OneToMany+@JoinColumn
 * (and thus owns the relation), and the other uses a @ManyToOne(insertable=false, updatable=false).
 *
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {
		DoubleListJoinColumnBidirectionalRefIngEntity.class,
		DoubleListJoinColumnBidirectionalRefEdEntity1.class,
		DoubleListJoinColumnBidirectionalRefEdEntity2.class
})
public class DoubleJoinColumnBidirectionalList {
	private Integer ed1_1_id;
	private Integer ed2_1_id;
	private Integer ed1_2_id;
	private Integer ed2_2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@BeforeClassTemplate
	public void createData(EntityManagerFactoryScope scope) {
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
		scope.inTransaction( em -> {
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
		} );

		// Revision 2 (ing1: ed1_1, ed1_2, ed2_1, ed2_2)
		scope.inTransaction( em -> {
			DoubleListJoinColumnBidirectionalRefIngEntity ing1Ref = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
			DoubleListJoinColumnBidirectionalRefIngEntity ing2Ref = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
			DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_2Ref = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2.getId() );
			DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_2Ref = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2.getId() );

			ing2Ref.getReferences1().clear();
			ing2Ref.getReferences2().clear();

			ing1Ref.getReferences1().add( ed1_2Ref );
			ing1Ref.getReferences2().add( ed2_2Ref );
		} );

		// Revision 3 (ing1: ed1_1, ed1_2, ed2_1, ed2_2)
		scope.inTransaction( em -> {
			DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_1Ref = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1.getId() );
			DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_2Ref = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2.getId() );

			ed1_1Ref.setData( "ed1_1 bis" );
			ed2_2Ref.setData( "ed2_2 bis" );
		} );

		// Revision 4 (ing1: ed2_2, ing2: ed2_1, ed1_1, ed1_2)
		scope.inTransaction( em -> {
			DoubleListJoinColumnBidirectionalRefIngEntity ing1Ref = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
			DoubleListJoinColumnBidirectionalRefIngEntity ing2Ref = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
			DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_1Ref = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1.getId() );
			DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_2Ref = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2.getId() );
			DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_1Ref = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1.getId() );

			ing1Ref.getReferences1().clear();
			ing2Ref.getReferences1().add( ed1_1Ref );
			ing2Ref.getReferences1().add( ed1_2Ref );

			ing1Ref.getReferences2().remove( ed2_1Ref );
			ing2Ref.getReferences2().add( ed2_1Ref );
		} );

		ing1_id = ing1.getId();
		ing2_id = ing2.getId();

		ed1_1_id = ed1_1.getId();
		ed1_2_id = ed1_2.getId();
		ed2_1_id = ed2_1.getId();
		ed2_2_id = ed2_2.getId();
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals(
					Arrays.asList( 1, 2, 4 ),
					auditReader.getRevisions( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1_id )
			);
			assertEquals(
					Arrays.asList( 1, 2, 4 ),
					auditReader.getRevisions( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2_id )
			);

			assertEquals(
					Arrays.asList( 1, 3, 4 ),
					auditReader.getRevisions( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1_id )
			);
			assertEquals(
					Arrays.asList( 1, 2, 4 ),
					auditReader.getRevisions( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2_id )
			);

			assertEquals(
					Arrays.asList( 1, 4 ),
					auditReader.getRevisions( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1_id )
			);
			assertEquals(
					Arrays.asList( 1, 2, 3 ),
					auditReader.getRevisions( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2_id )
			);
		} );
	}

	@Test
	public void testHistoryOfIng1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

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
			DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_2 = em.find(
					DoubleListJoinColumnBidirectionalRefEdEntity1.class,
					ed1_2_id
			);
			DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_1 = em.find(
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

			DoubleListJoinColumnBidirectionalRefIngEntity rev1 = auditReader.find(
					DoubleListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id,
					1
			);
			DoubleListJoinColumnBidirectionalRefIngEntity rev2 = auditReader.find(
					DoubleListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id,
					2
			);
			DoubleListJoinColumnBidirectionalRefIngEntity rev3 = auditReader.find(
					DoubleListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id,
					3
			);
			DoubleListJoinColumnBidirectionalRefIngEntity rev4 = auditReader.find(
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
		} );
	}

	@Test
	public void testHistoryOfIng2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_1_fromRev3 = new DoubleListJoinColumnBidirectionalRefEdEntity1(
					ed1_1_id,
					"ed1_1 bis",
					null
			);
			DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_2 = em.find(
					DoubleListJoinColumnBidirectionalRefEdEntity1.class,
					ed1_2_id
			);
			DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_1 = em.find(
					DoubleListJoinColumnBidirectionalRefEdEntity2.class,
					ed2_1_id
			);
			DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_2_fromRev1 = new DoubleListJoinColumnBidirectionalRefEdEntity2(
					ed2_2_id,
					"ed2_2",
					null
			);

			DoubleListJoinColumnBidirectionalRefIngEntity rev1 = auditReader.find(
					DoubleListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id,
					1
			);
			DoubleListJoinColumnBidirectionalRefIngEntity rev2 = auditReader.find(
					DoubleListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id,
					2
			);
			DoubleListJoinColumnBidirectionalRefIngEntity rev3 = auditReader.find(
					DoubleListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id,
					3
			);
			DoubleListJoinColumnBidirectionalRefIngEntity rev4 = auditReader.find(
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
		} );
	}

	@Test
	public void testHistoryOfEd1_1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			DoubleListJoinColumnBidirectionalRefIngEntity ing1 = em.find(
					DoubleListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id
			);
			DoubleListJoinColumnBidirectionalRefIngEntity ing2 = em.find(
					DoubleListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id
			);

			DoubleListJoinColumnBidirectionalRefEdEntity1 rev1 = auditReader.find(
					DoubleListJoinColumnBidirectionalRefEdEntity1.class,
					ed1_1_id,
					1
			);
			DoubleListJoinColumnBidirectionalRefEdEntity1 rev2 = auditReader.find(
					DoubleListJoinColumnBidirectionalRefEdEntity1.class,
					ed1_1_id,
					2
			);
			DoubleListJoinColumnBidirectionalRefEdEntity1 rev3 = auditReader.find(
					DoubleListJoinColumnBidirectionalRefEdEntity1.class,
					ed1_1_id,
					3
			);
			DoubleListJoinColumnBidirectionalRefEdEntity1 rev4 = auditReader.find(
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
		} );
	}

	@Test
	public void testHistoryOfEd1_2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			DoubleListJoinColumnBidirectionalRefIngEntity ing1 = em.find(
					DoubleListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id
			);
			DoubleListJoinColumnBidirectionalRefIngEntity ing2 = em.find(
					DoubleListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id
			);

			DoubleListJoinColumnBidirectionalRefEdEntity1 rev1 = auditReader.find(
					DoubleListJoinColumnBidirectionalRefEdEntity1.class,
					ed1_2_id,
					1
			);
			DoubleListJoinColumnBidirectionalRefEdEntity1 rev2 = auditReader.find(
					DoubleListJoinColumnBidirectionalRefEdEntity1.class,
					ed1_2_id,
					2
			);
			DoubleListJoinColumnBidirectionalRefEdEntity1 rev3 = auditReader.find(
					DoubleListJoinColumnBidirectionalRefEdEntity1.class,
					ed1_2_id,
					3
			);
			DoubleListJoinColumnBidirectionalRefEdEntity1 rev4 = auditReader.find(
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
		} );
	}

	@Test
	public void testHistoryOfEd2_1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			DoubleListJoinColumnBidirectionalRefIngEntity ing1 = em.find(
					DoubleListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id
			);
			DoubleListJoinColumnBidirectionalRefIngEntity ing2 = em.find(
					DoubleListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id
			);

			DoubleListJoinColumnBidirectionalRefEdEntity2 rev1 = auditReader.find(
					DoubleListJoinColumnBidirectionalRefEdEntity2.class,
					ed2_1_id,
					1
			);
			DoubleListJoinColumnBidirectionalRefEdEntity2 rev2 = auditReader.find(
					DoubleListJoinColumnBidirectionalRefEdEntity2.class,
					ed2_1_id,
					2
			);
			DoubleListJoinColumnBidirectionalRefEdEntity2 rev3 = auditReader.find(
					DoubleListJoinColumnBidirectionalRefEdEntity2.class,
					ed2_1_id,
					3
			);
			DoubleListJoinColumnBidirectionalRefEdEntity2 rev4 = auditReader.find(
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
		} );
	}

	@Test
	public void testHistoryOfEd2_2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			DoubleListJoinColumnBidirectionalRefIngEntity ing1 = em.find(
					DoubleListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id
			);
			DoubleListJoinColumnBidirectionalRefIngEntity ing2 = em.find(
					DoubleListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id
			);

			DoubleListJoinColumnBidirectionalRefEdEntity2 rev1 = auditReader.find(
					DoubleListJoinColumnBidirectionalRefEdEntity2.class,
					ed2_2_id,
					1
			);
			DoubleListJoinColumnBidirectionalRefEdEntity2 rev2 = auditReader.find(
					DoubleListJoinColumnBidirectionalRefEdEntity2.class,
					ed2_2_id,
					2
			);
			DoubleListJoinColumnBidirectionalRefEdEntity2 rev3 = auditReader.find(
					DoubleListJoinColumnBidirectionalRefEdEntity2.class,
					ed2_2_id,
					3
			);
			DoubleListJoinColumnBidirectionalRefEdEntity2 rev4 = auditReader.find(
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
		} );
	}
}
