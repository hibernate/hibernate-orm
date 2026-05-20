/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.xml.partial;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for partial XML overlays that contribute only an
 * {@code <entity-listeners>} block to an entity which otherwise relies on
 * annotation-based mapping. Prior to the fix, the overlay caused the access
 * type to fall back to {@link jakarta.persistence.AccessType#PROPERTY} and be
 * stamped onto the entity via a synthetic {@code @Access}, which masked the
 * natural {@link jakarta.persistence.AccessType#FIELD} detection driven by an
 * {@code @Id} inherited from a {@code @MappedSuperclass} field. Field-level
 * associations such as {@code @OneToMany} on {@link PartialOverlayParent}
 * then became invisible to the binder and the collection element type was
 * processed as a basic type, failing boot with {@code MappingException}
 * / {@code JdbcTypeRecommendationException}.
 */
@Jpa(
		annotatedClasses = {
				PartialOverlayBaseEntity.class,
				PartialOverlayParent.class,
				PartialOverlayChild.class
		},
		xmlMappings = "mappings/callbacks/partial-overlay.xml"
)
public class PartialOverlayEntityListenerTest {

	@AfterEach
	void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	void bootAndPersistThroughAnnotationDrivenAssociation(EntityManagerFactoryScope scope) {
		PartialOverlayListener.reset();

		final Long parentId = scope.fromTransaction( em -> {
			final PartialOverlayParent parent = new PartialOverlayParent();
			final PartialOverlayChild child = new PartialOverlayChild();
			child.setParent( parent );
			parent.getChildren().add( child );

			em.persist( parent );
			em.persist( child );
			return parent.getId();
		} );

		assertThat( PartialOverlayListener.getPrePersistCount() ).isEqualTo( 1 );

		scope.inTransaction( em -> {
			final PartialOverlayParent loaded = em.find( PartialOverlayParent.class, parentId );
			assertThat( loaded ).isNotNull();
			assertThat( loaded.getChildren() ).hasSize( 1 );
			assertThat( loaded.getChildren().get( 0 ).getParent() ).isSameAs( loaded );
		} );
	}
}
