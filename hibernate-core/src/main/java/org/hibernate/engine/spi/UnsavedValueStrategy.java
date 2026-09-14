/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	 * Make the transient/detached determination with access to entity context.
	 * <p>
	 * This variant allows strategies to consider the entity instance and session when determining
	 * unsaved status. This is particularly useful for generators that implement mixed-timing patterns
	 * where the timing of ID generation can vary per-instance.
	 * <p>
	 * The default implementation delegates to {@link #isUnsaved(Object)} for backward compatibility.
	 * Implementations that need entity/session context should override this method.
	 *
	 * @param test The value to be tested
	 * @param entity The entity instance
	 * @param session The session
	 *
	 * @return {@code true} indicates the value corresponds to unsaved data (aka, transient state); {@code false}
	 * indicates the value does not corresponds to unsaved data (aka, detached state); {@code null} indicates that
	 * this strategy was not able to determine conclusively.
	 *
	 * @since 6.6
	 */
	default @Nullable Boolean isUnsaved(@Nullable Object test, @Nullable Object entity, @Nullable SharedSessionContractImplementor session) {
		return isUnsaved( test );
	}

	/**
	 * Get a default value meant to indicate transience.
	 *
	 * @param currentValue The current state value.
	 *
	 * @return The default transience value.
	 */
	@Nullable Object getDefaultValue(@Nullable Object currentValue);
}
