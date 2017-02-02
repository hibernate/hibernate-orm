/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

/**
 * Represents an strategy for matching strings using "like".
 *
 * @author Gavin King
 * @see Example#enableLike(MatchMode)
 */
public enum MatchMode {

	/**
	 * Match the entire string to the pattern
	 */
	EXACT {
		@Override
		public String toMatchString(String pattern) {
			return pattern;
		}
	},

	/**
	 * Match the start of the string to the pattern
	 */
	START {
		@Override
		public String toMatchString(String pattern) {
			return pattern + '%';
		}
	},

	/**
	 * Match the end of the string to the pattern
	 */
	END {
		@Override
		public String toMatchString(String pattern) {
			return '%' + pattern;
		}
	},

	/**
	 * Match the pattern anywhere in the string
	 */
	ANYWHERE {
		@Override
		public String toMatchString(String pattern) {
			return '%' + pattern + '%';
		}
	};

	/**
	 * Convert the pattern, by appending/prepending "%"
	 *
	 * @param pattern The pattern for convert according to the mode
	 *
	 * @return The converted pattern
	 */
	public abstract String toMatchString(String pattern);

}
