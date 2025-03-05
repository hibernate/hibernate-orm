/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The base contract for determining transient status versus detached status.
 *
 * @author Steve Ebersole
 */
public interface UnsavedValueStrategy {
	/**
	 * Make the transient/detached determination
	 *
	 * @param test The value to be tested
	 *
	 * @return {@code true} indicates the value corresponds to unsaved data (aka, transient state); {@code false}
	 * indicates the value does not corresponds to unsaved data (aka, detached state); {@code null} indicates that
	 * this strategy was not able to determine conclusively.
	 */
	@Nullable Boolean isUnsaved(@Nullable Object test);

	/**
	 * Get a default value meant to indicate transience.
	 *
	 * @param currentValue The current state value.
	 *
	 * @return The default transience value.
	 */
	@Nullable Object getDefaultValue(@Nullable Object currentValue);
}
