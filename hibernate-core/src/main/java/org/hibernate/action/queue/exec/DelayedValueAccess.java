/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import java.util.Locale;

/// Mutable value reference used when a JDBC value is not known at decomposition time.
///
/// The binding layer unwraps this handle immediately before binding a JDBC parameter.
///
/// @author Steve Ebersole
public final class DelayedValueAccess {
	private final String description;
	private boolean resolved;
	private Object value;

	public DelayedValueAccess(String description) {
		this.description = description;
	}

	public DelayedValueAccess(String description, Object value) {
		this.description = description;
		set( value );
	}

	public Object get() {
		if ( !resolved ) {
			throw new IllegalStateException(
					String.format(
							Locale.ROOT,
							"Attempted to bind unresolved delayed value [%s]",
							description
					)
			);
		}
		return value;
	}

	public void set(Object value) {
		this.value = value;
		this.resolved = true;
	}

	public boolean isResolved() {
		return resolved;
	}

	@Override
	public String toString() {
		return "DelayedValueAccess(" + description + ")";
	}
}
