package org.hibernate.dialect.jdbc.spi;

import org.hibernate.SPI;

/// JDBC execution modes that may be restricted for an INSERT sourced by a SELECT.
///
/// @since 8.0
@SPI({ SPI.Role.USE, SPI.Role.SUPPLY })
public enum InsertSelectExecution {
	BATCH,
	GENERATED_KEYS
}
