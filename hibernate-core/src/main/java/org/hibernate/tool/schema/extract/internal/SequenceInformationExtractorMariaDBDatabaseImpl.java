/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;

import static java.util.Collections.emptyList;

/**
 * @author Vlad Mihalcea
 * @author Magnus Hagström
 */
public final class SequenceInformationExtractorMariaDBDatabaseImpl implements SequenceInformationExtractor {
	private static final String LOOKUP_SQL =
			"select table_name from information_schema.TABLES where table_schema=database() and table_type='SEQUENCE'";
	public static final SequenceInformationExtractor INSTANCE =
			new SequenceInformationExtractorMariaDBDatabaseImpl( LOOKUP_SQL );

	private static final String SQL_SEQUENCE_QUERY =
			"SELECT '%1$s' as sequence_name, minimum_value, maximum_value, start_value, increment, cache_size FROM %2$s ";
	private static final String UNION_ALL = "UNION ALL ";

	private final String lookupSql;

	private SequenceInformationExtractorMariaDBDatabaseImpl(String lookupSql) {
		this.lookupSql = lookupSql;
	}

	@Override
	public Iterable<SequenceInformation> extractMetadata(ExtractionContext extractionContext) throws SQLException {
		final List<String> sequenceNames = extractionContext.getQueryResults( lookupSql, null, resultSet -> {
			final List<String> sequences = new ArrayList<>();
			while ( resultSet.next() ) {
				sequences.add( resultSet.getString( 1 ) );
			}
			return sequences;
		} );

		if ( sequenceNames.isEmpty() ) {
			return emptyList();
		}

		final var sequenceInfoQueryBuilder = new StringBuilder();
		for ( String sequenceName : sequenceNames ) {
			if ( !sequenceInfoQueryBuilder.isEmpty() ) {
				sequenceInfoQueryBuilder.append( UNION_ALL );
			}
			sequenceInfoQueryBuilder.append(
					String.format( SQL_SEQUENCE_QUERY, sequenceName, Identifier.toIdentifier( sequenceName ) )
			);
		}

		return extractionContext.getQueryResults(
				sequenceInfoQueryBuilder.toString(),
				null,
				resultSet -> {
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
				}
		);
	}
}
