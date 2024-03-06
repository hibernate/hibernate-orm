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
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.QueryHint;

/**
 * @author Steve Ebersole
 */
public record NamedNativeQueryRegistration(String name, AnnotationUsage<NamedNativeQuery> configuration) {
	public String getQueryString() {
		return configuration.getString( "query" );
	}

	public ClassDetails getResultClass() {
		return configuration.getClassDetails( "resultClass" );
	}

	public String getResultSetMapping() {
		return configuration.getString( "resultSetMapping" );
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
