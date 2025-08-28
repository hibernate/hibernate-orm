/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

/**
 * @author Steve Ebersole
 */
public class SequenceInformationExtractorLegacyImpl implements SequenceInformationExtractor {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorLegacyImpl INSTANCE = new SequenceInformationExtractorLegacyImpl();

	@Override
	public Iterable<SequenceInformation> extractMetadata(ExtractionContext extractionContext) throws SQLException {
		final String lookupSql = extractionContext.getJdbcEnvironment().getDialect().getQuerySequencesString();

		// *should* never happen, but to be safe in the interest of performance...
		if ( lookupSql == null ) {
			return SequenceInformationExtractorNoOpImpl.INSTANCE.extractMetadata( extractionContext );
		}

		return extractionContext.getQueryResults(
				lookupSql,
				null,
				(ExtractionContext.ResultSetProcessor<Iterable<SequenceInformation>>) resultSet -> {
					final IdentifierHelper identifierHelper = extractionContext.getJdbcEnvironment()
							.getIdentifierHelper();
					final List<SequenceInformation> sequenceInformationList = new ArrayList<>();
					while ( resultSet.next() ) {
						sequenceInformationList.add(
								new SequenceInformationImpl(
										new QualifiedSequenceName(
												identifierHelper.toIdentifier( resultSetCatalogName( resultSet ) ),
												identifierHelper.toIdentifier( resultSetSchemaName( resultSet ) ),
												identifierHelper.toIdentifier( resultSetSequenceName( resultSet ) )
										),
										resultSetStartValueSize( resultSet ),
										resultSetMinValue( resultSet ),
										resultSetMaxValue( resultSet ),
										resultSetIncrementValue( resultSet )
								)
						);
					}
					return sequenceInformationList;
				}
		);
	}

	protected String sequenceNameColumn() {
		return "sequence_name";
	}

	protected String sequenceCatalogColumn() {
		return "sequence_catalog";
	}

	protected String sequenceSchemaColumn() {
		return "sequence_schema";
	}

	protected String sequenceStartValueColumn() {
		return "start_value";
	}

	protected String sequenceMinValueColumn() {
		return "minimum_value";
	}

	protected String sequenceMaxValueColumn() {
		return "maximum_value";
	}

	protected String sequenceIncrementColumn() {
		return "increment";
	}

	protected String resultSetSequenceName(ResultSet resultSet) throws SQLException {
		return resultSet.getString( sequenceNameColumn() );
	}

	protected String resultSetCatalogName(ResultSet resultSet) throws SQLException {
		String column = sequenceCatalogColumn();
		return column != null ? resultSet.getString( column ) : null;
	}

	protected String resultSetSchemaName(ResultSet resultSet) throws SQLException {
		String column = sequenceSchemaColumn();
		return column != null ? resultSet.getString( column ) : null;
	}

	protected Number resultSetStartValueSize(ResultSet resultSet) throws SQLException {
		String column = sequenceStartValueColumn();
		return column != null ? resultSet.getLong( column ) : null;
	}

	protected Number resultSetMinValue(ResultSet resultSet) throws SQLException {
		String column = sequenceMinValueColumn();
		return column != null ? resultSet.getLong( column ) : null;
	}

	protected Number resultSetMaxValue(ResultSet resultSet) throws SQLException {
		String column = sequenceMaxValueColumn();
		return column != null ? resultSet.getLong( column ) : null;
	}

	protected Number resultSetIncrementValue(ResultSet resultSet) throws SQLException {
		String column = sequenceIncrementColumn();
		return column != null ? resultSet.getLong( column ) : null;
	}
}
