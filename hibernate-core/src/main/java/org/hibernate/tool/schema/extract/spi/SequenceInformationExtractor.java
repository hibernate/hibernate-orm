/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	 * @throws SQLException Don't bother handling SQLExceptions (unless you want to), we will deal with them in the
	 * caller.
	 */
	Iterable<SequenceInformation> extractMetadata(ExtractionContext extractionContext) throws SQLException;
}
