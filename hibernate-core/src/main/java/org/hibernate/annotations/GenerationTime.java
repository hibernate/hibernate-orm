/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.AssertionFailure;
import org.hibernate.Internal;
import org.hibernate.tuple.GenerationTiming;

/**
 * Represents a class of events involving interaction with the database
 * that causes generation of a new value. Intended for use with the
 * {@link Generated} and {@link CurrentTimestamp} annotations.
 *
 * @author Emmanuel Bernard
 *
 * @see Generated
 * @see CurrentTimestamp
 */
public enum GenerationTime {
	/**
	 * Indicates that a value is never generated.
	 */
	NEVER,
	/**
	 * Indicates that a new value is generated on insert.
	 */
	INSERT,
	/**
	 * Indicates that a new value is generated on update.
	 *
	 * @since 6.2
	 */
	UPDATE,
	/**
	 * Indicates that a new value is generated on insert and on update.
	 *
	 * @since 6.2
	 */
	INSERT_OR_UPDATE,
	/**
	 * Indicates that a new value is generated on insert and on update.
	 *
	 * @deprecated use {@link #INSERT_OR_UPDATE}
	 */
	@Deprecated(since = "6.2")
	ALWAYS;

	/**
	 * @return {@code true} if a new value is generated when an insert is executed
	 */
	public boolean includesInsert() {
		return getEquivalent().includesInsert();
	}

	/**
	 * @return {@code true} if a new value is generated when an update is executed
	 */
	public boolean includesUpdate() {
		return getEquivalent().includesUpdate();
	}

	/**
	 * @return the equivalent instance of {@link GenerationTiming}
	 */
	@Internal
	public GenerationTiming getEquivalent() {
		switch (this) {
			case ALWAYS:
			case INSERT_OR_UPDATE:
				return GenerationTiming.ALWAYS;
			case INSERT:
				return GenerationTiming.INSERT;
			case UPDATE:
				return GenerationTiming.UPDATE;
			case NEVER:
				return GenerationTiming.NEVER;
			default:
				throw new AssertionFailure("unknown event");
		}
	}
}
