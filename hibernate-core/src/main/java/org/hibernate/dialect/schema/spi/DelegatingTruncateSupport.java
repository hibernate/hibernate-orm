/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import java.util.List;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;

/// Provider base forwarding truncation rendering to a stable delegate.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(IMPLEMENT)
public abstract class DelegatingTruncateSupport implements TruncateSupport {
	private final TruncateSupport delegate;

	@SPI(IMPLEMENT)
	protected DelegatingTruncateSupport(TruncateSupport delegate) {
		this.delegate = requireNonNull( delegate );
	}

	@Override public TruncateMode truncateMode() { return delegate.truncateMode(); }
	@Override public List<String> renderCommands(TruncateRequest request) { return delegate.renderCommands( request ); }
}
