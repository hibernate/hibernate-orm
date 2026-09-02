/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.type.internal.SpannerJsonJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import static org.hibernate.SPI.Role.USE;

/// Access to Hibernate's stock Spanner JDBC type descriptors.
///
/// Call these methods from
/// [org.hibernate.dialect.Dialect#contributeTypes(org.hibernate.boot.model.TypeContributions, org.hibernate.service.ServiceRegistry)]
/// and contribute the returned descriptor without referring to its concrete
/// implementation class.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public final class SpannerJdbcTypes {
	private SpannerJdbcTypes() {
	}

	/// Obtain Spanner's JSON descriptor.
	public static JdbcType json() {
		return SpannerJsonJdbcType.INSTANCE;
	}
}
