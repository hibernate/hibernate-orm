/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.detached;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.onetomany.detached.ListJoinColumnBidirectionalRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.detached.ListJoinColumnBidirectionalRefIngEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.envers.tools.TestTools.checkCollection;
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
		ListJoinColumnBidirectionalRefIngEntity.class,
		ListJoinColumnBidirectionalRefEdEntity.class
})
public class JoinColumnBidirectionalList {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@BeforeClassTemplate
	public void createData(EntityManagerFactoryScope scope) {
		// Revision 1 (ing1: ed1, ing2: ed2)
		scope.inTransaction( em -> {
			ListJoinColumnBidirectionalRefEdEntity ed1 = new ListJoinColumnBidirectionalRefEdEntity( "ed1", null );
			ListJoinColumnBidirectionalRefEdEntity ed2 = new ListJoinColumnBidirectionalRefEdEntity( "ed2", null );

			ListJoinColumnBidirectionalRefIngEntity ing1 = new ListJoinColumnBidirectionalRefIngEntity( "coll1", ed1 );
			ListJoinColumnBidirectionalRefIngEntity ing2 = new ListJoinColumnBidirectionalRefIngEntity( "coll1", ed2 );

			em.persist( ed1 );
			em.persist( ed2 );
			em.persist( ing1 );
			em.persist( ing2 );

			ed1_id = ed1.getId();
			ed2_id = ed2.getId();
			ing1_id = ing1.getId();
			ing2_id = ing2.getId();
		} );

		// Revision 2 (ing1: ed1, ed2)
		scope.inTransaction( em -> {
			ListJoinColumnBidirectionalRefIngEntity ing1 = em.find( ListJoinColumnBidirectionalRefIngEntity.class, ing1_id );
			ListJoinColumnBidirectionalRefIngEntity ing2 = em.find( ListJoinColumnBidirectionalRefIngEntity.class, ing2_id );
			ListJoinColumnBidirectionalRefEdEntity ed2 = em.find( ListJoinColumnBidirectionalRefEdEntity.class, ed2_id );

			ing2.getReferences().remove( ed2 );
			ing1.getReferences().add( ed2 );
		} );

		// No revision - no changes
		scope.inTransaction( em -> {
			ListJoinColumnBidirectionalRefEdEntity ed2 = em.find( ListJoinColumnBidirectionalRefEdEntity.class, ed2_id );
			ListJoinColumnBidirectionalRefIngEntity ing2 = em.find( ListJoinColumnBidirectionalRefIngEntity.class, ing2_id );

			ed2.setOwner( ing2 );
		} );

		// Revision 3 (ing1: ed1, ed2)
		scope.inTransaction( em -> {
			ListJoinColumnBidirectionalRefEdEntity ed1 = em.find( ListJoinColumnBidirectionalRefEdEntity.class, ed1_id );
			ListJoinColumnBidirectionalRefIngEntity ing2 = em.find( ListJoinColumnBidirectionalRefIngEntity.class, ing2_id );

			ed1.setData( "ed1 bis" );
			// Shouldn't get written
			ed1.setOwner( ing2 );
		} );

		// Revision 4 (ing2: ed1, ed2)
		scope.inTransaction( em -> {
			ListJoinColumnBidirectionalRefIngEntity ing1 = em.find( ListJoinColumnBidirectionalRefIngEntity.class, ing1_id );
			ListJoinColumnBidirectionalRefIngEntity ing2 = em.find( ListJoinColumnBidirectionalRefIngEntity.class, ing2_id );
			ListJoinColumnBidirectionalRefEdEntity ed1 = em.find( ListJoinColumnBidirectionalRefEdEntity.class, ed1_id );
			ListJoinColumnBidirectionalRefEdEntity ed2 = em.find( ListJoinColumnBidirectionalRefEdEntity.class, ed2_id );

			ing1.getReferences().clear();
			ing2.getReferences().add( ed1 );
			ing2.getReferences().add( ed2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals(
					Arrays.asList( 1, 2, 4 ),
					auditReader.getRevisions( ListJoinColumnBidirectionalRefIngEntity.class, ing1_id )
			);
			assertEquals(
					Arrays.asList( 1, 2, 4 ),
					auditReader.getRevisions( ListJoinColumnBidirectionalRefIngEntity.class, ing2_id )
			);

			assertEquals(
					Arrays.asList( 1, 3, 4 ),
					auditReader.getRevisions( ListJoinColumnBidirectionalRefEdEntity.class, ed1_id )
			);
			assertEquals(
					Arrays.asList( 1, 2, 4 ),
					auditReader.getRevisions( ListJoinColumnBidirectionalRefEdEntity.class, ed2_id )
			);
		} );
	}

	@Test
	public void testHistoryOfIng1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			ListJoinColumnBidirectionalRefEdEntity ed1_fromRev1 = new ListJoinColumnBidirectionalRefEdEntity(
					ed1_id,
					"ed1",
					null
			);
			ListJoinColumnBidirectionalRefEdEntity ed1_fromRev3 = new ListJoinColumnBidirectionalRefEdEntity(
					ed1_id,
					"ed1 bis",
					null
			);
			ListJoinColumnBidirectionalRefEdEntity ed2 = em.find(
					ListJoinColumnBidirectionalRefEdEntity.class,
					ed2_id
			);

			final var auditReader = AuditReaderFactory.get( em );
			ListJoinColumnBidirectionalRefIngEntity rev1 = auditReader.find(
					ListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id,
					1
			);
			ListJoinColumnBidirectionalRefIngEntity rev2 = auditReader.find(
					ListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id,
					2
			);
			ListJoinColumnBidirectionalRefIngEntity rev3 = auditReader.find(
					ListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id,
					3
			);
			ListJoinColumnBidirectionalRefIngEntity rev4 = auditReader.find(
					ListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id,
					4
			);

			assertTrue( checkCollection( rev1.getReferences(), ed1_fromRev1 ) );
			assertTrue( checkCollection( rev2.getReferences(), ed1_fromRev1, ed2 ) );
			assertTrue( checkCollection( rev3.getReferences(), ed1_fromRev3, ed2 ) );
			assertTrue( checkCollection( rev4.getReferences() ) );
		} );
	}

	@Test
	public void testHistoryOfIng2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			ListJoinColumnBidirectionalRefEdEntity ed1 = em.find(
					ListJoinColumnBidirectionalRefEdEntity.class,
					ed1_id
			);
			ListJoinColumnBidirectionalRefEdEntity ed2 = em.find(
					ListJoinColumnBidirectionalRefEdEntity.class,
					ed2_id
			);

			final var auditReader = AuditReaderFactory.get( em );
			ListJoinColumnBidirectionalRefIngEntity rev1 = auditReader.find(
					ListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id,
					1
			);
			ListJoinColumnBidirectionalRefIngEntity rev2 = auditReader.find(
					ListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id,
					2
			);
			ListJoinColumnBidirectionalRefIngEntity rev3 = auditReader.find(
					ListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id,
					3
			);
			ListJoinColumnBidirectionalRefIngEntity rev4 = auditReader.find(
					ListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id,
					4
			);

			assertTrue( checkCollection( rev1.getReferences(), ed2 ) );
			assertTrue( checkCollection( rev2.getReferences() ) );
			assertTrue( checkCollection( rev3.getReferences() ) );
			assertTrue( checkCollection( rev4.getReferences(), ed1, ed2 ) );
		} );
	}

	@Test
	public void testHistoryOfEd1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			ListJoinColumnBidirectionalRefIngEntity ing1 = em.find(
					ListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id
			);
			ListJoinColumnBidirectionalRefIngEntity ing2 = em.find(
					ListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id
			);

			final var auditReader = AuditReaderFactory.get( em );
			ListJoinColumnBidirectionalRefEdEntity rev1 = auditReader.find(
					ListJoinColumnBidirectionalRefEdEntity.class,
					ed1_id,
					1
			);
			ListJoinColumnBidirectionalRefEdEntity rev2 = auditReader.find(
					ListJoinColumnBidirectionalRefEdEntity.class,
					ed1_id,
					2
			);
			ListJoinColumnBidirectionalRefEdEntity rev3 = auditReader.find(
					ListJoinColumnBidirectionalRefEdEntity.class,
					ed1_id,
					3
			);
			ListJoinColumnBidirectionalRefEdEntity rev4 = auditReader.find(
					ListJoinColumnBidirectionalRefEdEntity.class,
					ed1_id,
					4
			);

			assertTrue( rev1.getOwner().equals( ing1 ) );
			assertTrue( rev2.getOwner().equals( ing1 ) );
			assertTrue( rev3.getOwner().equals( ing1 ) );
			assertTrue( rev4.getOwner().equals( ing2 ) );

			assertEquals( rev1.getData(), "ed1" );
			assertEquals( rev2.getData(), "ed1" );
			assertEquals( rev3.getData(), "ed1 bis" );
			assertEquals( rev4.getData(), "ed1 bis" );
		} );
	}

	@Test
	public void testHistoryOfEd2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			ListJoinColumnBidirectionalRefIngEntity ing1 = em.find(
					ListJoinColumnBidirectionalRefIngEntity.class,
					ing1_id
			);
			ListJoinColumnBidirectionalRefIngEntity ing2 = em.find(
					ListJoinColumnBidirectionalRefIngEntity.class,
					ing2_id
			);

			final var auditReader = AuditReaderFactory.get( em );
			ListJoinColumnBidirectionalRefEdEntity rev1 = auditReader.find(
					ListJoinColumnBidirectionalRefEdEntity.class,
					ed2_id,
					1
			);
			ListJoinColumnBidirectionalRefEdEntity rev2 = auditReader.find(
					ListJoinColumnBidirectionalRefEdEntity.class,
					ed2_id,
					2
			);
			ListJoinColumnBidirectionalRefEdEntity rev3 = auditReader.find(
					ListJoinColumnBidirectionalRefEdEntity.class,
					ed2_id,
					3
			);
			ListJoinColumnBidirectionalRefEdEntity rev4 = auditReader.find(
					ListJoinColumnBidirectionalRefEdEntity.class,
					ed2_id,
					4
			);

			assertTrue( rev1.getOwner().equals( ing2 ) );
			assertTrue( rev2.getOwner().equals( ing1 ) );
			assertTrue( rev3.getOwner().equals( ing1 ) );
			assertTrue( rev4.getOwner().equals( ing2 ) );

			assertEquals( rev1.getData(), "ed2" );
			assertEquals( rev2.getData(), "ed2" );
			assertEquals( rev3.getData(), "ed2" );
			assertEquals( rev4.getData(), "ed2" );
		} );
	}
}
