/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.AssertionFailure;
import org.hibernate.Internal;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.tuple.GenerationTiming;

import java.util.EnumSet;

/**
 * Represents a class of events involving interaction with the database
 * that causes generation of a new value. Intended for use with the
 * {@link Generated} and {@link CurrentTimestamp} annotations.
 *
 * @author Emmanuel Bernard
 *
 * @see Generated
 * @see CurrentTimestamp
 *
 * @deprecated use {@link EventType} and {@link EventTypeSets} instead
 */
@Deprecated(since = "6.2")
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
	 */
	ALWAYS;

	public EnumSet<EventType> eventTypes() {
		switch (this) {
			case NEVER:
				return EventTypeSets.NONE;
			case ALWAYS:
				return EventTypeSets.ALL;
			case INSERT:
				return EventTypeSets.INSERT_ONLY;
			case UPDATE:
				return EventTypeSets.UPDATE_ONLY;
			default:
				throw new AssertionFailure("unknown event");
		}
	}

	/**
	 * @return the equivalent instance of {@link GenerationTiming}
	 */
	@Internal
	public GenerationTiming getEquivalent() {
		switch (this) {
			case ALWAYS:
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
