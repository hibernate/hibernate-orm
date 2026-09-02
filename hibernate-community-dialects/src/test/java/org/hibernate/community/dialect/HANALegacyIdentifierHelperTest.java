/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.lang.reflect.Proxy;
import java.util.Set;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.identifier.spi.DelegatingIdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.dialect.identifier.spi.KeywordSupport;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies HANA Legacy identifier customization through the supported
/// delegating helper contract.
///
/// @author Steve Ebersole
public class HANALegacyIdentifierHelperTest {
	@Test
	void decoratesConstructionAndNormalizationWithoutChangingConfiguredBehavior() {
		final IdentifierHelper helper = new HANALegacyDialect().buildIdentifierHelper( request() );

		assertThat( helper ).isInstanceOf( DelegatingIdentifierHelper.class );
		assertThat( helper.toIdentifier( "ordinary" ).isQuoted() ).isFalse();
		assertThat( helper.toIdentifier( "word_123" ).isQuoted() ).isFalse();
		assertThat( helper.toIdentifier( "has space" ).isQuoted() ).isTrue();
		assertThat( helper.toIdentifier( "name:part", false, true ).isQuoted() ).isTrue();
		assertThat( helper.toIdentifier( "explicit", false, true ).isExplicit() ).isTrue();
		assertThat( helper.toIdentifier( "already", true ).isQuoted() ).isTrue();
		assertThat( helper.toIdentifier( null ) ).isNull();
		assertThat( helper.normalizeQuoting( new Identifier( "naïve", false ) ).isQuoted() ).isTrue();
	}

	@Test
	void forwardsKeywordGlobalQuotingAndMetadataOperations() {
		final IdentifierHelper helper = new HANALegacyDialect().buildIdentifierHelper( request() );

		assertThat( helper.isReservedWord( "select" ) ).isTrue();
		assertThat( helper.applyGlobalQuoting( "integer" ).isQuoted() ).isFalse();
		assertThat( helper.toMetaDataCatalogName( new Identifier( "catalog", false ) ) ).isNull();
		assertThat( helper.toMetaDataSchemaName( new Identifier( "schema", false ) ) ).isEqualTo( "schema" );
		assertThat( helper.toMetaDataObjectName( new Identifier( "table", false ) ) ).isEqualTo( "table" );
	}

	private static IdentifierHelperBuildRequest request() {
		final JdbcMetadata metadata = (JdbcMetadata) Proxy.newProxyInstance(
				JdbcMetadata.class.getClassLoader(),
				new Class<?>[] { JdbcMetadata.class },
				(proxy, method, arguments) -> switch ( method.getName() ) {
					case "isJdbcMetadataAccessible" -> false;
					case "getUnquotedIdentifierCaseStrategy" -> IdentifierCaseStrategy.LOWER;
					case "getQuotedIdentifierCaseStrategy" -> IdentifierCaseStrategy.UPPER;
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
}
