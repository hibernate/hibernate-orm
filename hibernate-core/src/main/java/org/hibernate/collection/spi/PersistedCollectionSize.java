/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import java.util.Locale;

import org.hibernate.Incubating;

/// Shared deferred handle for the persisted size of one collection mutation.
///
/// A specialized list producer may reference one handle from multiple append
/// positions. Planning or execution resolves the handle at most once.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public final class PersistedCollectionSize {
	private boolean resolved;
	private int size;

	/// Resolve the persisted size.
	public void resolve(int size) {
		if ( size < 0 ) {
			throw new IllegalArgumentException( "Collection size cannot be negative" );
		}
		if ( resolved && this.size != size ) {
			throw new IllegalStateException( "Persisted collection size was already resolved" );
		}
		this.size = size;
		resolved = true;
	}

	/// Whether this handle has been resolved.
	public boolean isResolved() {
		return resolved;
	}

	/// Obtain the resolved persisted size.
	public int get() {
		if ( !resolved ) {
			throw new IllegalStateException( "Persisted collection size has not been resolved" );
		}
		return size;
	}

	@Override
	public String toString() {
		return resolved
				? String.format( Locale.ROOT, "PersistedCollectionSize(%d)", size )
				: "PersistedCollectionSize(unresolved)";
	}
}
