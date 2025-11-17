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
 * @author Jakob Braeuchi.
 * @author Gail Badner
 */
@JiraKey(value = "HHH-9193")
@EnversTest
@Jpa(annotatedClasses = {
		EntityWithEmbeddableWithNoDeclaredData.class,
		AbstractEmbeddable.class,
		EmbeddableWithNoDeclaredData.class
})
public class EmbeddableWithNoDeclaredDataTest {
	private long id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		this.id = scope.fromTransaction( entityManager -> {
			EntityWithEmbeddableWithNoDeclaredData entity = new EntityWithEmbeddableWithNoDeclaredData();
			entity.setName( "Entity 1" );
			entity.setValue( new EmbeddableWithNoDeclaredData( 84 ) );
			entityManager.persist( entity );
			return entity.getId();
		} );
	}

	@Test
	public void testEmbeddableThatExtendsMappedSuperclass(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			EntityWithEmbeddableWithNoDeclaredData entityLoaded = entityManager.find( EntityWithEmbeddableWithNoDeclaredData.class, id );

			AuditReader reader = AuditReaderFactory.get( entityManager );

			List<Number> revs = reader.getRevisions( EntityWithEmbeddableWithNoDeclaredData.class, id );
			assertThat( revs ).hasSize( 1 );

			EntityWithEmbeddableWithNoDeclaredData entityRev1 = reader.find( EntityWithEmbeddableWithNoDeclaredData.class, id, revs.get( 0 ) );

			assertThat( entityRev1.getName() ).isEqualTo( entityLoaded.getName() );

			// value should be null because there is no data in EmbeddableWithNoDeclaredData
			// and the fields in AbstractEmbeddable should not be audited.
			assertThat( entityRev1.getValue() ).isNull();
		} );
	}
}
