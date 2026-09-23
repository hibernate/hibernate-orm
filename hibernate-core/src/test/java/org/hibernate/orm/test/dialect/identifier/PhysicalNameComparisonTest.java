/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.identifier;

import java.util.HashSet;
import java.util.Locale;
import java.util.TreeSet;

import org.hibernate.boot.model.naming.DatabaseIdentifier;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.boot.model.relational.internal.PersistenceUnitJdbcEnvironment;
import org.hibernate.dialect.identifier.spi.DelegatingIdentifierHelper;
import org.hibernate.relational.naming.spi.IdentifierComparisonPolicy;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.dialect.identifier.spi.IdentifierSupport;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Exercises name identity, provider policy configuration, and factory-independent equality.
///
/// @author Steve Ebersole
class PhysicalNameComparisonTest {
	@Test
	void logicalOriginDoesNotAffectIdentityButQuotingDoes() {
		final var explicit = new LogicalName( "ssn", false, true );
		final var implicit = new LogicalName( "SSN", false, false );
		assertThat( explicit ).isEqualTo( implicit );
		assertThat( explicit.isExplicit() ).isTrue();
		assertThat( implicit.isExplicit() ).isFalse();
		assertThat( explicit.getText() ).isEqualTo( "ssn" );
		assertThat( implicit.getText() ).isEqualTo( "SSN" );
		assertThat( explicit.compareTo( implicit ) ).isZero();
		assertThat( explicit.hashCode() ).isEqualTo( implicit.hashCode() );
		final var quoted = new LogicalName( "ssn", true, true );
		final var quotedUpper = new LogicalName( "SSN", true, true );
		assertThat( explicit ).isNotEqualTo( quoted );
		assertThat( quoted ).isNotEqualTo( quotedUpper );
		assertThat( new HashSet<>( java.util.List.of( explicit, implicit, quoted, quotedUpper ) ) ).hasSize( 3 );
		assertThat( new TreeSet<>( java.util.List.of( explicit, implicit, quoted, quotedUpper ) ) ).hasSize( 3 );
	}

	@Test
	void logicalCaseKeysAreLocaleIndependentAndConsistent() {
		final var upper = new LogicalName( "ÉCOLE", false, true );
		final var lower = new LogicalName( "école", false, false );
		assertThat( upper ).isEqualTo( lower );
		assertThat( upper.hashCode() ).isEqualTo( lower.hashCode() );
		assertThat( upper.compareTo( lower ) ).isZero();
		// Lowercase-key semantics deliberately do not use equalsIgnoreCase().
		assertThat( new LogicalName( "İ", false, true ) )
				.isNotEqualTo( new LogicalName( "i", false, true ) );
	}

	@Test
	void upperFoldingUnifiesUnquotedAndQuotedDatabaseSpellings() {
		final var helper = helper( IdentifierCaseStrategy.UPPER, IdentifierCaseStrategy.MIXED );
		final var factory = helper.getPhysicalNameFactory();
		assertThat( helper.getPhysicalNameFactory() ).isSameAs( factory );
		final var lower = factory.create( "ssn", false );
		final var upper = factory.create( "SSN", false );
		final var quotedUpper = factory.create( "SSN", true );
		final var quotedLower = factory.create( "ssn", true );
		assertThat( lower ).isEqualTo( upper ).isEqualTo( quotedUpper ).isNotEqualTo( quotedLower );
		assertThat( lower.compareTo( quotedUpper ) ).isZero();
		assertThat( lower.hashCode() ).isEqualTo( quotedUpper.hashCode() );
		assertThat( new HashSet<>( java.util.List.of( lower, upper, quotedUpper, quotedLower ) ) ).hasSize( 2 );
		assertThat( new TreeSet<>( java.util.List.of( lower, upper, quotedUpper, quotedLower ) ) ).hasSize( 2 );
		assertThat( lower.getText() ).isEqualTo( "ssn" );
		assertThat( new IdentifierSupport() {}.render( quotedUpper ) ).isEqualTo( "\"SSN\"" );
		assertThat( helper.getComparisonPolicy().matchesDatabaseName( lower, "SSN" ) ).isTrue();
		assertThat( helper.getComparisonPolicy().matchesDatabaseName( lower, "ssn" ) ).isFalse();
		assertThat( helper.toMetaDataObjectName( new Identifier( "ssn", false ) ) ).isEqualTo( "SSN" );
		assertThat( helper.toMetaDataObjectName( DatabaseIdentifier.toIdentifier( "ssn" ) ) ).isEqualTo( "ssn" );
	}

	@Test
	void lowerAndMixedCasePoliciesRemainDistinct() {
		final var lower = helper( IdentifierCaseStrategy.LOWER, IdentifierCaseStrategy.MIXED ).getPhysicalNameFactory();
		assertThat( lower.create( "SSN", false ) ).isEqualTo( lower.create( "ssn", true ) );
		assertThat( lower.create( "SSN", true ) ).isNotEqualTo( lower.create( "ssn", true ) );
		final var mixed = helper( IdentifierCaseStrategy.MIXED, IdentifierCaseStrategy.MIXED ).getPhysicalNameFactory();
		assertThat( mixed.create( "SSN", false ) ).isNotEqualTo( mixed.create( "ssn", false ) );
	}

	@Test
	void supportOverridesJdbcDefaultsBeforeFactoryConstruction() {
		final var request = request();
		final IdentifierSupport support = new IdentifierSupport() {
			@Override
			public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest input) {
				IdentifierSupport.super.buildIdentifierHelper( input );
				input.builder().setUnquotedCaseStrategy( IdentifierCaseStrategy.LOWER );
				return input.builder().build();
			}
		};
		final var helper = support.buildIdentifierHelper( request );
		final var factory = helper.getPhysicalNameFactory();
		assertThat( factory.create( "SSN", false ) ).isEqualTo( factory.create( "ssn", true ) );
		assertThat( helper.toMetaDataObjectName( new Identifier( "SSN", false ) ) ).isEqualTo( "ssn" );
		final var decorated = new DelegatingIdentifierHelper( helper ) {};
		assertThat( decorated.getPhysicalNameFactory() ).isSameAs( factory );
		assertThat( decorated.getComparisonPolicy() ).isSameAs( helper.getComparisonPolicy() );
	}

	@Test
	void persistenceUnitQuotingDoesNotCreateAnotherComparisonDomain() {
		final var helper = helper( IdentifierCaseStrategy.UPPER, IdentifierCaseStrategy.MIXED );
		final var environment = mock( JdbcEnvironment.class );
		when( environment.getIdentifierHelper() ).thenReturn( helper );
		final var scoped = new PersistenceUnitJdbcEnvironment( environment, () -> true, false )
				.getIdentifierHelper();
		assertThat( scoped.toIdentifier( "ssn" ).isQuoted() ).isTrue();
		assertThat( scoped.getComparisonPolicy() ).isSameAs( helper.getComparisonPolicy() );
		assertThat( scoped.getPhysicalNameFactory() ).isSameAs( helper.getPhysicalNameFactory() );
	}

	@Test
	void providerCanDistinguishCaseInsensitiveComparisonFromStoredSpelling() {
		final var request = request();
		final IdentifierSupport support = new IdentifierSupport() {
			@Override
			public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest input) {
				input.builder().setComparisonPolicy( new IdentifierComparisonPolicy() {
					@Override
					public String toDatabaseName(String text, boolean quoted) { return text; }
					@Override
					public String databaseNameComparisonKey(String text) { return text.toLowerCase( Locale.ROOT ); }
				} );
				return IdentifierSupport.super.buildIdentifierHelper( input );
			}
		};
		final var helper = support.buildIdentifierHelper( request );
		final var factory = helper.getPhysicalNameFactory();
		final var name = factory.create( "MiXeD", true );
		assertThat( name ).isEqualTo( factory.create( "mixed", false ) );
		assertThat( helper.toMetaDataObjectName( new Identifier( "MiXeD", true ) ) ).isEqualTo( "MiXeD" );
		assertThat( helper.getComparisonPolicy().matchesDatabaseName( name, "MIXED" ) ).isTrue();
	}

	@Test
	void factoryIdentityDoesNotAffectNameIdentity() {
		final var helper = helper( IdentifierCaseStrategy.UPPER, IdentifierCaseStrategy.MIXED );
		final var first = helper.getPhysicalNameFactory().create( "ssn", false );
		final var second = new PhysicalName.Factory( helper.getComparisonPolicy() ).create( "SSN", true );
		assertThat( first ).isEqualTo( second );
		assertThat( second ).isEqualTo( first );
		assertThat( first.hashCode() ).isEqualTo( second.hashCode() );
		assertThat( first.compareTo( second ) ).isZero();
		assertThat( second.compareTo( first ) ).isZero();
		assertThat( new HashSet<>( java.util.List.of( first, second ) ) ).hasSize( 1 );
		assertThat( new TreeSet<>( java.util.List.of( first, second ) ) ).hasSize( 1 );
	}

	@Test
	void sharedNamesRejectInvalidTextWithoutBootIdentifierConstruction() {
		final var factory = helper( IdentifierCaseStrategy.UPPER, IdentifierCaseStrategy.MIXED ).getPhysicalNameFactory();
		for ( String text : new String[] { null, "", "`name`", "\"name\"", "[name]" } ) {
			assertThatThrownBy( () -> new LogicalName( text, false, true ) )
					.isInstanceOf( IllegalArgumentException.class );
			assertThatThrownBy( () -> factory.create( text, false ) )
					.isInstanceOf( IllegalArgumentException.class );
		}
	}

	@Test
	void renderingUsesIdentifierSupportCustomization() {
		final var factory = helper( IdentifierCaseStrategy.UPPER, IdentifierCaseStrategy.MIXED ).getPhysicalNameFactory();
		final IdentifierSupport support = new IdentifierSupport() {
			@Override
			public String toQuotedIdentifier(String text) {
				return "[" + text + "]";
			}
		};
		assertThat( support.render( factory.create( "MixedName", true ) ) ).isEqualTo( "[MixedName]" );
		assertThat( support.render( factory.create( "MixedName", false ) ) ).isEqualTo( "MixedName" );
	}

	private static IdentifierHelper helper(IdentifierCaseStrategy unquoted, IdentifierCaseStrategy quoted) {
		final var builder = IdentifierHelperBuilder.from( null );
		builder.setUnquotedCaseStrategy( unquoted );
		builder.setQuotedCaseStrategy( quoted );
		return builder.build();
	}

	private static IdentifierHelperBuildRequest request() {
		final var metadata = mock( JdbcMetadata.class );
		when( metadata.getUnquotedIdentifierCaseStrategy() ).thenReturn( IdentifierCaseStrategy.UPPER );
		when( metadata.getQuotedIdentifierCaseStrategy() ).thenReturn( IdentifierCaseStrategy.MIXED );
		when( metadata.getSqlKeywords() ).thenReturn( java.util.Set.of() );
		return new IdentifierHelperBuildRequest(
				IdentifierHelperBuilder.from( null ), metadata, () -> java.util.Set.of(), NameQualifierSupport.BOTH );
	}
}
