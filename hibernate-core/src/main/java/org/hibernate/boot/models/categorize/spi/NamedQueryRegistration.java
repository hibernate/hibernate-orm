/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.spi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.AnnotationUsage;

import jakarta.persistence.LockModeType;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;

/**
 * @author Steve Ebersole
 */
public record NamedQueryRegistration(String name, AnnotationUsage<NamedQuery> configuration) {
	public String getQueryString() {
		return configuration.getString( "query" );
	}

	public LockModeType getLockModeType() {
		return configuration.getEnum( "lockMode" );
	}

	public Map<String,String> getQueryHints() {
		final List<AnnotationUsage<QueryHint>> hints = configuration.getList( "hints" );
		if ( CollectionHelper.isEmpty( hints ) ) {
			return Collections.emptyMap();
		}

		final Map<String,String> result = CollectionHelper.linkedMapOfSize( hints.size() );
		for ( AnnotationUsage<QueryHint> hint : hints ) {
			result.put( hint.getString( "name" ), hint.getString( "value" ) );
		}
		return result;
	}
}
