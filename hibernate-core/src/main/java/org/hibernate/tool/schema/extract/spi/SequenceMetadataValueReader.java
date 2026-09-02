/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.spi;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;

/// Read one value from the current row of a sequence-discovery result set.
///
/// The supplied [ResultSet] is produced by executing the lookup SQL configured
/// through [SequenceInformationExtractors#builder(String)] and is already
/// positioned on the row to read. Implementations must not close, advance, or
/// retain the result set. Propagate any [SQLException] unchanged.
///
/// @param <T> extracted value type
///
/// @since 8.0
/// @author Steve Ebersole
@FunctionalInterface
@SPI(IMPLEMENT)
public interface SequenceMetadataValueReader<T> {
	/// Read one value from `sequenceMetadataRow` without changing its position.
	T read(ResultSet sequenceMetadataRow) throws SQLException;
}
