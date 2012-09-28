/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.tool.schema.extract.spi;

import java.sql.SQLException;

/**
 * Because JDBC (at least up to an including Java 7, JDBC 4) still does not have support for obtaining information
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
	public Iterable<SequenceInformation> extractMetadata(ExtractionContext extractionContext) throws SQLException;
}
