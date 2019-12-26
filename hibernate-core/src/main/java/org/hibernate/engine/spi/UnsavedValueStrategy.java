/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

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
	public Boolean isUnsaved(Object test);

	/**
	 * Get a default value meant to indicate transience.
	 *
	 * @param currentValue The current state value.
	 *
	 * @return The default transience value.
	 */
	public Object getDefaultValue(Object currentValue);
}
