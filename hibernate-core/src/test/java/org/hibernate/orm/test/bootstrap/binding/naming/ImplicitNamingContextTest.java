/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.naming;

import java.util.List;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;
import org.hibernate.boot.model.naming.NamingHelper;
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
	void constraintHashingUsesContextCharsetAndSubclassHook() {
		final var buildingContext = mock( MetadataBuildingContext.class, RETURNS_DEEP_STUBS );
		when( buildingContext.getBuildingPlan().getSchemaCharset() ).thenReturn( "ISO-8859-1" );
		final var context = ImplicitNamingContextImpl.from( buildingContext );
		final var source = mock( ImplicitUniqueKeyNameSource.class );
		when( source.getNamingContext() ).thenReturn( context );
		when( source.kind() ).thenReturn( org.hibernate.boot.model.naming.ImplicitConstraintNameSource.Kind.UNIQUE_KEY );
		when( source.getTableName() ).thenReturn( Identifier.toIdentifier( "café" ) );
		when( source.getColumnNames() ).thenReturn( List.of(
				Identifier.toIdentifier( "col1" ), Identifier.toIdentifier( "col2" ), Identifier.toIdentifier( "col3" ) ) );
		final var strategy = new TrackingStrategy();
		assertThat( strategy.constraintName( source ) ).isEqualTo( "UK1pitt5gtytwpy6ea02o7l5men" );
		assertThat( strategy.hashContext ).isSameAs( context );
	}

	private static class TrackingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {
		private ImplicitNamingContext identifierContext;
		private ImplicitNamingContext hashContext;

		@Override
		protected Identifier toIdentifier(String name, ImplicitNamingContext context) {
			identifierContext = context;
			return super.toIdentifier( name, context );
		}

		@Override
		protected NamingHelper namingHelper(ImplicitNamingContext context) {
			hashContext = context;
			return super.namingHelper( context );
		}

		String constraintName(ImplicitUniqueKeyNameSource source) {
			return generateConstraintNameString( source );
		}
	}
}
