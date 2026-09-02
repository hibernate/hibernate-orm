/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;
import org.hibernate.sql.spi.SqlAppender;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;

/// Provider base forwarding column-definition rendering to a stable delegate.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(IMPLEMENT)
public abstract class DelegatingColumnDefinitionSupport implements ColumnDefinitionSupport {
	private final ColumnDefinitionSupport delegate;

	@SPI(IMPLEMENT)
	protected DelegatingColumnDefinitionSupport(ColumnDefinitionSupport delegate) {
		this.delegate = requireNonNull( delegate );
	}

	@Override
	public void appendDefinition(SqlAppender appender, ColumnDefinitionRequest request) {
		delegate.appendDefinition( appender, request );
	}
}
