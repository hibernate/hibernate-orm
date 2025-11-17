/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.onetomany.CollectionRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.CollectionRefIngEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {CollectionRefEdEntity.class, CollectionRefIngEntity.class})
public class BasicCollection {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// Revision 1
			CollectionRefEdEntity ed1 = new CollectionRefEdEntity( 1, "data_ed_1" );
			CollectionRefEdEntity ed2 = new CollectionRefEdEntity( 2, "data_ed_2" );

			CollectionRefIngEntity ing1 = new CollectionRefIngEntity( 3, "data_ing_1", ed1 );
			CollectionRefIngEntity ing2 = new CollectionRefIngEntity( 4, "data_ing_2", ed1 );

			em.persist( ed1 );
			em.persist( ed2 );

			em.persist( ing1 );
			em.persist( ing2 );

			ed1_id = ed1.getId();
			ed2_id = ed2.getId();

			ing1_id = ing1.getId();
			ing2_id = ing2.getId();
		} );

		scope.inTransaction( em -> {
			// Revision 2
			CollectionRefIngEntity ing1 = em.find( CollectionRefIngEntity.class, ing1_id );
			CollectionRefEdEntity ed2 = em.find( CollectionRefEdEntity.class, ed2_id );

			ing1.setReference( ed2 );
		} );

		scope.inTransaction( em -> {
			// Revision 3
			CollectionRefIngEntity ing2 = em.find( CollectionRefIngEntity.class, ing2_id );
			CollectionRefEdEntity ed2 = em.find( CollectionRefEdEntity.class, ed2_id );

			ing2.setReference( ed2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( CollectionRefEdEntity.class, ed1_id ) );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( CollectionRefEdEntity.class, ed2_id ) );

			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( CollectionRefIngEntity.class, ing1_id ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( CollectionRefIngEntity.class, ing2_id ) );
		} );
	}

	private <T> Set<T> makeSet(T... objects) {
		Set<T> ret = new HashSet<T>();
		//noinspection ManualArrayToCollectionCopy
		for ( T obj : objects ) {
			ret.add( obj );
		}
		return ret;
	}

	@Test
	public void testHistoryOfEdId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			CollectionRefIngEntity ing1 = em.find( CollectionRefIngEntity.class, ing1_id );
			CollectionRefIngEntity ing2 = em.find( CollectionRefIngEntity.class, ing2_id );

			CollectionRefEdEntity rev1 = auditReader.find( CollectionRefEdEntity.class, ed1_id, 1 );
			CollectionRefEdEntity rev2 = auditReader.find( CollectionRefEdEntity.class, ed1_id, 2 );
			CollectionRefEdEntity rev3 = auditReader.find( CollectionRefEdEntity.class, ed1_id, 3 );

			assertTrue( rev1.getReffering().containsAll( makeSet( ing1, ing2 ) ) );
			assertEquals( 2, rev1.getReffering().size() );

			assertTrue( rev2.getReffering().containsAll( makeSet( ing2 ) ) );
			assertEquals( 1, rev2.getReffering().size() );

			assertTrue( rev3.getReffering().containsAll( Collections.EMPTY_SET ) );
			assertEquals( 0, rev3.getReffering().size() );
		} );
	}

	@Test
	public void testHistoryOfEdId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			CollectionRefIngEntity ing1 = em.find( CollectionRefIngEntity.class, ing1_id );
			CollectionRefIngEntity ing2 = em.find( CollectionRefIngEntity.class, ing2_id );

			CollectionRefEdEntity rev1 = auditReader.find( CollectionRefEdEntity.class, ed2_id, 1 );
			CollectionRefEdEntity rev2 = auditReader.find( CollectionRefEdEntity.class, ed2_id, 2 );
			CollectionRefEdEntity rev3 = auditReader.find( CollectionRefEdEntity.class, ed2_id, 3 );

			assertTrue( rev1.getReffering().containsAll( Collections.EMPTY_SET ) );
			assertEquals( 0, rev1.getReffering().size() );

			assertTrue( rev2.getReffering().containsAll( makeSet( ing1 ) ) );
			assertEquals( 1, rev2.getReffering().size() );

			assertTrue( rev3.getReffering().containsAll( makeSet( ing1, ing2 ) ) );
			assertEquals( 2, rev3.getReffering().size() );
		} );
	}

	@Test
	public void testHistoryOfEdIng1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			CollectionRefEdEntity ed1 = em.find( CollectionRefEdEntity.class, ed1_id );
			CollectionRefEdEntity ed2 = em.find( CollectionRefEdEntity.class, ed2_id );

			CollectionRefIngEntity rev1 = auditReader.find( CollectionRefIngEntity.class, ing1_id, 1 );
			CollectionRefIngEntity rev2 = auditReader.find( CollectionRefIngEntity.class, ing1_id, 2 );
			CollectionRefIngEntity rev3 = auditReader.find( CollectionRefIngEntity.class, ing1_id, 3 );

			assertEquals( ed1, rev1.getReference() );
			assertEquals( ed2, rev2.getReference() );
			assertEquals( ed2, rev3.getReference() );
		} );
	}

	@Test
	public void testHistoryOfEdIng2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			CollectionRefEdEntity ed1 = em.find( CollectionRefEdEntity.class, ed1_id );
			CollectionRefEdEntity ed2 = em.find( CollectionRefEdEntity.class, ed2_id );

			CollectionRefIngEntity rev1 = auditReader.find( CollectionRefIngEntity.class, ing2_id, 1 );
			CollectionRefIngEntity rev2 = auditReader.find( CollectionRefIngEntity.class, ing2_id, 2 );
			CollectionRefIngEntity rev3 = auditReader.find( CollectionRefIngEntity.class, ing2_id, 3 );

			assertEquals( ed1, rev1.getReference() );
			assertEquals( ed1, rev2.getReference() );
			assertEquals( ed2, rev3.getReference() );
		} );
	}
}
