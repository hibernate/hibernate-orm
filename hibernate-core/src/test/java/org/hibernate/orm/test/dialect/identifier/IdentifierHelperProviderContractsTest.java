/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.identifier;

import java.lang.reflect.Proxy;
import java.util.Set;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.identifier.spi.DelegatingIdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.dialect.identifier.spi.IdentifierSupport;
import org.hibernate.dialect.identifier.spi.KeywordSupport;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy.LOWER;
import static org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy.MIXED;
import static org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy.UPPER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/// Verifies the supported identifier-helper supply and decoration contracts.
///
/// @author Steve Ebersole
public class IdentifierHelperProviderContractsTest {
	@Test
	void forwardsEveryOperationExactlyOnce() {
		final IdentifierHelper delegate = mock( IdentifierHelper.class );
		final TestIdentifierHelper helper = new TestIdentifierHelper( delegate );
		final Identifier input = Identifier.toIdentifier( "input" );
		final Identifier normalized = Identifier.toIdentifier( "normalized", true );
		final Identifier simple = Identifier.toIdentifier( "simple" );
		final Identifier quoted = Identifier.toIdentifier( "quoted", true );
		final Identifier explicit = Identifier.toIdentifier( "explicit", false, false, true );
		final Identifier global = Identifier.toIdentifier( "global", true );

		when( delegate.normalizeQuoting( input ) ).thenReturn( normalized );
		when( delegate.toIdentifier( "simple" ) ).thenReturn( simple );
		when( delegate.toIdentifier( "quoted", true ) ).thenReturn( quoted );
		when( delegate.toIdentifier( "explicit", false, true ) ).thenReturn( explicit );
		when( delegate.applyGlobalQuoting( "global" ) ).thenReturn( global );
		when( delegate.isReservedWord( "select" ) ).thenReturn( true );
		when( delegate.toMetaDataCatalogName( input ) ).thenReturn( "CATALOG" );
		when( delegate.toMetaDataSchemaName( input ) ).thenReturn( "SCHEMA" );
		when( delegate.toMetaDataObjectName( input ) ).thenReturn( "OBJECT" );

		assertThat( helper.exposedDelegate() ).isSameAs( delegate );
		assertThat( helper.normalizeQuoting( input ) ).isSameAs( normalized );
		assertThat( helper.toIdentifier( "simple" ) ).isSameAs( simple );
		assertThat( helper.toIdentifier( "quoted", true ) ).isSameAs( quoted );
		assertThat( helper.toIdentifier( "explicit", false, true ) ).isSameAs( explicit );
		assertThat( helper.applyGlobalQuoting( "global" ) ).isSameAs( global );
		assertThat( helper.isReservedWord( "select" ) ).isTrue();
		assertThat( helper.toMetaDataCatalogName( input ) ).isEqualTo( "CATALOG" );
		assertThat( helper.toMetaDataSchemaName( input ) ).isEqualTo( "SCHEMA" );
		assertThat( helper.toMetaDataObjectName( input ) ).isEqualTo( "OBJECT" );

		verify( delegate ).normalizeQuoting( input );
		verify( delegate ).toIdentifier( "simple" );
		verify( delegate ).toIdentifier( "quoted", true );
		verify( delegate ).toIdentifier( "explicit", false, true );
		verify( delegate ).applyGlobalQuoting( "global" );
		verify( delegate ).isReservedWord( "select" );
		verify( delegate ).toMetaDataCatalogName( input );
		verify( delegate ).toMetaDataSchemaName( input );
		verify( delegate ).toMetaDataObjectName( input );
		verifyNoMoreInteractions( delegate );
	}

	@Test
	void preservesNullExceptionsAndIndependentForwarding() {
		assertThatThrownBy( () -> new TestIdentifierHelper( null ) )
				.isInstanceOf( NullPointerException.class )
				.hasMessage( "delegate" );

		final IdentifierHelper delegate = mock( IdentifierHelper.class );
		final RuntimeException failure = new IllegalStateException( "delegate failure" );
		when( delegate.normalizeQuoting( null ) ).thenReturn( null );
		when( delegate.isReservedWord( "broken" ) ).thenThrow( failure );
		final Identifier direct = Identifier.toIdentifier( "direct" );
		when( delegate.toIdentifier( "direct" ) ).thenReturn( direct );

		final var helper = new DelegatingIdentifierHelper( delegate ) {
			@Override
			public Identifier normalizeQuoting(Identifier identifier) {
				throw new AssertionError( "neighboring operations must not be intercepted" );
			}
		};

		assertThat( new TestIdentifierHelper( delegate ).normalizeQuoting( null ) ).isNull();
		assertThatThrownBy( () -> helper.isReservedWord( "broken" ) ).isSameAs( failure );
		assertThat( helper.toIdentifier( "direct" ) ).isSameAs( direct );
	}

	@Test
	void maintainedHanaRetainsIdentifierCustomization() {
		final IdentifierHelper helper = new HANADialect().buildIdentifierHelper( request() );

		assertThat( helper.toIdentifier( "ordinary" ).isQuoted() ).isFalse();
		assertThat( helper.toIdentifier( "word_123" ).isQuoted() ).isFalse();
		assertThat( helper.toIdentifier( "has space" ).isQuoted() ).isTrue();
		assertThat( helper.toIdentifier( "name:part", false, true ).isQuoted() ).isTrue();
		assertThat( helper.toIdentifier( "explicit", false, true ).isExplicit() ).isTrue();
		assertThat( helper.toIdentifier( "already", true ).isQuoted() ).isTrue();
		assertThat( helper.toIdentifier( null ) ).isNull();
		assertThat( helper.normalizeQuoting( new Identifier( "naïve", false ) ).isQuoted() ).isTrue();
		assertThat( helper.isReservedWord( "select" ) ).isTrue();
		assertThat( helper.applyGlobalQuoting( "integer" ).isQuoted() ).isFalse();
		assertThat( helper.toMetaDataCatalogName( new Identifier( "catalog", false ) ) ).isNull();
		assertThat( helper.toMetaDataSchemaName( new Identifier( "schema", false ) ) ).isEqualTo( "SCHEMA" );
		assertThat( helper.toMetaDataObjectName( new Identifier( "table", false ) ) ).isEqualTo( "TABLE" );
	}

	@Test
	void dialectCaseOverridesTakePrecedenceOverJdbcMetadata() {
		assertIdentifierSupport( new MariaDBDialect(), MIXED, MIXED );
		assertIdentifierSupport( new SybaseDialect(), MIXED, MIXED );
		assertIdentifierSupport( new SQLServerDialect(), MIXED, MIXED );
		assertIdentifierSupport( new PostgreSQLDialect(), MIXED, LOWER );
		assertIdentifierSupport( new CockroachDialect(), MIXED, LOWER );
		assertIdentifierSupport( new MySQLDialect(), MIXED, MIXED );
		assertIdentifierSupport( new SpannerDialect(), UPPER, MIXED );
		assertIdentifierSupport( new HANADialect(), MIXED, UPPER );
	}

	private static void assertIdentifierSupport(IdentifierSupport support, IdentifierCaseStrategy quotedCaseStrategy, IdentifierCaseStrategy unquotedCaseStrategy) {
		assertIdentifier( support, quotedCaseStrategy, Identifier.toIdentifier( "MixedName", true ) );
		assertIdentifier( support, unquotedCaseStrategy, Identifier.toIdentifier( "MixedName", false ) );
	}

	private static void assertIdentifier(IdentifierSupport support, IdentifierCaseStrategy caseStrategy, Identifier quotedIdentifier) {
		final String expected = switch ( caseStrategy ) {
			case LOWER -> "mixedname";
			case MIXED -> "MixedName";
			case UPPER -> "MIXEDNAME";
		};

		final IdentifierHelper helper = support.buildIdentifierHelper( request() );
		assertThat( helper.toMetaDataObjectName( quotedIdentifier ) )
				.as(  "Identifier generation failed for " + support.getClass().getSimpleName() )
				.isEqualTo( expected );
	}

	private static IdentifierHelperBuildRequest request() {
		final JdbcMetadata metadata = (JdbcMetadata) Proxy.newProxyInstance(
				JdbcMetadata.class.getClassLoader(),
				new Class<?>[] { JdbcMetadata.class },
				(proxy, method, arguments) -> switch ( method.getName() ) {
					case "isJdbcMetadataAccessible" -> false;
					case "getUnquotedIdentifierCaseStrategy" -> LOWER;
					case "getQuotedIdentifierCaseStrategy" -> UPPER;
					case "getSqlKeywords" -> Set.of();
					default -> null;
				}
		);
		final KeywordSupport keywordSupport = () -> Set.of( "select" );
		return new IdentifierHelperBuildRequest(
				IdentifierHelperBuilder.from( null ),
				metadata,
				keywordSupport,
				NameQualifierSupport.SCHEMA
		);
	}

	private static final class TestIdentifierHelper extends DelegatingIdentifierHelper {
		private TestIdentifierHelper(IdentifierHelper delegate) {
			super( delegate );
		}

		private IdentifierHelper exposedDelegate() {
			return delegate();
		}
	}
}
