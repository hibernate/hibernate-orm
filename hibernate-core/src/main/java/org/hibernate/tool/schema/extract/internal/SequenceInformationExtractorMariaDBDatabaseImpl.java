/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

import static java.util.Collections.emptyList;

/**
 * @author Vlad Mihalcea, Magnus Hagström
 */
public class SequenceInformationExtractorMariaDBDatabaseImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorMariaDBDatabaseImpl INSTANCE = new SequenceInformationExtractorMariaDBDatabaseImpl();

	// SQL to get metadata from individual sequence
	private static final String SQL_SEQUENCE_QUERY =
			"SELECT '%1$s' as sequence_name, minimum_value, maximum_value, start_value, increment, cache_size FROM %2$s ";

	private static final String UNION_ALL =
			"UNION ALL ";

	@Override
	public Iterable<SequenceInformation> extractMetadata(ExtractionContext extractionContext) throws SQLException {
		final String lookupSql = extractionContext.getJdbcEnvironment().getDialect().getQuerySequencesString();
		// *should* never happen, but to be safe in the interest of performance...
		if ( lookupSql == null ) {
			return SequenceInformationExtractorNoOpImpl.INSTANCE.extractMetadata(extractionContext);
		}

		final List<String> sequenceNames =
				extractionContext.getQueryResults( lookupSql, null, resultSet -> {
					final List<String> sequences = new ArrayList<>();
					while ( resultSet.next() ) {
						sequences.add( resultSetSequenceName( resultSet ) );
					}
					return sequences;
				});

		if ( sequenceNames.isEmpty() ) {
			return emptyList();
		}
		else {
			final var sequenceInfoQueryBuilder = new StringBuilder();
			for ( String sequenceName : sequenceNames ) {
				if ( !sequenceInfoQueryBuilder.isEmpty() ) {
					sequenceInfoQueryBuilder.append( UNION_ALL );
				}
				sequenceInfoQueryBuilder.append(
						String.format( SQL_SEQUENCE_QUERY, sequenceName,
								Identifier.toIdentifier( sequenceName ) ) );
			}
			return extractionContext.getQueryResults(
					sequenceInfoQueryBuilder.toString(),
					null,
					resultSet -> {
						final List<SequenceInformation> sequenceInformationList = new ArrayList<>();
						final var identifierHelper =
								extractionContext.getJdbcEnvironment().getIdentifierHelper();
						while ( resultSet.next() ) {
							sequenceInformationList.add( new SequenceInformationImpl(
									new QualifiedSequenceName( null, null,
											identifierHelper.toIdentifier( resultSetSequenceName( resultSet ) ) ),
									resultSetStartValueSize( resultSet ),
									resultSetMinValue( resultSet ),
									resultSetMaxValue( resultSet ),
									resultSetIncrementValue( resultSet )
							) );
						}
						return sequenceInformationList;
					});
		}
	}

	protected String resultSetSequenceName(ResultSet resultSet) throws SQLException {
		return resultSet.getString(1);
	}

	@Override
	protected String sequenceCatalogColumn() {
		return null;
	}

	@Override
	protected String sequenceSchemaColumn() {
		return null;
	}

}
