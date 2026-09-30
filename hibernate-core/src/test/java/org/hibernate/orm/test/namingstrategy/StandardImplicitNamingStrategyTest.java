/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Hibernate naming conventions, including full component paths and logical dependencies.
/// Shared rules are inherited as tests with independently stated expected names.
///
/// @author Steve Ebersole
class StandardImplicitNamingStrategyTest extends AbstractSupportedImplicitNamingTest {
	@Override
	ImplicitNamingStrategy strategy() { return new StandardImplicitNamingStrategy(); }

	/// Full paths distinguish nested embeddable columns, relationship columns, and collection roles.
	@Test
	void nestedEmbeddableNaming() {
		inspect( metadata -> {
			var model = entity( metadata, EmbeddedOwner.class );
			columns( model.getRecursiveProperty( "home.location.zip" ).getValue(), "home_location_zip" );
			columns( model.getRecursiveProperty( "home.location.target" ).getValue(), "home_location_target_target_pk" );
			var tags = collection( metadata, EmbeddedOwner.class, "home.tags" );
			assertThat( tags.getCollectionTable().getName() ).isEqualTo( "EmbeddedOwner_home_tags" );
			var labels = (org.hibernate.mapping.Map) collection( metadata, EmbeddedOwner.class, "home.labels" );
			var links = collection( metadata, EmbeddedOwner.class, "home.links" );
			org.assertj.core.api.SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( ((org.hibernate.mapping.Map) collection( metadata, EmbeddedOwner.class, "home.byTarget" )).getIndex().getColumns() )
						.extracting( org.hibernate.mapping.Column::getName ).containsExactly( "home_byTarget_KEY" );
				softly.assertThat( ((org.hibernate.mapping.List) tags).getIndex().getColumns() )
						.extracting( org.hibernate.mapping.Column::getName ).containsExactly( "home_tags_ORDER" );
				softly.assertThat( labels.getIndex().getColumns() )
						.extracting( org.hibernate.mapping.Column::getName ).containsExactly( "home_labels_KEY" );
				softly.assertThat( tags.getElement().getColumns() )
						.extracting( org.hibernate.mapping.Column::getName ).containsExactly( "home_tags" );
				softly.assertThat( links.getCollectionTable().getName() ).isEqualTo( "EmbeddedOwner_targets" );
				softly.assertThat( links.getKey().getColumns() )
						.extracting( org.hibernate.mapping.Column::getName ).containsExactly( "EmbeddedOwner_id" );
				softly.assertThat( links.getElement().getColumns() )
						.extracting( org.hibernate.mapping.Column::getName ).containsExactly( "home_links_target_pk" );
			} );
		}, EmbeddedOwner.class, Target.class );
	}

	/// Explicit [jakarta.persistence.AttributeOverride] and [jakarta.persistence.AssociationOverride]
	/// replace implicit names even inside nested embeddables.
	@Test
	void nestedOverridesWin() {
		inspect( metadata -> {
			var model = entity( metadata, OverriddenOwner.class );
			columns( model.getRecursiveProperty( "home.location.zip" ).getValue(), "home_zip" );
			columns( model.getRecursiveProperty( "home.location.target" ).getValue(), "home_target" );
		}, OverriddenOwner.class, Target.class );
	}
	/// Explicit overrides resolve repeated embeddable column collisions under either strategy.
	@Test
	void repeatedEmbeddablesWithOverrides() {
		inspect( metadata -> {
			var model = entity( metadata, RepeatedOverrides.class );
			columns( model.getRecursiveProperty( "home.location.zip" ).getValue(), "home_zip" );
			columns( model.getRecursiveProperty( "work.location.zip" ).getValue(), "work_zip" );
		}, RepeatedOverrides.class );
	}

	/// Full paths make repeated embeddables usable without explicit column overrides.
	@Test
	void repeatedEmbeddablesNeedNoOverrides() {
		inspect( metadata -> {
			var model = entity( metadata, Repeated.class );
			columns( model.getRecursiveProperty( "home.location.zip" ).getValue(), "home_location_zip" );
			columns( model.getRecursiveProperty( "work.location.zip" ).getValue(), "work_location_zip" );
		}, Repeated.class );
	}

	/// Embedded-id members include the identifier attribute path under Standard.
	@Test
	void implicitEmbeddedIdentifierNames() {
		inspect( metadata -> columns( entity( metadata, ImplicitIdOwner.class ).getIdentifier(), "id_region", "id_number" ), ImplicitIdOwner.class );
	}

	/// Hibernate physical-naming integration, not a JPA requirement: Standard composes from logical table names.
	@Test
	void associationTableWithPhysicalPrefix() {
		inspect( new org.hibernate.boot.pipeline.internal.source.MappingSources().addManagedClasses( Author.class, Book.class ),
				new ImplicitNamingStrategyMatrixTest.PhysicalStrategy( "p_" ), metadata -> {
			assertThat( collection( metadata, Author.class, "books" ).getCollectionTable().getName() )
					.isEqualTo( "p_author_table_book_table" );
		} );
	}

	/// IdClass attributes are declared directly on the entity: their full paths
	/// are the field names, without an artificial identifier-mapper prefix.
	@Test
	void implicitIdClassNames() {
		inspect( metadata -> columns( entity( metadata, ImplicitIdClassOwner.class ).getIdentifier(), "region", "number" ), ImplicitIdClassOwner.class );
	}

	/// The collection element path contributes to Standard member names without leaking synthetic element markers.
	@Test
	void embeddableElementNames() {
		inspect( metadata -> {
			var addresses = collection( metadata, EmbeddedElements.class, "addresses" );
			assertThat( addresses.getCollectionTable().getName() ).isEqualTo( "EmbeddedElements_addresses" );
			columns( addresses.getElement(), "addresses_zip" );
		}, EmbeddedElements.class );
	}

}
