/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

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
			"SELECT '%1$s' as sequence_name, minimum_value, maximum_value, start_value, increment, cache_size FROM %1$s ";

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
				final ResultSet resultSet = statement.executeQuery( lookupSql );
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

			int index = 0;

			try (
					final Statement statement = extractionContext.getJdbcConnection().createStatement();
					final ResultSet resultSet = statement.executeQuery( sequenceInfoQueryBuilder.toString() );
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
