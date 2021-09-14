/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

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
	 * Does value generation happen for SQL {@code INSERT} statements?
	 */
	public abstract boolean includesInsert();
	/**
	 * Does value generation happen for SQL {@code UPDATE} statements?
	 */
	public abstract boolean includesUpdate();

	public abstract boolean includes(GenerationTiming timing);

	public static GenerationTiming parseFromName(String name) {
		if ( "insert".equalsIgnoreCase( name ) ) {
			return INSERT;
		}
		else if ( "always".equalsIgnoreCase( name ) ) {
			return ALWAYS;
		}
		else {
			return NEVER;
		}
	}
}
