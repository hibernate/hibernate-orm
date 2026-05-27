/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.FetchOption;
import jakarta.persistence.QueryHint;

/**
 * Internal adapter used to preserve {@link jakarta.persistence.Fetch#hints()}
 * metadata on a graph attribute node.
 * <p>
 * JPA {@link QueryHint} annotations are not themselves {@link FetchOption}s,
 * but entity graph attribute nodes expose fetch metadata through
 * {@link jakarta.persistence.AttributeNode#getOptions()}. This option stores
 * the declared hints so they remain visible through that API. It does not
 * interpret the hints or alter SQL loading behavior.
 *
 * @since 8.0
 * @author Steve Ebersole
 */
record FetchHintOptions(Map<String, String> hints) implements FetchOption {
	static FetchHintOptions from(QueryHint[] hints) {
		final var hintMap = new LinkedHashMap<String, String>();
		for ( var hint : hints ) {
			hintMap.put( hint.name(), hint.value() );
		}
		return new FetchHintOptions( Map.copyOf( hintMap ) );
	}
}
