/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.detached;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.onetomany.detached.inheritance.ChildIndexedListJoinColumnBidirectionalRefIngEntity;
import org.hibernate.orm.test.envers.entities.onetomany.detached.inheritance.ParentIndexedListJoinColumnBidirectionalRefIngEntity;
import org.hibernate.orm.test.envers.entities.onetomany.detached.inheritance.ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for a "fake" bidirectional mapping where one side uses @OneToMany+@JoinColumn (and thus owns the relation),
 * in the parent entity, and the other uses a @ManyToOne(insertable=false, updatable=false).
 *
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {
		ParentIndexedListJoinColumnBidirectionalRefIngEntity.class,
		ChildIndexedListJoinColumnBidirectionalRefIngEntity.class,
		ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class
})
public class InheritanceIndexedJoinColumnBidirectionalList {
	private Integer ed1_id;
	private Integer ed2_id;
	private Integer ed3_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@BeforeClassTemplate
	public void createData(EntityManagerFactoryScope scope) {
		// Revision 1 (ing1: ed1, ed2, ed3)
		scope.inTransaction( em -> {
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity ed1 = new ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity(
					"ed1",
					null
			);
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity ed2 = new ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity(
					"ed2",
					null
			);
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity ed3 = new ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity(
					"ed3",
					null
			);

			ChildIndexedListJoinColumnBidirectionalRefIngEntity ing1 = new ChildIndexedListJoinColumnBidirectionalRefIngEntity(
					"coll1",
					"coll1bis",
					ed1,
					ed2,
					ed3
			);
			ChildIndexedListJoinColumnBidirectionalRefIngEntity ing2 = new ChildIndexedListJoinColumnBidirectionalRefIngEntity(
					"coll1",
					"coll1bis"
			);

			em.persist( ed1 );
			em.persist( ed2 );
			em.persist( ed3 );
			em.persist( ing1 );
			em.persist( ing2 );

			ed1_id = ed1.getId();
			ed2_id = ed2.getId();
			ed3_id = ed3.getId();
			ing1_id = ing1.getId();
			ing2_id = ing2.getId();
		} );

		// Revision 2 (ing1: ed1, ed3, ing2: ed2)
		scope.inTransaction( em -> {
			ChildIndexedListJoinColumnBidirectionalRefIngEntity ing1 = em.find( ChildIndexedListJoinColumnBidirectionalRefIngEntity.class, ing1_id );
			ChildIndexedListJoinColumnBidirectionalRefIngEntity ing2 = em.find( ChildIndexedListJoinColumnBidirectionalRefIngEntity.class, ing2_id );
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity ed2 = em.find( ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class, ed2_id );

			ing1.getReferences().remove( ed2 );
			ing2.getReferences().add( ed2 );
		} );

		// Revision 3 (ing1: ed3, ed1, ing2: ed2)
		scope.inTransaction( em -> {
			ChildIndexedListJoinColumnBidirectionalRefIngEntity ing1 = em.find( ChildIndexedListJoinColumnBidirectionalRefIngEntity.class, ing1_id );
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity ed3 = em.find( ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class, ed3_id );

			ing1.getReferences().remove( ed3 );
			ing1.getReferences().add( 0, ed3 );
		} );

		// Revision 4 (ing1: ed2, ed3, ed1)
		scope.inTransaction( em -> {
			ChildIndexedListJoinColumnBidirectionalRefIngEntity ing1 = em.find( ChildIndexedListJoinColumnBidirectionalRefIngEntity.class, ing1_id );
			ChildIndexedListJoinColumnBidirectionalRefIngEntity ing2 = em.find( ChildIndexedListJoinColumnBidirectionalRefIngEntity.class, ing2_id );
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity ed2 = em.find( ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class, ed2_id );

			ing2.getReferences().remove( ed2 );
			ing1.getReferences().add( 0, ed2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals(
					Arrays.asList( 1, 2, 3, 4 ),
					auditReader.getRevisions( ChildIndexedListJoinColumnBidirectionalRefIngEntity.class, ing1_id )
			);
			assertEquals(
					Arrays.asList( 1, 2, 4 ),
					auditReader.getRevisions( ChildIndexedListJoinColumnBidirectionalRefIngEntity.class, ing2_id )
			);

			assertEquals(
					Arrays.asList( 1, 3, 4 ),
					auditReader.getRevisions( ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class, ed1_id )
			);
			assertEquals(
					Arrays.asList( 1, 2, 4 ),
					auditReader.getRevisions( ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class, ed2_id )
			);
			assertEquals(
					Arrays.asList( 1, 2, 3, 4 ),
					auditReader.getRevisions( ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class, ed3_id )
			);
		} );
	}

	@Test
	public void testHistoryOfIng1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity ed1 = em.find(
					ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed1_id
			);
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity ed2 = em.find(
					ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed2_id
			);
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity ed3 = em.find(
					ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed3_id
			);

			final var auditReader = AuditReaderFactory.get( em );
			ChildIndexedListJoinColumnBidirectionalRefIngEntity rev1 = auditReader.find(
					ChildIndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id,
					1
			);
			ChildIndexedListJoinColumnBidirectionalRefIngEntity rev2 = auditReader.find(
					ChildIndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id,
					2
			);
			ChildIndexedListJoinColumnBidirectionalRefIngEntity rev3 = auditReader.find(
					ChildIndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id,
					3
			);
			ChildIndexedListJoinColumnBidirectionalRefIngEntity rev4 = auditReader.find(
					ChildIndexedListJoinColumnBidirectionalRefIngEntity.class,
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
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity ed2 = em.find(
					ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed2_id
			);

			final var auditReader = AuditReaderFactory.get( em );
			ChildIndexedListJoinColumnBidirectionalRefIngEntity rev1 = auditReader.find(
					ChildIndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id,
					1
			);
			ChildIndexedListJoinColumnBidirectionalRefIngEntity rev2 = auditReader.find(
					ChildIndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id,
					2
			);
			ChildIndexedListJoinColumnBidirectionalRefIngEntity rev3 = auditReader.find(
					ChildIndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id,
					3
			);
			ChildIndexedListJoinColumnBidirectionalRefIngEntity rev4 = auditReader.find(
					ChildIndexedListJoinColumnBidirectionalRefIngEntity.class,
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
			ChildIndexedListJoinColumnBidirectionalRefIngEntity ing1 = em.find(
					ChildIndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id
			);

			final var auditReader = AuditReaderFactory.get( em );
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity rev1 = auditReader.find(
					ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed1_id,
					1
			);
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity rev2 = auditReader.find(
					ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed1_id,
					2
			);
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity rev3 = auditReader.find(
					ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed1_id,
					3
			);
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity rev4 = auditReader.find(
					ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class,
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
			ChildIndexedListJoinColumnBidirectionalRefIngEntity ing1 = em.find(
					ChildIndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id
			);
			ChildIndexedListJoinColumnBidirectionalRefIngEntity ing2 = em.find(
					ChildIndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id
			);

			final var auditReader = AuditReaderFactory.get( em );
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity rev1 = auditReader.find(
					ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed2_id,
					1
			);
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity rev2 = auditReader.find(
					ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed2_id,
					2
			);
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity rev3 = auditReader.find(
					ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed2_id,
					3
			);
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity rev4 = auditReader.find(
					ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class,
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
			ChildIndexedListJoinColumnBidirectionalRefIngEntity ing1 = em.find(
					ChildIndexedListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id
			);

			final var auditReader = AuditReaderFactory.get( em );
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity rev1 = auditReader.find(
					ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed3_id,
					1
			);
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity rev2 = auditReader.find(
					ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed3_id,
					2
			);
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity rev3 = auditReader.find(
					ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class,
					ed3_id,
					3
			);
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity rev4 = auditReader.find(
					ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity.class,
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
