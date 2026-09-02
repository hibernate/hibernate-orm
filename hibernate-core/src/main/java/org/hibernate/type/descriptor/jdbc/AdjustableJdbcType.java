/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.SPI;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// A [JdbcType] which may select a more appropriate descriptor for a specific
/// mapping context, including LOB, nationalized, or primitive/wrapper use.
///
/// @author Christian Beikov
/// @see #resolveIndicatedType(JdbcTypeIndicators, JavaType)
@SPI({ USE, IMPLEMENT })
public interface AdjustableJdbcType extends JdbcType {

	/// Resolve and supply the descriptor to use for the indicated mapping.
	///
	/// @see JdbcType
	@SPI(SUPPLY)
	JdbcType resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<?> domainJtd);
}
