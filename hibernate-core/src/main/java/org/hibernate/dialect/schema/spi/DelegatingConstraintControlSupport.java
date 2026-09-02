/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import java.util.List;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;

/// Provider base forwarding constraint-control commands to a stable delegate.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(IMPLEMENT)
public abstract class DelegatingConstraintControlSupport implements ConstraintControlSupport {
	private final ConstraintControlSupport delegate;

	@SPI(IMPLEMENT)
	protected DelegatingConstraintControlSupport(ConstraintControlSupport delegate) {
		this.delegate = requireNonNull( delegate );
	}

	@Override public ConstraintControlMode constraintControlMode() { return delegate.constraintControlMode(); }
	@Override public List<String> disableCommands() { return delegate.disableCommands(); }
	@Override public List<String> enableCommands() { return delegate.enableCommands(); }
	@Override public List<String> disableConstraintCommands(ConstraintControlRequest request) { return delegate.disableConstraintCommands( request ); }
	@Override public List<String> enableConstraintCommands(ConstraintControlRequest request) { return delegate.enableConstraintCommands( request ); }
}
