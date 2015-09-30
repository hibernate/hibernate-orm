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

		final IdentifierHelper identifierHelper = extractionContext.getJdbcEnvironment().getIdentifierHelper();
		final Statement statement = extractionContext.getJdbcConnection().createStatement();
		try {
			final ResultSet resultSet = statement.executeQuery( lookupSql );
			try {
				final List<SequenceInformation> sequenceInformationList = new ArrayList<SequenceInformation>();
				while ( resultSet.next() ) {
					sequenceInformationList.add(
							new SequenceInformationImpl(
									new QualifiedSequenceName(
											null,
											null,
											identifierHelper.toIdentifier(
													resultSet.getString( 1 )
											)
									),
									-1
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
