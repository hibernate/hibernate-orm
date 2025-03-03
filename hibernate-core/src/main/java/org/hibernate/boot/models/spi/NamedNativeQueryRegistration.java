/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import java.util.Collections;
import java.util.Map;

import org.hibernate.internal.util.collections.CollectionHelper;

import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.QueryHint;

/**
 * @author Steve Ebersole
 */
public record NamedNativeQueryRegistration(String name, NamedNativeQuery configuration) {
	public String getQueryString() {
		return configuration.query();
	}

	public Class<?> getResultClass() {
		return configuration.resultClass();
	}

	public String getResultSetMapping() {
		return configuration.resultSetMapping();
	}

	public Map<String,String> getQueryHints() {
		final QueryHint[] hints = configuration.hints();
		if ( CollectionHelper.isEmpty( hints ) ) {
			return Collections.emptyMap();
		}

		final Map<String,String> result = CollectionHelper.linkedMapOfSize( hints.length );
		for ( QueryHint hint : hints ) {
			result.put( hint.name(), hint.value() );
		}
		return result;
	}
}
