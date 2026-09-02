/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.spi;

import org.hibernate.SPI;
import org.hibernate.tool.schema.extract.internal.InformationExtractorJdbcDatabaseMetaDataImpl;
import org.hibernate.tool.schema.extract.internal.InformationExtractorMySQLImpl;
import org.hibernate.tool.schema.extract.internal.InformationExtractorOracleImpl;
import org.hibernate.tool.schema.extract.internal.InformationExtractorPostgreSQLImpl;

import static org.hibernate.SPI.Role.USE;

/// Creates context-bound stock information extractors without exposing
/// Hibernate's concrete extractor implementations.
///
/// Create a new extractor for each [ExtractionContext]. Use [#jdbcMetadata]
/// for standard JDBC metadata and select a database profile only when its
/// complete bulk-extraction behavior matches the provider.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public final class InformationExtractors {
	private InformationExtractors() {
	}

	/// Create a standard JDBC metadata extractor using imported keys only.
	public static InformationExtractor jdbcMetadata(ExtractionContext extractionContext) {
		return jdbcMetadata( extractionContext, ForeignKeyMetadataPolicy.importedKeysOnly() );
	}

	/// Create a standard JDBC metadata extractor using `foreignKeyMetadataPolicy`.
	public static InformationExtractor jdbcMetadata(
			ExtractionContext extractionContext,
			ForeignKeyMetadataPolicy foreignKeyMetadataPolicy) {
		return new InformationExtractorJdbcDatabaseMetaDataImpl(
				requireContext( extractionContext ),
				requirePolicy( foreignKeyMetadataPolicy )
		);
	}

	/// Create the MySQL bulk-metadata profile.
	public static InformationExtractor mysql(ExtractionContext extractionContext) {
		return new InformationExtractorMySQLImpl( requireContext( extractionContext ) );
	}

	/// Create the PostgreSQL bulk-metadata profile.
	public static InformationExtractor postgresql(ExtractionContext extractionContext) {
		return new InformationExtractorPostgreSQLImpl( requireContext( extractionContext ) );
	}

	/// Create the Oracle bulk-metadata profile.
	public static InformationExtractor oracle(ExtractionContext extractionContext) {
		return new InformationExtractorOracleImpl( requireContext( extractionContext ) );
	}

	private static ExtractionContext requireContext(ExtractionContext extractionContext) {
		if ( extractionContext == null ) {
			throw new IllegalArgumentException( "extractionContext must not be null" );
		}
		return extractionContext;
	}

	private static ForeignKeyMetadataPolicy requirePolicy(ForeignKeyMetadataPolicy policy) {
		if ( policy == null ) {
			throw new IllegalArgumentException( "foreignKeyMetadataPolicy must not be null" );
		}
		return policy;
	}
}
