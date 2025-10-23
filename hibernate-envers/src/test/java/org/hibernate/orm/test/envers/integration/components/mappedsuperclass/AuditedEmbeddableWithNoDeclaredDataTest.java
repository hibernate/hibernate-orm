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
		EntityWithAuditedEmbeddableWithNoDeclaredData.class,
		AbstractAuditedEmbeddable.class,
		AuditedEmbeddableWithDeclaredData.class,
		AuditedEmbeddableWithNoDeclaredData.class
})
public class AuditedEmbeddableWithNoDeclaredDataTest {

	private long id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		this.id = scope.fromTransaction( entityManager -> {
			final EntityWithAuditedEmbeddableWithNoDeclaredData entity = new EntityWithAuditedEmbeddableWithNoDeclaredData();
			entity.setName( "Entity 1" );
			entity.setValue( new AuditedEmbeddableWithNoDeclaredData( 42 ) );

			entityManager.persist( entity );
			return entity.getId();
		} );
	}

	@Test
	public void testEmbeddableThatExtendsAuditedMappedSuperclass(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
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
