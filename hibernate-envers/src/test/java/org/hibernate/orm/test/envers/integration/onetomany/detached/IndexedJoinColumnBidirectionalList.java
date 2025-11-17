/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.detached;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.onetomany.detached.IndexedListJoinColumnBidirectionalRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.detached.IndexedListJoinColumnBidirectionalRefIngEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for a "fake" bidirectional mapping where one side uses @OneToMany+@JoinColumn (and thus owns the relatin),
 * and the other uses a @ManyToOne(insertable=false, updatable=false).
 *
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {
		IndexedListJoinColumnBidirectionalRefIngEntity.class,
		IndexedListJoinColumnBidirectionalRefEdEntity.class
})
public class IndexedJoinColumnBidirectionalList {
	private Integer ed1_id;
	private Integer ed2_id;
	private Integer ed3_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@BeforeClassTemplate
	public void createData(EntityManagerFactoryScope scope) {
		IndexedListJoinColumnBidirectionalRefEdEntity ed1 = new IndexedListJoinColumnBidirectionalRefEdEntity(
				"ed1",
				null
		);
		IndexedListJoinColumnBidirectionalRefEdEntity ed2 = new IndexedListJoinColumnBidirectionalRefEdEntity(
				"ed2",
				null
		);
		IndexedListJoinColumnBidirectionalRefEdEntity ed3 = new IndexedListJoinColumnBidirectionalRefEdEntity(
				"ed3",
				null
		);

		IndexedListJoinColumnBidirectionalRefIngEntity ing1 = new IndexedListJoinColumnBidirectionalRefIngEntity(
				"coll1",
				ed1,
				ed2,
				ed3
		);
		IndexedListJoinColumnBidirectionalRefIngEntity ing2 = new IndexedListJoinColumnBidirectionalRefIngEntity(
				"coll1"
		);

		// Revision 1 (ing1: ed1, ed2, ed3)
		scope.inTransaction( em -> {
			em.persist( ed1 );
			em.persist( ed2 );
			em.persist( ed3 );
			em.persist( ing1 );
			em.persist( ing2 );
		} );

		// Revision 2 (ing1: ed1, ed3, ing2: ed2)
		scope.inTransaction( em -> {
			IndexedListJoinColumnBidirectionalRefIngEntity ing1Ref = em.find( IndexedListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
			IndexedListJoinColumnBidirectionalRefIngEntity ing2Ref = em.find( IndexedListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
			IndexedListJoinColumnBidirectionalRefEdEntity ed2Ref = em.find( IndexedListJoinColumnBidirectionalRefEdEntity.class, ed2.getId() );

			ing1Ref.getReferences().remove( ed2Ref );
			ing2Ref.getReferences().add( ed2Ref );
		} );

		// Revision 3 (ing1: ed3, ed1, ing2: ed2)
		scope.inTransaction( em -> {
			IndexedListJoinColumnBidirectionalRefIngEntity ing1Ref = em.find( IndexedListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
			IndexedListJoinColumnBidirectionalRefEdEntity ed3Ref = em.find( IndexedListJoinColumnBidirectionalRefEdEntity.class, ed3.getId() );

			ing1Ref.getReferences().remove( ed3Ref );
			ing1Ref.getReferences().add( 0, ed3Ref );
		} );

		// Revision 4 (ing1: ed2, ed3, ed1)
		scope.inTransaction( em -> {
			IndexedListJoinColumnBidirectionalRefIngEntity ing1Ref = em.find( IndexedListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
			IndexedListJoinColumnBidirectionalRefIngEntity ing2Ref = em.find( IndexedListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
			IndexedListJoinColumnBidirectionalRefEdEntity ed2Ref = em.find( IndexedListJoinColumnBidirectionalRefEdEntity.class, ed2.getId() );

			ing2Ref.getReferences().remove( ed2Ref );
			ing1Ref.getReferences().add( 0, ed2Ref );
		} );

		ing1_id = ing1.getId();
		ing2_id = ing2.getId();

		ed1_id = ed1.getId();
		ed2_id = ed2.getId();
		ed3_id = ed3.getId();
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals(
					Arrays.asList( 1, 2, 3, 4 ),
					auditReader.getRevisions( IndexedListJoinColumnBidirectionalRefIngEntity.class, ing1_id )
			);
			assertEquals(
					Arrays.asList( 1, 2, 4 ),
					auditReader.getRevisions( IndexedListJoinColumnBidirectionalRefIngEntity.class, ing2_id )
			);

			assertEquals(
					Arrays.asList( 1, 3, 4 ),
					auditReader.getRevisions( IndexedListJoinColumnBidirectionalRefEdEntity.class, ed1_id )
			);
			assertEquals(
					Arrays.asList( 1, 2, 4 ),
					auditReader.getRevisions( IndexedListJoinColumnBidirectionalRefEdEntity.class, ed2_id )
			);
			assertEquals(
					Arrays.asList( 1, 2, 3, 4 ),
					auditReader.getRevisions( IndexedListJoinColumnBidirectionalRefEdEntity.class, ed3_id )
			);
		} );
	}

	@Test
	public void testHistoryOfIng1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			IndexedListJoinColumnBidirectionalRefEdEntity ed1 = em.find(
					IndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed1_id
			);
			IndexedListJoinColumnBidirectionalRefEdEntity ed2 = em.find(
					IndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed2_id
			);
			IndexedListJoinColumnBidirectionalRefEdEntity ed3 = em.find(
					IndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed3_id
			);

			IndexedListJoinColumnBidirectionalRefIngEntity rev1 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id,
					1
			);
			IndexedListJoinColumnBidirectionalRefIngEntity rev2 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id,
					2
			);
			IndexedListJoinColumnBidirectionalRefIngEntity rev3 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id,
					3
			);
			IndexedListJoinColumnBidirectionalRefIngEntity rev4 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id,
					4
			);

			assertEquals( rev1.getReferences().size(), 3 );
			assertEquals( rev1.getReferences().get( 0 ), ed1 );
			assertEquals( rev1.getReferences().get( 1 ), ed2 );
			assertEquals( rev1.getReferences().get( 2 ), ed3 );

			assertEquals( rev2.getReferences().size(), 2 );
			assertEquals( rev2.getReferences().get( 0 ), ed1 );
			assertEquals( rev2.getReferences().get( 1 ), ed3 );

			assertEquals( rev3.getReferences().size(), 2 );
			assertEquals( rev3.getReferences().get( 0 ), ed3 );
			assertEquals( rev3.getReferences().get( 1 ), ed1 );

			assertEquals( rev4.getReferences().size(), 3 );
			assertEquals( rev4.getReferences().get( 0 ), ed2 );
			assertEquals( rev4.getReferences().get( 1 ), ed3 );
			assertEquals( rev4.getReferences().get( 2 ), ed1 );
		} );
	}

	@Test
	public void testHistoryOfIng2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			IndexedListJoinColumnBidirectionalRefEdEntity ed2 = em.find(
					IndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed2_id
			);

			IndexedListJoinColumnBidirectionalRefIngEntity rev1 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id,
					1
			);
			IndexedListJoinColumnBidirectionalRefIngEntity rev2 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id,
					2
			);
			IndexedListJoinColumnBidirectionalRefIngEntity rev3 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id,
					3
			);
			IndexedListJoinColumnBidirectionalRefIngEntity rev4 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id,
					4
			);

			assertEquals( rev1.getReferences().size(), 0 );

			assertEquals( rev2.getReferences().size(), 1 );
			assertEquals( rev2.getReferences().get( 0 ), ed2 );

			assertEquals( rev3.getReferences().size(), 1 );
			assertEquals( rev3.getReferences().get( 0 ), ed2 );

			assertEquals( rev4.getReferences().size(), 0 );
		} );
	}

	@Test
	public void testHistoryOfEd1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			IndexedListJoinColumnBidirectionalRefIngEntity ing1 = em.find(
					IndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id
			);

			IndexedListJoinColumnBidirectionalRefEdEntity rev1 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed1_id,
					1
			);
			IndexedListJoinColumnBidirectionalRefEdEntity rev2 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed1_id,
					2
			);
			IndexedListJoinColumnBidirectionalRefEdEntity rev3 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed1_id,
					3
			);
			IndexedListJoinColumnBidirectionalRefEdEntity rev4 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed1_id,
					4
			);

			assertTrue( rev1.getOwner().equals( ing1 ) );
			assertTrue( rev2.getOwner().equals( ing1 ) );
			assertTrue( rev3.getOwner().equals( ing1 ) );
			assertTrue( rev4.getOwner().equals( ing1 ) );

			assertEquals( rev1.getPosition(), Integer.valueOf( 0 ) );
			assertEquals( rev2.getPosition(), Integer.valueOf( 0 ) );
			assertEquals( rev3.getPosition(), Integer.valueOf( 1 ) );
			assertEquals( rev4.getPosition(), Integer.valueOf( 2 ) );
		} );
	}

	@Test
	public void testHistoryOfEd2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			IndexedListJoinColumnBidirectionalRefIngEntity ing1 = em.find(
					IndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id
			);
			IndexedListJoinColumnBidirectionalRefIngEntity ing2 = em.find(
					IndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id
			);

			IndexedListJoinColumnBidirectionalRefEdEntity rev1 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed2_id,
					1
			);
			IndexedListJoinColumnBidirectionalRefEdEntity rev2 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed2_id,
					2
			);
			IndexedListJoinColumnBidirectionalRefEdEntity rev3 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed2_id,
					3
			);
			IndexedListJoinColumnBidirectionalRefEdEntity rev4 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed2_id,
					4
			);

			assertTrue( rev1.getOwner().equals( ing1 ) );
			assertTrue( rev2.getOwner().equals( ing2 ) );
			assertTrue( rev3.getOwner().equals( ing2 ) );
			assertTrue( rev4.getOwner().equals( ing1 ) );

			assertEquals( rev1.getPosition(), Integer.valueOf( 1 ) );
			assertEquals( rev2.getPosition(), Integer.valueOf( 0 ) );
			assertEquals( rev3.getPosition(), Integer.valueOf( 0 ) );
			assertEquals( rev4.getPosition(), Integer.valueOf( 0 ) );
		} );
	}

	@Test
	public void testHistoryOfEd3(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			IndexedListJoinColumnBidirectionalRefIngEntity ing1 = em.find(
					IndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id
			);

			IndexedListJoinColumnBidirectionalRefEdEntity rev1 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed3_id,
					1
			);
			IndexedListJoinColumnBidirectionalRefEdEntity rev2 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed3_id,
					2
			);
			IndexedListJoinColumnBidirectionalRefEdEntity rev3 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed3_id,
					3
			);
			IndexedListJoinColumnBidirectionalRefEdEntity rev4 = auditReader.find(
					IndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed3_id,
					4
			);

			assertTrue( rev1.getOwner().equals( ing1 ) );
			assertTrue( rev2.getOwner().equals( ing1 ) );
			assertTrue( rev3.getOwner().equals( ing1 ) );
			assertTrue( rev4.getOwner().equals( ing1 ) );

			assertEquals( rev1.getPosition(), Integer.valueOf( 2 ) );
			assertEquals( rev2.getPosition(), Integer.valueOf( 1 ) );
			assertEquals( rev3.getPosition(), Integer.valueOf( 0 ) );
			assertEquals( rev4.getPosition(), Integer.valueOf( 1 ) );
		} );
	}
}
