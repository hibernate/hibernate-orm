/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

import static org.assertj.core.api.Assertions.assertThat;

public class DeterministicOrderingTests {
	@Test
	@ServiceRegistry
	void testCompatibilityAndRuntimeOrderingRemainAligned(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( OrderingEntity.class.getName() );
					final Component component =
							(Component) entityBinding.getProperty( "component" ).getValue();

					try (var sessionFactory =
							org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory( context.getMetadata() )) {
						assertThat( entityBinding.getProperties() )
								.extracting( Property::getName )
								.containsExactly( "alpha", "component", "zeta" );
						assertThat( component.getProperties() )
								.extracting( Property::getName )
								.containsExactly( "alpha", "zeta" );

						final var entityMapping = sessionFactory.getMappingMetamodel()
								.getEntityDescriptor( OrderingEntity.class );
						final var attributeMappings = entityMapping.getAttributeMappings();
						assertThat( attributeMappings.size() ).isEqualTo( 3 );
						assertThat( attributeMappings.get( 0 ).getAttributeName() ).isEqualTo( "alpha" );
						assertThat( attributeMappings.get( 1 ).getAttributeName() ).isEqualTo( "component" );
						assertThat( attributeMappings.get( 2 ).getAttributeName() ).isEqualTo( "zeta" );
						assertThat( entityMapping.getEntityPersister().getPropertyNames() )
								.containsExactly( "alpha", "component", "zeta" );

						final var embedded = (EmbeddedAttributeMapping) attributeMappings.get( 1 );
						final var embeddedAttributes = embedded.getMappedType().getAttributeMappings();
						assertThat( embeddedAttributes.size() ).isEqualTo( 2 );
						assertThat( embeddedAttributes.get( 0 ).getAttributeName() ).isEqualTo( "alpha" );
						assertThat( embeddedAttributes.get( 1 ).getAttributeName() ).isEqualTo( "zeta" );
					}
				},
				scope.getRegistry(),
				OrderingEntity.class
		);
	}

	@Entity(name = "OrderingEntity")
	public static class OrderingEntity {
		@Id
		private Integer id;
		private String zeta;
		@Embedded
		private OrderingComponent component;
		private String alpha;
	}

	@Embeddable
	public static class OrderingComponent {
		@Column(name = "component_zeta")
		private String zeta;
		@Column(name = "component_alpha")
		private String alpha;
	}
}
