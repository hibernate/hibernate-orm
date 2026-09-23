/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal;

import org.hibernate.bytecode.enhance.spi.Enhancer;

/// Composes the independently enabled managed and client enhancement passes.
/// The delegate's legacy extended-enhancement option must be disabled.
///
/// @since 8.0
/// @author Steve Ebersole
public final class EnhancementPipeline implements Enhancer {

	private final Enhancer delegate;
	private final boolean managed;
	private final boolean client;

	public EnhancementPipeline(Enhancer delegate, boolean managed, boolean client) {
		this.delegate = delegate;
		this.managed = managed;
		this.client = client;
	}

	/// A view for completing managed enhancement of the selected source set first.
	public Enhancer managedPass() {
		return pass( false );
	}

	/// A view for rewriting clients after the selected managed classes are enhanced.
	public Enhancer clientPass() {
		return pass( true );
	}

	private Enhancer pass(boolean clientPass) {
		return new Enhancer() {
			@Override
			public byte[] enhance(String className, byte[] originalBytes) {
				return clientPass
						? ( client ? delegate.enhanceClient( className, originalBytes ) : null )
						: ( managed ? delegate.enhance( className, originalBytes ) : null );
			}

			@Override
			public void discoverTypes(String className, byte[] originalBytes) {
				if ( !clientPass ) {
					EnhancementPipeline.this.discoverTypes( className, originalBytes );
				}
			}
		};
	}

	@Override
	public byte[] enhance(String className, byte[] originalBytes) {
		final byte[] managedBytes = managed ? delegate.enhance( className, originalBytes ) : null;
		if ( client ) {
			final byte[] clientBytes = delegate.enhanceClient(
					className,
					managedBytes == null ? originalBytes : managedBytes
			);
			if ( clientBytes != null ) {
				return clientBytes;
			}
		}
		return managedBytes;
	}

	@Override
	public byte[] enhanceClient(String className, byte[] originalBytes) {
		return delegate.enhanceClient( className, originalBytes );
	}

	@Override
	public void discoverTypes(String className, byte[] originalBytes) {
		if ( managed ) {
			delegate.discoverTypes( className, originalBytes );
		}
	}
}
