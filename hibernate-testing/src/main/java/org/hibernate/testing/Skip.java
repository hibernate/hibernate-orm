/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation, used in combination with {@link Matcher}, to determine when/if tests should be skipped.
 *
 * @author Steve Ebersole
 */
@Retention( RetentionPolicy.RUNTIME )
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Skip {
	/**
	 * The condition which causes a skip
	 *
	 * @return The condition
	 */
	Class<? extends Matcher> condition();

	/**
	 * A message describing the reason for the skip
	 *
	 * @return Descriptive message
	 */
	String message();

	/**
	 * Simple boolean assertion
	 */
	public static interface Matcher {
		/**
		 * Do we have a match to the underlying condition?
		 *
		 * @return True/false ;)
		 */
		public boolean isMatch();
	}

	public static class AlwaysSkip implements Matcher {
		@Override
		public boolean isMatch() {
			return true;
		}
	}
}
