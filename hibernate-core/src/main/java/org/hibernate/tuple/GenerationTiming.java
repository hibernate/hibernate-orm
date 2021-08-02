/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

/**
 * @author Steve Ebersole
 */
public enum GenerationTiming {
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

	public abstract boolean includesInsert();
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
