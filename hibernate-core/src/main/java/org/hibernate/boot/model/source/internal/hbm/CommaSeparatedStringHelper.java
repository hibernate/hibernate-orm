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
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
public final class CommaSeparatedStringHelper {

	private static final Pattern COMMA_SEPARATED_PATTERN = Pattern.compile( "\\s*,\\s*" );

	private CommaSeparatedStringHelper() {
	}

	public static Set<String> split(String values) {
		if ( values == null || values.isEmpty() ) {
			return Collections.emptySet();
		}
		return COMMA_SEPARATED_PATTERN.splitAsStream( values ).collect( Collectors.toSet() );
	}

	public static Set<String> splitAndCombine(Set<String> x, String values) {
		if ( x.isEmpty() && ( values == null || values.isEmpty() ) ) {
			return Collections.emptySet();
		}

		HashSet<String> set = new HashSet<String>( x );
		if ( values != null && !values.isEmpty() ) {
			Collections.addAll( set, COMMA_SEPARATED_PATTERN.split( values ) );
		}
		return set;
	}

	public static List<String> parseCommaSeparatedString(String incomingString) {
		if ( StringHelper.isEmpty( incomingString ) ) {
			return Collections.emptyList();
		}
		return COMMA_SEPARATED_PATTERN.splitAsStream( incomingString ).collect( Collectors.toList() );
	}
}
