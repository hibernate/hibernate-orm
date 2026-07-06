/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml;

import java.util.List;

import org.hibernate.boot.mapping.internal.categorize.DomainModelCategorizer;
import org.hibernate.boot.pipeline.internal.source.PreparedMappingSources;
import org.hibernate.orm.test.boot.models.XmlHelper;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;

/**
 * Verifies the XML/source phase boundary before categorization.
 */
@ServiceRegistry
public class XmlPhaseBoundaryTests {
	@Test
	void metadataCompleteXmlReplacesExistingAnnotationsBeforeCategorization(ServiceRegistryScope scope) {
		final var categorizedDomainModel = categorize(
				"mappings/models/xml-phase-boundary/complete-embeddable.xml",
				scope
		);

		assertThat( categorizedDomainModel.getEntityHierarchies() ).isEmpty();
		assertThat( categorizedDomainModel.getEmbeddables() )
				.containsKey( CompleteXmlReplacesEntity.class.getName() );
	}

	@Test
	void nonCompleteXmlOverlayCreatesPersistentRoleBeforeCategorization(ServiceRegistryScope scope) {
		final var categorizedDomainModel = categorize(
				"mappings/models/xml-phase-boundary/overlay-entity.xml",
				scope
		);

		assertThat( categorizedDomainModel.getEntityHierarchies() ).singleElement().satisfies( (hierarchy) ->
				assertThat( hierarchy.getRoot().getClassDetails().getClassName() )
						.isEqualTo( OverlayXmlCreatesEntity.class.getName() )
		);
	}

	@Test
	void implicitXmlDefaultListenerUsesAnnotatedCallbackMethods(ServiceRegistryScope scope) {
		final var categorizedDomainModel = categorize(
				"mappings/models/xml-phase-boundary/implicit-default-listener.xml",
				scope
		);

		assertThat( categorizedDomainModel.getGlobalRegistrations().getEntityListenerRegistrations() )
				.singleElement()
				.satisfies( (listener) -> {
					assertThat( listener.getCallbackClass().getClassName() )
							.isEqualTo( ImplicitDefaultListener.class.getName() );
					assertThat( listener.getPrePersistMethod() )
							.isNotNull()
							.extracting( method -> method.getName() )
							.isEqualTo( "beforePersist" );
				} );
	}

	private static org.hibernate.boot.mapping.internal.categorize.CategorizedDomainModel categorize(
			String mappingResource,
			ServiceRegistryScope scope) {
		final var metadataBuildingContext = new MetadataBuildingContextTestingImpl( scope.getRegistry() );
		final var resolvedMappingSources = new PreparedMappingSources(
				List.of(),
				List.of(),
				List.of( XmlHelper.bindMapping( mappingResource, SIMPLE_CLASS_LOADING ) )
		);
		return DomainModelCategorizer.categorize( resolvedMappingSources, metadataBuildingContext );
	}

	@Entity
	public static class CompleteXmlReplacesEntity {
		@Id
		private Long id;
		private String name;
	}

	public static class OverlayXmlCreatesEntity {
		private Long id;
		private String name;
	}

	public static class ImplicitDefaultListener {
		@PrePersist
		public void beforePersist(Object entity) {
		}
	}
}
