/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.GenerationTime;

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
	NEVER {
		@Override
		public boolean includesInsert() {
			return false;
		}

		@Override
		public boolean includesUpdate() {
			return false;
		}

		@Override
		public boolean includes(GenerationTiming timing) {
			return false;
		}
	},
	/**
	 * Value generation that occurs when a row is inserted in the database.
	 */
	INSERT {
		@Override
		public boolean includesInsert() {
			return true;
		}

		@Override
		public boolean includesUpdate() {
			return false;
		}

		@Override
		public boolean includes(GenerationTiming timing) {
			return timing.includesInsert();
		}
	},
	/**
	 * Value generation that occurs when a row is updated in the database.
	 */
	UPDATE {
		@Override
		public boolean includesInsert() {
			return false;
		}

		@Override
		public boolean includesUpdate() {
			return true;
		}

		@Override
		public boolean includes(GenerationTiming timing) {
			return timing.includesUpdate();
		}
	},
	/**
	 * Value generation that occurs when a row is inserted or updated in the database.
	 */
	ALWAYS {
		@Override
		public boolean includesInsert() {
			return true;
		}

		@Override
		public boolean includesUpdate() {
			return true;
		}

		@Override
		public boolean includes(GenerationTiming timing) {
			return timing != NEVER;
		}
	};

	/**
	 * Does value generation happen for SQL {@code insert} statements?
	 */
	public abstract boolean includesInsert();
	/**
	 * Does value generation happen for SQL {@code update} statements?
	 */
	public abstract boolean includesUpdate();

	public boolean isAlways() {
		return this == ALWAYS;
	}

	public abstract boolean includes(GenerationTiming timing);

	public static GenerationTiming parseFromName(String name) {
		if ( "insert".equalsIgnoreCase( name ) ) {
			return INSERT;
		}
		else if ( "update".equalsIgnoreCase( name ) ) {
			return UPDATE;
		}
		else if ( "always".equalsIgnoreCase( name ) ) {
			return ALWAYS;
		}
		else {
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
