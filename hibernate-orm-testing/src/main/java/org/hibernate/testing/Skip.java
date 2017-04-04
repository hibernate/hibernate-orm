/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	class AlwaysSkip implements Matcher {
		@Override
		public boolean isMatch() {
			return true;
		}
	}

	interface OperatingSystem {
		class Windows implements Matcher {
			@Override
			public boolean isMatch() {
				return System.getProperty("os.name").toLowerCase().contains( "windows" );
			}
		}
	}
}
