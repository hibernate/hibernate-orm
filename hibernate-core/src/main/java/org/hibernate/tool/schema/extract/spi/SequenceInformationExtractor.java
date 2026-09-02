/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.spi;

import java.sql.SQLException;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Extract information about existing database sequences when JDBC metadata
/// does not expose it.
///
/// Implement this contract directly only for a multi-stage or otherwise
/// nonstandard discovery algorithm. For a single lookup query, configure a
/// standard extractor with [SequenceInformationExtractors#builder(String)].
/// Supply the resulting strategy from
/// [org.hibernate.dialect.Dialect#getSequenceInformationExtractor()].
///
/// @see org.hibernate.dialect.Dialect#getSequenceInformationExtractor()
///
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface SequenceInformationExtractor {
	/// Extract sequence information, preserving discovery-query row order.
	///
	/// Propagate an [SQLException] unchanged unless the implementation can handle
	/// it without changing the extraction contract.
	Iterable<SequenceInformation> extractMetadata(ExtractionContext extractionContext) throws SQLException;
}
