/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.comment;


import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey(value = "HHH-19011")
@DomainModel(annotatedClasses = {CommentElementCollectionTest.MainEntity.class})
@SessionFactory
@BootstrapServiceRegistry(integrators = CommentElementCollectionTest.MetadataIntegrator.class)
public class CommentElementCollectionTest {
	private static Metadata METADATA;

	@Test
	public void testTableCommentsPlacement(SessionFactoryScope scope) {
		scope.inSession(session -> {
			Collection<PersistentClass> entityBindings = METADATA.getEntityBindings();
			assertThat( entityBindings.iterator().next().getTable().getComment() ).isEqualTo(
					MainEntity.EXPECTED_MAIN_ENTITY_TABLE_COMMENT );

			Collection<org.hibernate.mapping.Collection> collectionBindings = METADATA.getCollectionBindings();
			assertThat( collectionBindings.iterator().next().getCollectionTable().getComment() ).isEqualTo(
					MainEntity.EXPECTED_ELEMENT_COLLECTION_TABLE_COMMENT );
		});
	}

	@Entity
	@Table(comment = MainEntity.EXPECTED_MAIN_ENTITY_TABLE_COMMENT)
	static class MainEntity {
		public static final String EXPECTED_MAIN_ENTITY_TABLE_COMMENT = "Expected main entity table level comment";
		public static final String EXPECTED_ELEMENT_COLLECTION_TABLE_COMMENT = "Expected element collection table level comment";

		@Id
		@GeneratedValue
		public long id;

		@ElementCollection
		@CollectionTable(comment = EXPECTED_ELEMENT_COLLECTION_TABLE_COMMENT)
		@OrderColumn(name = "elementOrder", nullable = false)
		public List<String> embeddableValues;
	}

	public static class MetadataIntegrator implements Integrator {
		@Override
		public void integrate(Metadata metadata, BootstrapContext bootstrapContext, SessionFactoryImplementor sessionFactory) {
			METADATA = metadata;
		}
	}
}
