/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.naming;

import java.util.List;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.spi.IndexNamingInput;
import org.hibernate.boot.model.naming.spi.IndexTermNamingInput;
import org.hibernate.boot.model.naming.spi.NamedTableNamingInput;
import org.hibernate.boot.model.naming.spi.NamingNamePair;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.boot.model.naming.internal.ImplicitNamingContextImpl;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.spi.MetadataBuildingContext;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Regression coverage for mapping-local defaults and migrated subclass hooks.
///
/// @author Steve Ebersole
class ImplicitNamingContextTest {
	@Test
	void mappingDefaultsAndIdentifierHelperArePreserved() {
		final var buildingContext = mock( MetadataBuildingContext.class, RETURNS_DEEP_STUBS );
		final var defaults = buildingContext.getEffectiveDefaults();
		when( defaults.getDefaultDiscriminatorColumnName() ).thenReturn( "kind" );
		when( defaults.isDefaultQuoteIdentifiers() ).thenReturn( true );
		final var helper = buildingContext.getMetadataCollector().getDatabase().getJdbcEnvironment().getIdentifierHelper();
		when( helper.toIdentifier( "kind" ) ).thenReturn( Identifier.toIdentifier( "kind", true ) );
		final var context = ImplicitNamingContextImpl.from( buildingContext );
		assertThat( context.getNamingDefaults() ).isSameAs( defaults );
		assertThat( context.getNamingDefaults().isDefaultQuoteIdentifiers() ).isTrue();
		assertThat( context.getIdentifierHelper() ).isSameAs( helper );
		final var strategy = new TrackingStrategy();
		assertThat( strategy.toIdentifier( "kind", context ) ).isEqualTo( Identifier.toIdentifier( "kind", true ) );
		assertThat( strategy.identifierContext ).isSameAs( context );

		final var otherMapping = mock( MetadataBuildingContext.class, RETURNS_DEEP_STUBS );
		when( otherMapping.getEffectiveDefaults().getDefaultDiscriminatorColumnName() ).thenReturn( "other_kind" );
		assertThat( ImplicitNamingContextImpl.from( otherMapping ).getNamingDefaults().getDefaultDiscriminatorColumnName() )
				.isEqualTo( "other_kind" );
		assertThat( context.getNamingDefaults().getDefaultDiscriminatorColumnName() ).isEqualTo( "kind" );
	}

	@Test
	void indexHashingUsesContextCharset() {
		final var buildingContext = mock( MetadataBuildingContext.class, RETURNS_DEEP_STUBS );
		when( buildingContext.getBuildingPlan().getSchemaCharset() ).thenReturn( "ISO-8859-1" );
		final var context = ImplicitNamingContextImpl.from( buildingContext );
		final var input = new IndexNamingInput( new NamedTableNamingInput( new NamingNamePair(
				new LogicalName( "café", false, true ), mock( PhysicalName.class ) ) ),
				List.of( "col1", "col2", "col3" ).stream().<IndexTermNamingInput>map( name ->
						new IndexTermNamingInput.ColumnTerm( new NamingNamePair(
								new LogicalName( name, false, true ), mock( PhysicalName.class ) ), name,
								IndexTermNamingInput.Order.UNSPECIFIED ) ).toList(), false, null, null );
		assertThat( new TrackingStrategy().determineIndexName( input, context ).getText() ).isEqualTo( "IDX1pitt5gtytwpy6ea02o7l5men" );
	}

	private static class TrackingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {
		private ImplicitNamingContext identifierContext;

		@Override
		protected Identifier toIdentifier(String name, ImplicitNamingContext context) {
			identifierContext = context;
			return super.toIdentifier( name, context );
		}

	}
}
