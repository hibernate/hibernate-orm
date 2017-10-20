/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.hamcrest.sqm;

import org.hibernate.query.sqm.produce.spi.ImplicitAliasGenerator;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * @author Steve Ebersole
 */
public class SqmAliasMatchers {
	private static final BaseMatcher<String> IS_IMPLICIT = new BaseMatcher<String>() {
		@Override
		public boolean matches(Object item) {
			return ImplicitAliasGenerator.isImplicitAlias( (String) item );
		}

		@Override
		public void describeTo(Description description) {
			description.appendText( "isImplicitAlias()" );
		}
	};

	public static Matcher<String> isImplicitAlias() {
		return IS_IMPLICIT;
	}
}
