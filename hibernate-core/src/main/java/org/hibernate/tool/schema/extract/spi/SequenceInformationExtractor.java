/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.spi;

import java.sql.SQLException;

/**
 * Because JDBC (at least up to and including Java 7, JDBC 4) still does not have support for obtaining information
 * about sequences from DatabaseMetaData.
 *
 * @author Steve Ebersole
 */
public interface SequenceInformationExtractor {
	/**
	 * Get the information about sequences.
	 *
	 * @param extractionContext Access to resources needed to perform the extraction
	 *
	 * @return The extracted information about existing sequences.
	 *
	 * @throws java.sql.SQLException Don't bother handling SQLExceptions (unless you want to), we will deal with them in the
	 * caller.
	 */
	public Iterable<SequenceInformation> extractMetadata(ExtractionContext extractionContext) throws SQLException;
}
