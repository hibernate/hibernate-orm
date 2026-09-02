/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import org.hibernate.SPI;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;

/// Strategy supplied by a Dialect for resolving the column size of a mapped
/// Java and JDBC type.
///
/// Implement [#resolveSize(JdbcType, JavaType, Integer, Integer, Long)] to
/// combine database defaults with the explicit precision, scale, and length.
/// Return a new or otherwise safely owned [Size]; do not retain or mutate the
/// `Size` passed to the convenience overload. Extend [StandardSizeStrategy]
/// when the database changes only selected JDBC-type cases and delegate every
/// other case to its standard implementation.
///
/// @see org.hibernate.dialect.Dialect#getSizeStrategy()
///
/// @since 8.0
/// @author Steve Ebersole
/// @author Gavin King
@SPI({ IMPLEMENT, SUPPLY })
public interface SizeStrategy {
	/// Resolve the size for the given mapped JDBC and Java types.
	///
	/// The precision, scale, and length arguments are explicit mapping values
	/// when non-null. The implementation should apply type and database defaults
	/// where an argument is null.
	///
	/// @return a non-null, safely owned resolved size
	Size resolveSize(
			JdbcType jdbcType,
			JavaType<?> javaType,
			Integer precision,
			Integer scale,
			Long length);

	/// Resolve a size without mutating the supplied size.
	///
	/// @param size the size whose explicit values should be resolved
	/// @return a non-null, safely owned resolved size
	default Size resolveSize(
			JdbcType jdbcType,
			JavaType<?> javaType,
			Size size) {
		return resolveSize( jdbcType, javaType, size.getPrecision(), size.getScale(), size.getLength() );
	}
}
