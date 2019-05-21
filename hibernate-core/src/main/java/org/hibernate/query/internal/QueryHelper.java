/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.SqmPathSource;

/**
 * @author Steve Ebersole
 */
public class QueryHelper {
	private QueryHelper() {
		// disallow direct instantiation
	}

	@SafeVarargs
	public static <T> SqmExpressable<? extends T> highestPrecedenceType(SqmExpressable<? extends T>... types) {
		if ( types == null || types.length == 0 ) {
			return null;
		}

		if ( types.length == 1 ) {
			return types[0];
		}

		//noinspection unchecked
		SqmExpressable<? extends T> highest = highestPrecedenceType2( (SqmExpressable) types[0], types[1] );
		for ( int i = 2; i < types.length; i++ ) {
			//noinspection unchecked
			highest = highestPrecedenceType2( highest, types[i] );
		}

		return highest;
	}

	public static <X> SqmExpressable<? extends X> highestPrecedenceType2(
			SqmExpressable<? extends X> type1,
			SqmExpressable<? extends X> type2) {
		if ( type1 == null && type2 == null ) {
			return null;
		}
		else if ( type1 == null ) {
			return type2;
		}
		else if ( type2 == null ) {
			return type1;
		}

		if ( type1 instanceof SqmPathSource ) {
			return type1;
		}

		if ( type2 instanceof SqmPathSource ) {
			return type2;
		}

		// any other precedence rules?

		return type1;
	}

}
