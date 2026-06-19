/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.collation;

import org.hibernate.annotations.Collate;
import org.hibernate.boot.mapping.internal.view.CollationContributionView;
import org.hibernate.boot.mapping.internal.categorize.EntityHierarchy;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.orm.test.boot.models.bind.BindingTestingHelper;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class CollationContributionTests {
	@Test
	@ServiceRegistry
	void testCollationContribution(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final EntityTypeMetadata entityType = entityType( context, CollatedEntity.class );
					final CollationContributionView nameContribution = context.getBindingState()
							.getBootBindingModel()
							.getCollationContributionView( entityType, "name" );
					assertThat( nameContribution ).isNotNull();
					assertThat( nameContribution.owner() ).isSameAs( entityType );
					assertThat( nameContribution.attributePath() ).isEqualTo( "name" );
					assertThat( nameContribution.member().getName() ).isEqualTo( "name" );
					assertThat( nameContribution.collation() ).isEqualTo( "ucs_basic" );

					final CollationContributionView nestedContribution = context.getBindingState()
							.getBootBindingModel()
							.getCollationContributionView( entityType, "details.description" );
					assertThat( nestedContribution ).isNotNull();
					assertThat( nestedContribution.owner() ).isSameAs( entityType );
					assertThat( nestedContribution.attributePath() ).isEqualTo( "details.description" );
					assertThat( nestedContribution.member().getName() ).isEqualTo( "description" );
					assertThat( nestedContribution.collation() ).isEqualTo( "en_US" );

					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CollatedEntity.class.getName() );
					final BasicValue nameValue = (BasicValue) entityBinding.getProperty( "name" ).getValue();
					assertThat( ( (Column) nameValue.getColumn() ).getCollation() ).isEqualTo( "ucs_basic" );

					final Component details = (Component) entityBinding.getProperty( "details" ).getValue();
					final BasicValue descriptionValue = (BasicValue) details.getProperty( "description" ).getValue();
					assertThat( ( (Column) descriptionValue.getColumn() ).getCollation() ).isEqualTo( "en_US" );
				},
				scope.getRegistry(),
				CollatedEntity.class
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

	@Entity(name = "CollatedEntity")
	public static class CollatedEntity {
		@Id
		private Integer id;

		@Collate("ucs_basic")
		private String name;

		@Embedded
		private CollatedDetails details;
	}

	@Embeddable
	public static class CollatedDetails {
		@Collate("en_US")
		private String description;
	}
}
