/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tuple;

import java.util.EnumSet;
import java.util.Locale;

import org.hibernate.generator.EventType;

/**
 * Represents the {@linkplain #getEventTypes timing} of value generation.
 *
 * @author Steve Ebersole
 *
 * @deprecated Replaced by {@link EventType} as id-generation has been
 * redefined using the new broader {@linkplain org.hibernate.generator generation}
 * approach.  For 7.0, this is kept around to support {@code hbm.xml} mappings and
 * will be removed in 8.0 once we finally drop {@code hbm.xml} support.
 */
@Deprecated(since = "6.2", forRemoval = true)
public enum GenerationTiming {
	/**
	 * Value generation that never occurs.
	 */
	NEVER,
	/**
	 * Value generation that occurs when a row is inserted in the database.
	 */
	INSERT,
	/**
	 * Value generation that occurs when a row is updated in the database.
	 */
	UPDATE,
	/**
	 * Value generation that occurs when a row is inserted or updated in the database.
	 */
	ALWAYS;

	/**
	 * Does value generation happen for SQL {@code insert} statements?
	 */
	public boolean includesInsert() {
		return this == INSERT || this == ALWAYS;
	}
	/**
	 * Does value generation happen for SQL {@code update} statements?
	 */
	public boolean includesUpdate() {
		return this == UPDATE || this == ALWAYS;
	}

	public boolean includes(GenerationTiming timing) {
		return switch (this) {
			case NEVER -> timing == NEVER;
			case INSERT -> timing.includesInsert();
			case UPDATE -> timing.includesUpdate();
			case ALWAYS -> true;
		};
	}

	public static GenerationTiming parseFromName(String name) {
		return switch (name.toLowerCase(Locale.ROOT)) {
			case "insert" -> INSERT;
			case "update" -> UPDATE;
			case "always" -> ALWAYS;
			default -> NEVER;
		};
	}

	/**
	 * Return the equivalent set of {@linkplain EventType event types}
	 */
	public EnumSet<EventType> getEventTypes() {
		return switch ( this ) {
			case ALWAYS -> EnumSet.allOf( EventType.class );
			case INSERT -> EnumSet.of( EventType.INSERT );
			case UPDATE -> EnumSet.of( EventType.UPDATE );
			case NEVER -> null;
		};
	}
}
