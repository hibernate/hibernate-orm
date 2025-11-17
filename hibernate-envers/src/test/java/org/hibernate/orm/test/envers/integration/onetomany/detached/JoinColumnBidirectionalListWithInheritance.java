/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.detached;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.onetomany.detached.ListJoinColumnBidirectionalInheritanceRefEdChildEntity;
import org.hibernate.orm.test.envers.entities.onetomany.detached.ListJoinColumnBidirectionalInheritanceRefEdParentEntity;
import org.hibernate.orm.test.envers.entities.onetomany.detached.ListJoinColumnBidirectionalInheritanceRefIngEntity;
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
		ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
		ListJoinColumnBidirectionalInheritanceRefEdChildEntity.class,
		ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class
})
public class JoinColumnBidirectionalListWithInheritance {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@BeforeClassTemplate
	public void createData(EntityManagerFactoryScope scope) {
		// Revision 1 (ing1: ed1, ing2: ed2)
		scope.inTransaction( em -> {
			ListJoinColumnBidirectionalInheritanceRefEdParentEntity ed1 = new ListJoinColumnBidirectionalInheritanceRefEdChildEntity(
					"ed1",
					null,
					"ed1 child"
			);
			ListJoinColumnBidirectionalInheritanceRefEdParentEntity ed2 = new ListJoinColumnBidirectionalInheritanceRefEdChildEntity(
					"ed2",
					null,
					"ed2 child"
			);

			ListJoinColumnBidirectionalInheritanceRefIngEntity ing1 = new ListJoinColumnBidirectionalInheritanceRefIngEntity(
					"coll1",
					ed1
			);
			ListJoinColumnBidirectionalInheritanceRefIngEntity ing2 = new ListJoinColumnBidirectionalInheritanceRefIngEntity(
					"coll1",
					ed2
			);

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
			ListJoinColumnBidirectionalInheritanceRefIngEntity ing1 = em.find( ListJoinColumnBidirectionalInheritanceRefIngEntity.class, ing1_id );
			ListJoinColumnBidirectionalInheritanceRefIngEntity ing2 = em.find( ListJoinColumnBidirectionalInheritanceRefIngEntity.class, ing2_id );
			ListJoinColumnBidirectionalInheritanceRefEdParentEntity ed2 = em.find( ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class, ed2_id );

			ing2.getReferences().remove( ed2 );
			ing1.getReferences().add( ed2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals(
					Arrays.asList( 1, 2 ),
					auditReader.getRevisions( ListJoinColumnBidirectionalInheritanceRefIngEntity.class, ing1_id )
			);
			assertEquals(
					Arrays.asList( 1, 2 ),
					auditReader.getRevisions( ListJoinColumnBidirectionalInheritanceRefIngEntity.class, ing2_id )
			);

			assertEquals(
					Arrays.asList( 1 ),
					auditReader.getRevisions( ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class, ed1_id )
			);
			assertEquals(
					Arrays.asList( 1, 2 ),
					auditReader.getRevisions( ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class, ed2_id )
			);
		} );
	}

	@Test
	public void testHistoryOfIng1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			ListJoinColumnBidirectionalInheritanceRefEdParentEntity ed1 = em.find(
					ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
					ed1_id
			);
			ListJoinColumnBidirectionalInheritanceRefEdParentEntity ed2 = em.find(
					ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
					ed2_id
			);

			final var auditReader = AuditReaderFactory.get( em );
			ListJoinColumnBidirectionalInheritanceRefIngEntity rev1 = auditReader.find(
					ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
					ing1_id,
					1
			);
			ListJoinColumnBidirectionalInheritanceRefIngEntity rev2 = auditReader.find(
					ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
					ing1_id,
					2
			);

			assertTrue( checkCollection( rev1.getReferences(), ed1 ) );
			assertTrue( checkCollection( rev2.getReferences(), ed1, ed2 ) );
		} );
	}

	@Test
	public void testHistoryOfIng2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			ListJoinColumnBidirectionalInheritanceRefEdParentEntity ed2 = em.find(
					ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
					ed2_id
			);

			final var auditReader = AuditReaderFactory.get( em );
			ListJoinColumnBidirectionalInheritanceRefIngEntity rev1 = auditReader.find(
					ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
					ing2_id,
					1
			);
			ListJoinColumnBidirectionalInheritanceRefIngEntity rev2 = auditReader.find(
					ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
					ing2_id,
					2
			);

			assertTrue( checkCollection( rev1.getReferences(), ed2 ) );
			assertTrue( checkCollection( rev2.getReferences() ) );
		} );
	}

	@Test
	public void testHistoryOfEd1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			ListJoinColumnBidirectionalInheritanceRefIngEntity ing1 = em.find(
					ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
					ing1_id
			);

			final var auditReader = AuditReaderFactory.get( em );
			ListJoinColumnBidirectionalInheritanceRefEdParentEntity rev1 = auditReader.find(
					ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
					ed1_id,
					1
			);
			ListJoinColumnBidirectionalInheritanceRefEdParentEntity rev2 = auditReader.find(
					ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
					ed1_id,
					2
			);

			assertTrue( rev1.getOwner().equals( ing1 ) );
			assertTrue( rev2.getOwner().equals( ing1 ) );
		} );
	}

	@Test
	public void testHistoryOfEd2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			ListJoinColumnBidirectionalInheritanceRefIngEntity ing1 = em.find(
					ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
					ing1_id
			);
			ListJoinColumnBidirectionalInheritanceRefIngEntity ing2 = em.find(
					ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
					ing2_id
			);

			final var auditReader = AuditReaderFactory.get( em );
			ListJoinColumnBidirectionalInheritanceRefEdParentEntity rev1 = auditReader.find(
					ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
					ed2_id,
					1
			);
			ListJoinColumnBidirectionalInheritanceRefEdParentEntity rev2 = auditReader.find(
					ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
					ed2_id,
					2
			);

			assertTrue( rev1.getOwner().equals( ing2 ) );
			assertTrue( rev2.getOwner().equals( ing1 ) );
		} );
	}
}
