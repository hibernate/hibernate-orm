/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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





