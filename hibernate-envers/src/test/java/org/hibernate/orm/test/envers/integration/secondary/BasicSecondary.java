/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.secondary;

import org.hibernate.envers.AuditReaderFactory;
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
@DomainModel(annotatedClasses = {SecondaryTestEntity.class})
@SessionFactory
public class BasicSecondary {
	private Integer id;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		SecondaryTestEntity ste = new SecondaryTestEntity( "a", "1" );

		// Revision 1
		scope.inTransaction( em -> {
			em.persist( ste );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			SecondaryTestEntity entity = em.find( SecondaryTestEntity.class, ste.getId() );
			entity.setS1( "b" );
			entity.setS2( "2" );
		} );

		id = ste.getId();
	}

	@Test
	public void testRevisionsCounts(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( SecondaryTestEntity.class, id ) );
		} );
	}

	@Test
	public void testHistoryOfId(SessionFactoryScope scope) {
		SecondaryTestEntity ver1 = new SecondaryTestEntity( id, "a", "1" );
		SecondaryTestEntity ver2 = new SecondaryTestEntity( id, "b", "2" );

		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( SecondaryTestEntity.class, id, 1 ) );
			assertEquals( ver2, auditReader.find( SecondaryTestEntity.class, id, 2 ) );
		} );
	}

	@Test
	public void testTableNames(DomainModelScope scope) {
		assertEquals( "secondary_AUD", scope.getDomainModel()
				.getEntityBinding( "org.hibernate.orm.test.envers.integration.secondary.SecondaryTestEntity_AUD" )
				.getJoins().get( 0 ).getTable().getName() );
	}
}
