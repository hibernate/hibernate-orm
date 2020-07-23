/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.hibernate.internal.util.collections.ArrayHelper;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractPropertyContainer<T extends AbstractPropertyContainer<T>> implements PropertyContainer {
	private Map<String,FetchBuilder> fetchBuilderMap;

	protected abstract String getPropertyBase();

	@Override
	public T addProperty(String propertyName, String columnAlias) {
		final FetchBuilder fetchBuilder = addProperty( propertyName );
		fetchBuilder.addColumnAlias( columnAlias );

		return (T) this;
	}

	@Override
	public T addProperty(String propertyName, String... columnAliases) {
		final FetchBuilder fetchBuilder = addProperty( propertyName );
		ArrayHelper.forEach( columnAliases, fetchBuilder::addColumnAlias );

		return (T) this;
	}

	@Override
	public FetchBuilder addProperty(String propertyName) {
		if ( fetchBuilderMap == null ) {
			fetchBuilderMap = new HashMap<>();
		}
		else {
			final FetchBuilder existing = fetchBuilderMap.get( propertyName );
			if ( existing != null ) {
				throw new IllegalArgumentException(
						String.format(
								Locale.ROOT,
								"Fetch was already defined for %s.%s : %s",
								getPropertyBase(),
								propertyName,
								existing
						)
				);
			}
		}

		final FetchBuilder fetchBuilder = new StandardFetchBuilderImpl();
		fetchBuilderMap.put( propertyName, fetchBuilder );

		return fetchBuilder;
	}
}
