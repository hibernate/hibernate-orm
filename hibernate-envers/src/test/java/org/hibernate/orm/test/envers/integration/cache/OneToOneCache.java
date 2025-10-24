/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.cache;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.integration.onetoone.bidirectional.BiRefEdEntity;
import org.hibernate.orm.test.envers.integration.onetoone.bidirectional.BiRefIngEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"ObjectEquality"})
@EnversTest
@Jpa(annotatedClasses = {BiRefEdEntity.class, BiRefIngEntity.class})
public class OneToOneCache {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		BiRefEdEntity ed1 = new BiRefEdEntity( 1, "data_ed_1" );
		BiRefEdEntity ed2 = new BiRefEdEntity( 2, "data_ed_2" );

		BiRefIngEntity ing1 = new BiRefIngEntity( 3, "data_ing_1" );

		// Revision 1
		scope.inTransaction( em -> {
			ing1.setReference( ed1 );

			em.persist( ed1 );
			em.persist( ed2 );

			em.persist( ing1 );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			BiRefIngEntity ing1Ref = em.find( BiRefIngEntity.class, ing1.getId() );
			BiRefEdEntity ed2Ref = em.find( BiRefEdEntity.class, ed2.getId() );

			ing1Ref.setReference( ed2Ref );
		} );

		ed1_id = ed1.getId();
		ed2_id = ed2.getId();

		ing1_id = ing1.getId();
	}

	@Test
	public void testCacheReferenceAccessAfterFindRev1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			BiRefEdEntity ed1_rev1 = auditReader.find( BiRefEdEntity.class, ed1_id, 1 );
			BiRefIngEntity ing1_rev1 = auditReader.find( BiRefIngEntity.class, ing1_id, 1 );

			assertSame( ed1_rev1, ing1_rev1.getReference() );
		} );
	}

	@Test
	public void testCacheReferenceAccessAfterFindRev2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			BiRefEdEntity ed2_rev2 = auditReader.find( BiRefEdEntity.class, ed2_id, 2 );
			BiRefIngEntity ing1_rev2 = auditReader.find( BiRefIngEntity.class, ing1_id, 2 );

			assertSame( ed2_rev2, ing1_rev2.getReference() );
		} );
	}
}
