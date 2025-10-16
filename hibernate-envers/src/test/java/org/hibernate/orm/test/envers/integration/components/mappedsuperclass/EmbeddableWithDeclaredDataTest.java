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
import org.hibernate.testing.orm.junit.FailureExpected;
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
		EntityWithEmbeddableWithDeclaredData.class,
		AbstractEmbeddable.class,
		EmbeddableWithDeclaredData.class
})
public class EmbeddableWithDeclaredDataTest {
	private long id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		this.id = scope.fromTransaction( entityManager -> {
			EntityWithEmbeddableWithDeclaredData entity = new EntityWithEmbeddableWithDeclaredData();
			entity.setName( "Entity 1" );
			entity.setValue( new EmbeddableWithDeclaredData( 42, "TestCodeart" ) );
			entityManager.persist( entity );
			return entity.getId();
		} );
	}

	@Test
	@FailureExpected( jiraKey = "HHH-9193" )
	public void testEmbeddableThatExtendsMappedSuperclass(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			EntityWithEmbeddableWithDeclaredData entityLoaded = entityManager.find( EntityWithEmbeddableWithDeclaredData.class, id );

			AuditReader reader = AuditReaderFactory.get( entityManager );

			List<Number> revs = reader.getRevisions( EntityWithEmbeddableWithDeclaredData.class, id );
			assertThat( revs ).hasSize( 1 );

			EntityWithEmbeddableWithDeclaredData entityRev1 = reader.find( EntityWithEmbeddableWithDeclaredData.class, id, revs.get( 0 ) );
			assertThat( entityRev1.getName() ).isEqualTo( entityLoaded.getName() );

			// only value.codeArt should be audited because it is the only audited field in EmbeddableWithDeclaredData;
			// fields in AbstractEmbeddable should not be audited.
			assertThat( entityRev1.getValue().getCodeart() ).isEqualTo( entityLoaded.getValue().getCodeart() );
			assertThat( entityRev1.getValue().getCode() ).isEqualTo( 0 );
		} );
	}
}
