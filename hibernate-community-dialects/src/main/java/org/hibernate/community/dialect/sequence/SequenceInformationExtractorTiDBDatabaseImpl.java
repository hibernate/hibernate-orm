/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationImpl;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SequenceInformationExtractorTiDBDatabaseImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorTiDBDatabaseImpl INSTANCE = new SequenceInformationExtractorTiDBDatabaseImpl();

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

	@Override
	public Iterable<SequenceInformation> extractMetadata(ExtractionContext extractionContext) throws SQLException {
		final String lookupSql = extractionContext.getJdbcEnvironment().getDialect().getQuerySequencesString();

		// *should* never happen, but to be safe in the interest of performance...
		if (lookupSql == null) {
			return SequenceInformationExtractorNoOpImpl.INSTANCE.extractMetadata(extractionContext);
		}

		final IdentifierHelper identifierHelper = extractionContext.getJdbcEnvironment().getIdentifierHelper();

		final List<SequenceInformation> sequenceInformationList = new ArrayList<>();
		final List<String> sequenceNames = new ArrayList<>();

		try (
				final Statement statement = extractionContext.getJdbcConnection().createStatement();
				final ResultSet resultSet = statement.executeQuery( lookupSql )
		) {
			while ( resultSet.next() ) {
				sequenceNames.add( resultSetSequenceName( resultSet ) );
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
					SequenceInformation sequenceInformation = new SequenceInformationImpl(
							new QualifiedSequenceName(
									null,
									null,
									identifierHelper.toIdentifier(
											resultSetSequenceName(resultSet)
									)
							),
							resultSetStartValueSize(resultSet),
							resultSetMinValue(resultSet),
							resultSetMaxValue(resultSet),
							resultSetIncrementValue(resultSet)
					);

					sequenceInformationList.add(sequenceInformation);
				}

			}
		}

		return sequenceInformationList;
	}

	protected String sequenceNameColumn() {
		return "sequence_name";
	}

	protected String sequenceIncrementColumn() {
		return "increment";
	}

	protected String resultSetSequenceName(ResultSet resultSet) throws SQLException {
		return resultSet.getString( sequenceNameColumn() );
	}

	protected Number resultSetIncrementValue(ResultSet resultSet) throws SQLException {
		String column = sequenceIncrementColumn();
		return column != null ? resultSet.getLong( column ) : null;
	}
}
