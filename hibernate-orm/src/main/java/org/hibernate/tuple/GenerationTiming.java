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
	};

	public abstract boolean includesInsert();
	public abstract boolean includesUpdate();

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
