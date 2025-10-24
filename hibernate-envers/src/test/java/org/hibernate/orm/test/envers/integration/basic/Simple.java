/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.IntTestEntity;
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
@Jpa(annotatedClasses = {IntTestEntity.class})
public class Simple {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			IntTestEntity ite = new IntTestEntity( 10 );
			em.persist( ite );
			id1 = ite.getId();
		} );

		scope.inTransaction( em -> {
			IntTestEntity ite = em.find( IntTestEntity.class, id1 );
			ite.setNumber( 20 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals(
					Arrays.asList( 1, 2 ),
					AuditReaderFactory.get( em ).getRevisions( IntTestEntity.class, id1 )
			);
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		IntTestEntity ver1 = new IntTestEntity( 10, id1 );
		IntTestEntity ver2 = new IntTestEntity( 20, id1 );

		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( IntTestEntity.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( IntTestEntity.class, id1, 2 ) );
		} );
	}
}
