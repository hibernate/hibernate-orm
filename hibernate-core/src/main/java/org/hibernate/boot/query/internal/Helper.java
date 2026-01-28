/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query.internal;

import jakarta.persistence.QueryHint;
import org.hibernate.internal.util.collections.CollectionHelper;

import java.util.Collections;
import java.util.Map;

/// Helper for building [org.hibernate.boot.query.NamedQueryDefinition]
/// references from annotations.
///
/// @author Steve Ebersole
public final class Helper {

	public static Map<String,Object> extractHints(QueryHint[] hints) {
		if ( hints.length == 0 ) {
			return Collections.emptyMap();
		}
		final Map<String,Object> hintMap = CollectionHelper.mapOfSize( hints.length );
		for ( int i = 0; i < hints.length; i++ ) {
			hintMap.put( hints[i].name(), hints[i].value() );
		}
		return hintMap;
	}

	private Helper() {
	}
}
