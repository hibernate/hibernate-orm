/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;

import static java.util.Collections.emptyList;

/// Extract legacy MariaDB sequence metadata using the database's two-stage
/// discovery protocol.
///
/// Retain [#INSTANCE] instead of constructing an extractor for each metadata
/// request.
///
/// @author Steve Ebersole
/// @since 8.0
@Internal
public final class SequenceInformationExtractorMariaDBLegacyDatabaseImpl
		implements SequenceInformationExtractor {
	public static final SequenceInformationExtractor INSTANCE =
			new SequenceInformationExtractorMariaDBLegacyDatabaseImpl();

	private static final String LOOKUP_SQL =
			"select table_name from information_schema.TABLES where table_schema=database() and table_type='SEQUENCE'";
	private static final String SEQUENCE_QUERY =
			"SELECT '%1$s' as sequence_name, minimum_value, maximum_value, start_value, increment, cache_size FROM %2$s ";
	private static final String UNION_ALL = "UNION ALL ";

	private SequenceInformationExtractorMariaDBLegacyDatabaseImpl() {
	}

	@Override
	public Iterable<SequenceInformation> extractMetadata(ExtractionContext extractionContext) throws SQLException {
		final List<String> sequenceNames = extractionContext.getQueryResults( LOOKUP_SQL, null, resultSet -> {
			final List<String> names = new ArrayList<>();
			while ( resultSet.next() ) {
				names.add( resultSet.getString( 1 ) );
			}
			return names;
		} );

		if ( sequenceNames.isEmpty() ) {
			return emptyList();
		}

		final var query = new StringBuilder();
		for ( String sequenceName : sequenceNames ) {
			if ( !query.isEmpty() ) {
				query.append( UNION_ALL );
			}
			query.append( String.format(
					SEQUENCE_QUERY,
					sequenceName,
					Identifier.toIdentifier( sequenceName )
			) );
		}

		return extractionContext.getQueryResults( query.toString(), null, resultSet -> {
			final List<SequenceInformation> results = new ArrayList<>();
			final var identifierHelper = extractionContext.getJdbcEnvironment().getIdentifierHelper();
			while ( resultSet.next() ) {
				results.add( SequenceInformationExtractors.information(
						new QualifiedSequenceName(
								null,
								null,
								identifierHelper.toIdentifier( resultSet.getString( 1 ) )
						),
						resultSet.getLong( "start_value" ),
						resultSet.getLong( "minimum_value" ),
						resultSet.getLong( "maximum_value" ),
						resultSet.getLong( "increment" )
				) );
			}
			return results;
		} );
	}
}
