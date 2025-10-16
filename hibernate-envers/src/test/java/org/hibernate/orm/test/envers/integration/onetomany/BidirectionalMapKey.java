/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {RefIngMapKeyEntity.class, RefEdMapKeyEntity.class})
public class BidirectionalMapKey {
	private Integer ed_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 (intialy 1 relation: ing1 -> ed)
		scope.inTransaction( em -> {
			RefEdMapKeyEntity ed = new RefEdMapKeyEntity();

			em.persist( ed );

			RefIngMapKeyEntity ing1 = new RefIngMapKeyEntity();
			ing1.setData( "a" );
			ing1.setReference( ed );

			RefIngMapKeyEntity ing2 = new RefIngMapKeyEntity();
			ing2.setData( "b" );

			em.persist( ing1 );
			em.persist( ing2 );

			ed_id = ed.getId();
			ing1_id = ing1.getId();
			ing2_id = ing2.getId();
		} );

		// Revision 2 (adding second relation: ing2 -> ed)
		scope.inTransaction( em -> {
			RefEdMapKeyEntity ed = em.find( RefEdMapKeyEntity.class, ed_id );
			RefIngMapKeyEntity ing2 = em.find( RefIngMapKeyEntity.class, ing2_id );

			ing2.setReference( ed );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( RefEdMapKeyEntity.class, ed_id ) );

			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( RefIngMapKeyEntity.class, ing1_id ) );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( RefIngMapKeyEntity.class, ing2_id ) );
		} );
	}

	@Test
	public void testHistoryOfEd(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			RefIngMapKeyEntity ing1 = em.find( RefIngMapKeyEntity.class, ing1_id );
			RefIngMapKeyEntity ing2 = em.find( RefIngMapKeyEntity.class, ing2_id );

			RefEdMapKeyEntity rev1 = auditReader.find( RefEdMapKeyEntity.class, ed_id, 1 );
			RefEdMapKeyEntity rev2 = auditReader.find( RefEdMapKeyEntity.class, ed_id, 2 );

			assertEquals( TestTools.makeMap( "a", ing1 ), rev1.getIdmap() );
			assertEquals( TestTools.makeMap( "a", ing1, "b", ing2 ), rev2.getIdmap() );
		} );
	}
}
