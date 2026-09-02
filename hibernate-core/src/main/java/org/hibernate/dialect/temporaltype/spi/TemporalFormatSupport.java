/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporaltype.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.sql.spi.SqlAppender;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Translates Hibernate datetime format patterns to database SQL fragments.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getTemporalFormatSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface TemporalFormatSupport {
	/// Append the database translation of the supplied Hibernate format pattern.
	void appendFormat(SqlAppender appender, String format);
}
