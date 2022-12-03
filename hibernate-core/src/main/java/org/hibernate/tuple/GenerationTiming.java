/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.GenerationTime;

import java.util.Locale;

/**
 * Represents the timing of {@link ValueGeneration value generation} that occurs
 * in the Java program, or in the database.
 *
 * @author Steve Ebersole
 *
 * @see ValueGeneration
 */
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
		switch (this) {
			case NEVER:
				return timing == NEVER;
			case INSERT:
				return timing.includesInsert();
			case UPDATE:
				return timing.includesUpdate();
			case ALWAYS:
				return true;
			default:
				throw new AssertionFailure("unknown timing");
		}
	}

	public static GenerationTiming parseFromName(String name) {
		switch ( name.toLowerCase(Locale.ROOT) ) {
			case "insert":
				return INSERT;
			case "update":
				return UPDATE;
			case "always":
				return ALWAYS;
			default:
				return NEVER;
		}
	}

	/**
	 * @return the equivalent instance of {@link GenerationTime}
	 */
	public GenerationTime getEquivalent() {
		switch (this) {
			case ALWAYS:
				return GenerationTime.INSERT_OR_UPDATE;
			case INSERT:
				return GenerationTime.INSERT;
			case UPDATE:
				return GenerationTime.UPDATE;
			case NEVER:
				return GenerationTime.NEVER;
			default:
				throw new AssertionFailure("unknown timing");
		}
	}
}
