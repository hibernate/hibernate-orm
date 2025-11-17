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
@DomainModel(annotatedClasses = {SecondaryNamingTestEntity.class})
@SessionFactory
public class NamingSecondary {
	private Integer id;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		SecondaryNamingTestEntity ste = new SecondaryNamingTestEntity( "a", "1" );

		// Revision 1
		scope.inTransaction( em -> {
			em.persist( ste );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			SecondaryNamingTestEntity entity = em.find( SecondaryNamingTestEntity.class, ste.getId() );
			entity.setS1( "b" );
			entity.setS2( "2" );
		} );

		id = ste.getId();
	}

	@Test
	public void testRevisionsCounts(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( SecondaryNamingTestEntity.class, id ) );
		} );
	}

	@Test
	public void testHistoryOfId(SessionFactoryScope scope) {
		SecondaryNamingTestEntity ver1 = new SecondaryNamingTestEntity( id, "a", "1" );
		SecondaryNamingTestEntity ver2 = new SecondaryNamingTestEntity( id, "b", "2" );

		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( SecondaryNamingTestEntity.class, id, 1 ) );
			assertEquals( ver2, auditReader.find( SecondaryNamingTestEntity.class, id, 2 ) );
		} );
	}

	@Test
	public void testTableNames(DomainModelScope scope) {
		assertEquals( "sec_versions", scope.getDomainModel()
				.getEntityBinding( "org.hibernate.orm.test.envers.integration.secondary.SecondaryNamingTestEntity_AUD" )
				.getJoins().get( 0 ).getTable().getName() );
	}
}
