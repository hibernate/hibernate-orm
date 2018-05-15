/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.criteria;

/**
 * A strategy for matching strings using "like".
 *
 * @author Chris Cranford
 */
public enum MatchMode {
	/**
	 * Match the pattern exactly.
	 */
	EXACT {
		@Override
		public String toMatchString(String pattern) {
			return pattern;
		}
	},

	/**
	 * Match the start of the string to the pattern.
	 */
	START {
		@Override
		public String toMatchString(String pattern) {
			return pattern + '%';
		}
	},

	/**
	 * Match the end of the string to the pattern.
	 */
	END {
		@Override
		public String toMatchString(String pattern) {
			return '%' + pattern;
		}
	},

	/**
	 * Match the pattern anywhere in the string.
	 */
	ANYWHERE {
		@Override
		public String toMatchString(String pattern) {
			return '%' + pattern + '%';
		}
	};

	/**
	 * Convert the pattern by prepending/append "%".
	 *
	 * @param pattern The pattern to convert according to the mode.
	 * @return The converted pattern.
	 */
	public abstract String toMatchString(String pattern);
}
