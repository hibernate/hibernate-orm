/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids;

import java.util.Arrays;
import java.util.Date;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.ids.CompositeDateIdTestEntity;
import org.hibernate.orm.test.envers.entities.ids.DateEmbId;
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
@Jpa(annotatedClasses = {CompositeDateIdTestEntity.class})
public class CompositeDateId {
	private DateEmbId id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			CompositeDateIdTestEntity dite = new CompositeDateIdTestEntity( new DateEmbId( new Date(), new Date() ), "x" );
			em.persist( dite );

			id1 = dite.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			CompositeDateIdTestEntity dite = em.find( CompositeDateIdTestEntity.class, id1 );
			dite.setStr1( "y" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( CompositeDateIdTestEntity.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( "x", auditReader.find( CompositeDateIdTestEntity.class, id1, 1 ).getStr1() );
			assertEquals( "y", auditReader.find( CompositeDateIdTestEntity.class, id1, 2 ).getStr1() );
		} );
	}
}
