/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;

/// Provider base forwarding index DDL policy to a stable delegate.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(IMPLEMENT)
public abstract class DelegatingIndexDdlSupport implements IndexDdlSupport {
	private final IndexDdlSupport delegate;

	@SPI(IMPLEMENT)
	protected DelegatingIndexDdlSupport(IndexDdlSupport delegate) {
		this.delegate = requireNonNull( delegate );
	}

	@Override public String createCommand(IndexDdlRequest request) { return delegate.createCommand( request ); }
	@Override public String createTail(IndexDdlRequest request) { return delegate.createTail( request ); }
	@Override public IndexNameQualification nameQualification() { return delegate.nameQualification(); }
}
