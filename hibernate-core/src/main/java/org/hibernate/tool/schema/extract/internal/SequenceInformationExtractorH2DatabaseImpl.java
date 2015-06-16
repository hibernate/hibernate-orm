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
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

/**
 * Temporary implementation that works for H2.
 *
 * @author Steve Ebersole
 */
public class SequenceInformationExtractorH2DatabaseImpl implements SequenceInformationExtractor {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorH2DatabaseImpl INSTANCE = new SequenceInformationExtractorH2DatabaseImpl();

	@Override
	public Iterable<SequenceInformation> extractMetadata(ExtractionContext extractionContext) throws SQLException {
		final IdentifierHelper identifierHelper = extractionContext.getJdbcEnvironment().getIdentifierHelper();
		final Statement statement = extractionContext.getJdbcConnection().createStatement();
		try {
			ResultSet resultSet = statement.executeQuery(
					"select SEQUENCE_CATALOG, SEQUENCE_SCHEMA, SEQUENCE_NAME, INCREMENT " +
							"from information_schema.sequences"
			);
			try {
				final List<SequenceInformation> sequenceInformationList = new ArrayList<SequenceInformation>();
				while ( resultSet.next() ) {
					sequenceInformationList.add(
							new SequenceInformationImpl(
									new QualifiedSequenceName(
											identifierHelper.toIdentifier(
													resultSet.getString( "SEQUENCE_CATALOG" )
											),
											identifierHelper.toIdentifier(
													resultSet.getString( "SEQUENCE_SCHEMA" )
											),
											identifierHelper.toIdentifier(
													resultSet.getString( "SEQUENCE_NAME" )
											)
									),
									resultSet.getInt( "INCREMENT" )
							)
					);
				}
				return sequenceInformationList;
			}
			finally {
				try {
					resultSet.close();
				}
				catch (SQLException ignore) {
				}
			}
		}
		finally {
			try {
				statement.close();
			}
			catch (SQLException ignore) {
			}
		}
	}
}
