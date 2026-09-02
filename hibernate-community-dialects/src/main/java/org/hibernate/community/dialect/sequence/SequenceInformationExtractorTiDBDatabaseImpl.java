/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;

/// Extract TiDB sequence metadata using its two-stage discovery protocol.
///
/// Retain [#INSTANCE] instead of constructing an extractor for each metadata
/// request.
///
/// @author Steve Ebersole
/// @since 8.0
@Internal
public final class SequenceInformationExtractorTiDBDatabaseImpl implements SequenceInformationExtractor {
	private static final String LOOKUP_SQL =
			"SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = database()";
	public static final SequenceInformationExtractor INSTANCE =
			new SequenceInformationExtractorTiDBDatabaseImpl( LOOKUP_SQL );

	// SQL to get metadata from individual sequence
	private static final String SQL_SEQUENCE_QUERY = "SELECT " +
					"'%1$s' AS sequence_name, " +
					"MIN_VALUE AS minimum_value, " +
					"MAX_VALUE AS maximum_value, " +
					"START AS start_value, " +
					"INCREMENT AS increment " +
					"FROM information_schema.sequences WHERE sequence_name = '%1$s' AND sequence_schema = database()";

	private static final String UNION_ALL =
			"UNION ALL ";
	private final String lookupSql;

	private SequenceInformationExtractorTiDBDatabaseImpl(String lookupSql) {
		this.lookupSql = lookupSql;
	}

	@Override
	public Iterable<SequenceInformation> extractMetadata(ExtractionContext extractionContext) throws SQLException {
		final var identifierHelper = extractionContext.getJdbcEnvironment().getIdentifierHelper();

		final List<SequenceInformation> sequenceInformationList = new ArrayList<>();
		final List<String> sequenceNames = new ArrayList<>();

		try (
				final Statement statement = extractionContext.getJdbcConnection().createStatement();
				final ResultSet resultSet = statement.executeQuery( lookupSql )
		) {
			while ( resultSet.next() ) {
				sequenceNames.add( resultSet.getString( "sequence_name" ) );
			}
		}

		if ( !sequenceNames.isEmpty() ) {
			StringBuilder sequenceInfoQueryBuilder = new StringBuilder();

			for ( String sequenceName : sequenceNames ) {
				if ( sequenceInfoQueryBuilder.length() > 0 ) {
					sequenceInfoQueryBuilder.append( UNION_ALL );
				}
				sequenceInfoQueryBuilder.append( String.format( SQL_SEQUENCE_QUERY, sequenceName ) );
			}

			try (
					final Statement statement = extractionContext.getJdbcConnection().createStatement();
					final ResultSet resultSet = statement.executeQuery( sequenceInfoQueryBuilder.toString() )
			) {

				while ( resultSet.next() ) {
					sequenceInformationList.add( SequenceInformationExtractors.information(
							new QualifiedSequenceName(
									null,
									null,
									identifierHelper.toIdentifier( resultSet.getString( "sequence_name" ) )
							),
							resultSet.getLong( "start_value" ),
							resultSet.getLong( "minimum_value" ),
							resultSet.getLong( "maximum_value" ),
							resultSet.getLong( "increment" )
					) );
				}

			}
		}

		return sequenceInformationList;
	}
}
