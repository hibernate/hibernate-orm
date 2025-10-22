/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.cache;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.onetomany.SetRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.SetRefIngEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"ObjectEquality"})
@EnversTest
@Jpa(annotatedClasses = {SetRefEdEntity.class, SetRefIngEntity.class})
public class OneToManyCache {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );
		SetRefEdEntity ed2 = new SetRefEdEntity( 2, "data_ed_2" );

		SetRefIngEntity ing1 = new SetRefIngEntity( 1, "data_ing_1" );
		SetRefIngEntity ing2 = new SetRefIngEntity( 2, "data_ing_2" );

		// Revision 1
		scope.inTransaction( em -> {
			em.persist( ed1 );
			em.persist( ed2 );

			ing1.setReference( ed1 );
			ing2.setReference( ed1 );

			em.persist( ing1 );
			em.persist( ing2 );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			SetRefIngEntity ing1Ref = em.find( SetRefIngEntity.class, ing1.getId() );
			SetRefIngEntity ing2Ref = em.find( SetRefIngEntity.class, ing2.getId() );
			SetRefEdEntity ed2Ref = em.find( SetRefEdEntity.class, ed2.getId() );

			ing1Ref.setReference( ed2Ref );
			ing2Ref.setReference( ed2Ref );
		} );

		//

		ed1_id = ed1.getId();
		ed2_id = ed2.getId();

		ing1_id = ing1.getId();
		ing2_id = ing2.getId();
	}

	@Test
	public void testCacheReferenceAccessAfterFind(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefEdEntity ed1_rev1 = auditReader.find( SetRefEdEntity.class, ed1_id, 1 );

			SetRefIngEntity ing1_rev1 = auditReader.find( SetRefIngEntity.class, ing1_id, 1 );
			SetRefIngEntity ing2_rev1 = auditReader.find( SetRefIngEntity.class, ing2_id, 1 );

			// It should be exactly the same object
			assertSame( ed1_rev1, ing1_rev1.getReference() );
			assertSame( ed1_rev1, ing2_rev1.getReference() );
		} );
	}

	@Test
	public void testCacheReferenceAccessAfterCollectionAccessRev1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefEdEntity ed1_rev1 = auditReader.find( SetRefEdEntity.class, ed1_id, 1 );

			// It should be exactly the same object
			assertEquals( 2, ed1_rev1.getReffering().size() );
			for ( SetRefIngEntity setRefIngEntity : ed1_rev1.getReffering() ) {
				assertSame( ed1_rev1, setRefIngEntity.getReference() );
			}
		} );
	}

	@Test
	public void testCacheReferenceAccessAfterCollectionAccessRev2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefEdEntity ed2_rev2 = auditReader.find( SetRefEdEntity.class, ed2_id, 2 );

			assertEquals( 2, ed2_rev2.getReffering().size() );
			for ( SetRefIngEntity setRefIngEntity : ed2_rev2.getReffering() ) {
				assertSame( ed2_rev2, setRefIngEntity.getReference() );
			}
		} );
	}

	@Test
	public void testCacheFindAfterCollectionAccessRev1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefEdEntity ed1_rev1 = auditReader.find( SetRefEdEntity.class, ed1_id, 1 );

			// Reading the collection
			assertEquals( 2, ed1_rev1.getReffering().size() );

			SetRefIngEntity ing1_rev1 = auditReader.find( SetRefIngEntity.class, ing1_id, 1 );
			SetRefIngEntity ing2_rev1 = auditReader.find( SetRefIngEntity.class, ing2_id, 1 );

			for ( SetRefIngEntity setRefIngEntity : ed1_rev1.getReffering() ) {
				assertSame( true, setRefIngEntity == ing1_rev1 || setRefIngEntity == ing2_rev1 );
			}
		} );
	}
}
