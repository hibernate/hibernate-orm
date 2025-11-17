/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.secondary.ids;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.ids.MulId;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@DomainModel(annotatedClasses = {SecondaryMulIdTestEntity.class})
@SessionFactory
public class MulIdSecondary {
	private MulId id;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		id = new MulId( 1, 2 );

		SecondaryMulIdTestEntity ste = new SecondaryMulIdTestEntity( id, "a", "1" );

		// Revision 1
		scope.inTransaction( em -> {
			em.persist( ste );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			SecondaryMulIdTestEntity entity = em.find( SecondaryMulIdTestEntity.class, id );
			entity.setS1( "b" );
			entity.setS2( "2" );
		} );
	}

	@Test
	public void testRevisionsCounts(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( SecondaryMulIdTestEntity.class, id ) );
		} );
	}

	@Test
	public void testHistoryOfId(SessionFactoryScope scope) {
		SecondaryMulIdTestEntity ver1 = new SecondaryMulIdTestEntity( id, "a", "1" );
		SecondaryMulIdTestEntity ver2 = new SecondaryMulIdTestEntity( id, "b", "2" );

		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( SecondaryMulIdTestEntity.class, id, 1 ) );
			assertEquals( ver2, auditReader.find( SecondaryMulIdTestEntity.class, id, 2 ) );
		} );
	}

	@Test
	public void testTableNames(DomainModelScope scope) {
		assertEquals( "sec_mulid_versions", scope.getDomainModel().getEntityBinding(
						"org.hibernate.orm.test.envers.integration.secondary.ids.SecondaryMulIdTestEntity_AUD" ).getJoins()
				.get( 0 ).getTable().getName() );
	}
}
