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
public class AuditedEmbeddableWithDeclaredDataTest extends BaseEnversJPAFunctionalTestCase {

	private long id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				EntityWithAuditedEmbeddableWithDeclaredData.class,
				AbstractAuditedEmbeddable.class,
				AuditedEmbeddableWithDeclaredData.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		this.id = TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
		final EntityWithAuditedEmbeddableWithDeclaredData entity = new EntityWithAuditedEmbeddableWithDeclaredData();
		entity.setName( "Entity 1" );
		entity.setValue( new AuditedEmbeddableWithDeclaredData( 42, "Data" ) );

		entityManager.persist(entity);
		return entity.getId();
		} );
	}

	@Test
	public void testEmbeddableThatExtendsAuditedMappedSuperclass() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final EntityWithAuditedEmbeddableWithDeclaredData entity = entityManager.find(
					EntityWithAuditedEmbeddableWithDeclaredData.class,
					id
			);

			final AuditReader auditReader = AuditReaderFactory.get( entityManager );

			final List<Number> revisions = auditReader.getRevisions( EntityWithAuditedEmbeddableWithDeclaredData.class, id );
			assertThat( revisions ).hasSize( 1 );

			final EntityWithAuditedEmbeddableWithDeclaredData entityRevision1 = auditReader.find(
					EntityWithAuditedEmbeddableWithDeclaredData.class,
					id,
					revisions.get( 0 )
			);
			assertThat( entityRevision1.getName() ).isEqualTo( entity.getName() );

			// All fields should be audited because the mapped superclass is annotated
			assertThat( entity.getValue().getCodeart() ).isEqualTo( entityRevision1.getValue().getCodeart() );
			assertThat( entityRevision1.getValue().getCode() ).isEqualTo( 42 );
		} );
	}
}
