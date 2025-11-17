/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.secondary.ids;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.ids.EmbId;
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
@DomainModel(annotatedClasses = {SecondaryEmbIdTestEntity.class})
@SessionFactory
public class EmbIdSecondary {
	private EmbId id;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		id = new EmbId( 1, 2 );

		SecondaryEmbIdTestEntity ste = new SecondaryEmbIdTestEntity( id, "a", "1" );

		// Revision 1
		scope.inTransaction( em -> {
			em.persist( ste );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			SecondaryEmbIdTestEntity entity = em.find( SecondaryEmbIdTestEntity.class, ste.getId() );
			entity.setS1( "b" );
			entity.setS2( "2" );
		} );
	}

	@Test
	public void testRevisionsCounts(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( SecondaryEmbIdTestEntity.class, id ) );
		} );
	}

	@Test
	public void testHistoryOfId(SessionFactoryScope scope) {
		SecondaryEmbIdTestEntity ver1 = new SecondaryEmbIdTestEntity( id, "a", "1" );
		SecondaryEmbIdTestEntity ver2 = new SecondaryEmbIdTestEntity( id, "b", "2" );

		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( SecondaryEmbIdTestEntity.class, id, 1 ) );
			assertEquals( ver2, auditReader.find( SecondaryEmbIdTestEntity.class, id, 2 ) );
		} );
	}

	@Test
	public void testTableNames(DomainModelScope scope) {
		assertEquals( "sec_embid_versions", scope.getDomainModel().getEntityBinding(
						"org.hibernate.orm.test.envers.integration.secondary.ids.SecondaryEmbIdTestEntity_AUD" ).getJoins()
				.get( 0 ).getTable().getName() );
	}
}
