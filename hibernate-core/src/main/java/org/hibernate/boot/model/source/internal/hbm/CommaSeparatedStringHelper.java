/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class CommaSeparatedStringHelper {
	private CommaSeparatedStringHelper() {
	}

	public static Set<String> split(String values) {
		if ( values == null || values.isEmpty() ) {
			return Collections.emptySet();
		}

		HashSet<String> set = new HashSet<String>();
		Collections.addAll( set, values.split( "\\s*,\\s*" ) );
		return set;
	}

	public static Set<String> splitAndCombine(Set<String> x, String values) {
		if ( x.isEmpty() && (values == null || values.isEmpty()) ) {
			return Collections.emptySet();
		}

		HashSet<String> set = new HashSet<String>();
		set.addAll( x );
		if ( values != null && !values.isEmpty() ) {
			Collections.addAll( set, values.split( "\\s*,\\s*" ) );
		}
		return set;
	}
}
