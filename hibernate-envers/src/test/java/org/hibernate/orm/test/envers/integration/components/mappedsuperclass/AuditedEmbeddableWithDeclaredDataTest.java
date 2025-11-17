/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.mappedsuperclass;

import java.util.List;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Chris Cranford
 */
@JiraKey("HHH-17189")
@EnversTest
@Jpa(annotatedClasses = {
		EntityWithAuditedEmbeddableWithDeclaredData.class,
		AbstractAuditedEmbeddable.class,
		AuditedEmbeddableWithDeclaredData.class
})
public class AuditedEmbeddableWithDeclaredDataTest {

	private long id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		this.id = scope.fromTransaction( entityManager -> {
			final EntityWithAuditedEmbeddableWithDeclaredData entity = new EntityWithAuditedEmbeddableWithDeclaredData();
			entity.setName( "Entity 1" );
			entity.setValue( new AuditedEmbeddableWithDeclaredData( 42, "Data" ) );

			entityManager.persist( entity );
			return entity.getId();
		} );
	}

	@Test
	public void testEmbeddableThatExtendsAuditedMappedSuperclass(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
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
