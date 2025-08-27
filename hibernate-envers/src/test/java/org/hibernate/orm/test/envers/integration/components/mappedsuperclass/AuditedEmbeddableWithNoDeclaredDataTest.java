/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.mappedsuperclass;

import java.util.List;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Chris Cranford
 */
@JiraKey("HHH-17189")
public class AuditedEmbeddableWithNoDeclaredDataTest extends BaseEnversJPAFunctionalTestCase {

	private long id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				EntityWithAuditedEmbeddableWithNoDeclaredData.class,
				AbstractAuditedEmbeddable.class,
				AuditedEmbeddableWithDeclaredData.class,
				AuditedEmbeddableWithNoDeclaredData.class,
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		this.id = TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
		final EntityWithAuditedEmbeddableWithNoDeclaredData entity = new EntityWithAuditedEmbeddableWithNoDeclaredData();
		entity.setName( "Entity 1" );
		entity.setValue( new AuditedEmbeddableWithNoDeclaredData( 42 ) );

		entityManager.persist(entity);
		return entity.getId();
		} );
	}

	@Test
	public void testEmbeddableThatExtendsAuditedMappedSuperclass() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final EntityWithAuditedEmbeddableWithNoDeclaredData entity = entityManager.find(
					EntityWithAuditedEmbeddableWithNoDeclaredData.class,
					id
			);

			final AuditReader auditReader = AuditReaderFactory.get( entityManager );

			final List<Number> revisions = auditReader.getRevisions( EntityWithAuditedEmbeddableWithNoDeclaredData.class, id );
			assertThat( revisions ).hasSize( 1 );

			final EntityWithAuditedEmbeddableWithNoDeclaredData entityRevision1 = auditReader.find(
					EntityWithAuditedEmbeddableWithNoDeclaredData.class,
					id,
					revisions.get( 0 )
			);
			assertThat( entityRevision1.getName() ).isEqualTo( entity.getName() );

			// All fields should be audited because the mapped superclass is annotated
			assertThat( entityRevision1.getValue().getCode() ).isEqualTo( 42 );
		} );
	}
}
