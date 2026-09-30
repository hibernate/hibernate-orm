/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Specification-derived JPA naming expectations with identity physical naming.
/// Shared rules are inherited as tests with independently stated expected names.
///
/// @author Steve Ebersole
class JpaImplicitNamingStrategyComplianceTest extends AbstractSupportedImplicitNamingTest {
	@Override
	ImplicitNamingStrategy strategy() { return new ImplicitNamingStrategyJpaCompliantImpl(); }

	/// [jakarta.persistence.Column#name()] and [jakarta.persistence.JoinColumn#name()] use the embeddable member name.
	@Test
	void nestedEmbeddableNaming() {
		inspect( metadata -> {
			var model = entity( metadata, EmbeddedOwner.class );
			columns( model.getRecursiveProperty( "home.location.zip" ).getValue(), "zip" );
			columns( model.getRecursiveProperty( "home.location.target" ).getValue(), "target_target_pk" );
			var tags = collection( metadata, EmbeddedOwner.class, "home.tags" );
			assertThat( tags.getCollectionTable().getName() ).isEqualTo( "EmbeddedOwner_tags" );
			columns( ((org.hibernate.mapping.List) tags).getIndex(), "tags_ORDER" );
			var labels = (org.hibernate.mapping.Map) collection( metadata, EmbeddedOwner.class, "home.labels" );
			columns( labels.getIndex(), "labels_KEY" );
			columns( ((org.hibernate.mapping.Map) collection( metadata, EmbeddedOwner.class, "home.byTarget" )).getIndex(), "byTarget_KEY" );
			columns( tags.getElement(), "tags" );
			var links = collection( metadata, EmbeddedOwner.class, "home.links" );
			assertThat( links.getCollectionTable().getName() ).isEqualTo( "EmbeddedOwner_targets" );
			columns( links.getKey(), "EmbeddedOwner_id" );
			columns( links.getElement(), "links_target_pk" );
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

	/// JPA embedded-id members use their own field names ([jakarta.persistence.Column#name()]).
	@Test
	void implicitEmbeddedIdentifierNames() {
		inspect( metadata -> columns( entity( metadata, ImplicitIdOwner.class ).getIdentifier(), "region", "number" ), ImplicitIdOwner.class );
	}

	/// Hibernate physical-naming integration, not a JPA requirement: JPA composes from mapped physical table names.
	@Test
	void associationTableWithPhysicalPrefix() {
		inspect( new org.hibernate.boot.pipeline.internal.source.MappingSources().addManagedClasses( Author.class, Book.class ),
				new ImplicitNamingStrategyMatrixTest.PhysicalStrategy( "p_" ), metadata -> {
			assertThat( collection( metadata, Author.class, "books" ).getCollectionTable().getName() )
					.isEqualTo( "p_p_author_table_p_book_table" );
		} );
	}

	/// JPA IdClass mappings default to the entity field names ([jakarta.persistence.IdClass]).
	@Test
	void implicitIdClassNames() {
		inspect( metadata -> columns( entity( metadata, ImplicitIdClassOwner.class ).getIdentifier(), "region", "number" ), ImplicitIdClassOwner.class );
	}

	/// [jakarta.persistence.CollectionTable] uses embeddable member column defaults for collection elements.
	@Test
	void embeddableElementNames() {
		inspect( metadata -> {
			var addresses = collection( metadata, EmbeddedElements.class, "addresses" );
			assertThat( addresses.getCollectionTable().getName() ).isEqualTo( "EmbeddedElements_addresses" );
			columns( addresses.getElement(), "zip" );
		}, EmbeddedElements.class );
	}

}
