/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SqmPathSource;

/**
 * @author Steve Ebersole
 */
public class QueryHelper {
	private QueryHelper() {
		// disallow direct instantiation
	}

	@SafeVarargs
	public static <T> SqmBindableType<? extends T> highestPrecedenceType(SqmBindableType<? extends T>... types) {
		if ( types == null || types.length == 0 ) {
			return null;
		}

		if ( types.length == 1 ) {
			return types[0];
		}

		var highest = highestPrecedenceType2( types[0], types[1] );
		for ( int i = 2; i < types.length; i++ ) {
			highest = highestPrecedenceType2( highest, types[i] );
		}

		return highest;
	}

	public static <X> SqmBindableType<? extends X> highestPrecedenceType2(
			SqmBindableType<? extends X> type1,
			SqmBindableType<? extends X> type2) {
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

		if ( type1.getExpressibleJavaType() == null ) {
			return type2;
		}
		else if ( type2.getExpressibleJavaType() == null ) {
			return type1;
		}
		// any other precedence rules?
		else if ( type2.getExpressibleJavaType().isWider( type1.getExpressibleJavaType() ) ) {
			return type2;
		}

		return type1;
	}

}
