/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.naturalid;

import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.mapping.internal.view.NaturalIdContributionView;
import org.hibernate.boot.mapping.internal.categorize.EntityHierarchy;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.orm.test.boot.models.bind.BindingTestingHelper;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class NaturalIdContributionTests {
	@Test
	@ServiceRegistry
	void testNaturalIdContribution(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final EntityTypeMetadata entityType = entityType( context, NaturalEntity.class );
					final NaturalIdContributionView contribution = context.getBindingState()
							.getBootBindingModel()
							.getNaturalIdContributionView( entityType, "code" );
					assertThat( contribution ).isNotNull();
					assertThat( contribution.owner() ).isSameAs( entityType );
					assertThat( contribution.attributeName() ).isEqualTo( "code" );
					assertThat( contribution.member().getName() ).isEqualTo( "code" );
					assertThat( contribution.mutable() ).isTrue();

					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( NaturalEntity.class.getName() );
					final Property property = entityBinding.getProperty( "code" );
					assertThat( property.isNaturalIdentifier() ).isTrue();
					assertThat( property.isUpdatable() ).isTrue();
				},
				scope.getRegistry(),
				NaturalEntity.class
		);
	}

	private static EntityTypeMetadata entityType(
			BindingTestingHelper.DomainModelCheckContext context,
			Class<?> entityClass) {
		for ( EntityHierarchy hierarchy : context.getCategorizedDomainModel().getEntityHierarchies() ) {
			if ( hierarchy.getRoot().getClassDetails().getClassName().equals( entityClass.getName() ) ) {
				return hierarchy.getRoot();
			}
		}
		throw new AssertionError( "Could not locate entity type for " + entityClass.getName() );
	}

	@Entity(name = "NaturalEntity")
	public static class NaturalEntity {
		@Id
		private Integer id;

		@NaturalId(mutable = true)
		private String code;
	}
}
