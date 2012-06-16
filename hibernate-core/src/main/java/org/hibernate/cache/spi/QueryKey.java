/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cache.spi;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.transform.CacheableResultTransformer;
import org.hibernate.type.Type;

/**
 * A key that identifies a particular query with bound parameter values.  This is the object Hibernate uses
 * as its key into its query cache.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class QueryKey implements Serializable {
	private final String sqlQueryString;
	private final Type[] positionalParameterTypes;
	private final Object[] positionalParameterValues;
	private final Map namedParameters;
	private final Integer firstRow;
	private final Integer maxRows;
	private final String tenantIdentifier;
	private final Set filterKeys;

	// the explicit user-provided result transformer, not the one used with "select new". Here to avoid mangling
	// transformed/non-transformed results.
	private final CacheableResultTransformer customTransformer;

	/**
	 * For performance reasons, the hashCode is cached; however, it is marked transient so that it can be
	 * recalculated as part of the serialization process which allows distributed query caches to work properly.
	 */
	private transient int hashCode;

	/**
	 * Generates a QueryKey.
	 *
	 * @param queryString The sql query string.
	 * @param queryParameters The query parameters
	 * @param filterKeys The keys of any enabled filters.
	 * @param session The current session.
	 * @param customTransformer The result transformer; should be
	 *            null if data is not transformed before being cached.
	 *
	 * @return The generate query cache key.
	 */
	public static QueryKey generateQueryKey(
			String queryString,
			QueryParameters queryParameters,
			Set filterKeys,
			SessionImplementor session,
			CacheableResultTransformer customTransformer) {
		// disassemble positional parameters
		final int positionalParameterCount = queryParameters.getPositionalParameterTypes().length;
		final Type[] types = new Type[positionalParameterCount];
		final Object[] values = new Object[positionalParameterCount];
		for ( int i = 0; i < positionalParameterCount; i++ ) {
			types[i] = queryParameters.getPositionalParameterTypes()[i];
			values[i] = types[i].disassemble( queryParameters.getPositionalParameterValues()[i], session, null );
		}

		// disassemble named parameters
		final Map<String,TypedValue> namedParameters;
		if ( queryParameters.getNamedParameters() == null ) {
			namedParameters = null;
		}
		else {
			namedParameters = CollectionHelper.mapOfSize( queryParameters.getNamedParameters().size() );
			for ( Map.Entry<String,TypedValue> namedParameterEntry : queryParameters.getNamedParameters().entrySet() ) {
				namedParameters.put(
						namedParameterEntry.getKey(),
						new TypedValue(
								namedParameterEntry.getValue().getType(),
								namedParameterEntry.getValue().getType().disassemble(
										namedParameterEntry.getValue().getValue(),
										session,
										null
								)
						)
				);
			}
		}

		// decode row selection...
		final RowSelection selection = queryParameters.getRowSelection();
		final Integer firstRow;
		final Integer maxRows;
		if ( selection != null ) {
			firstRow = selection.getFirstRow();
			maxRows = selection.getMaxRows();
		}
		else {
			firstRow = null;
			maxRows = null;
		}

		return new QueryKey(
				queryString,
				types,
				values,
				namedParameters,
				firstRow,
				maxRows,
				filterKeys,
				session.getTenantIdentifier(),
				customTransformer
		);
	}

	/**
	 * Package-protected constructor.
	 *
	 * @param sqlQueryString The sql query string.
	 * @param positionalParameterTypes Positional parameter types.
	 * @param positionalParameterValues Positional parameter values.
	 * @param namedParameters Named parameters.
	 * @param firstRow First row selection, if any.
	 * @param maxRows Max-rows selection, if any.
	 * @param filterKeys Enabled filter keys, if any.
	 * @param customTransformer Custom result transformer, if one.
	 * @param tenantIdentifier The tenant identifier in effect for this query, or {@code null}
	 */
	QueryKey(
			String sqlQueryString,
			Type[] positionalParameterTypes,
			Object[] positionalParameterValues,
			Map namedParameters,
			Integer firstRow,
			Integer maxRows,
			Set filterKeys,
			String tenantIdentifier,
			CacheableResultTransformer customTransformer) {
		this.sqlQueryString = sqlQueryString;
		this.positionalParameterTypes = positionalParameterTypes;
		this.positionalParameterValues = positionalParameterValues;
		this.namedParameters = namedParameters;
		this.firstRow = firstRow;
		this.maxRows = maxRows;
		this.tenantIdentifier = tenantIdentifier;
		this.filterKeys = filterKeys;
		this.customTransformer = customTransformer;
		this.hashCode = generateHashCode();
	}

	/**
	 * Provides access to the explicitly user-provided result transformer.
	 *
	 * @return The result transformer.
	 */
	public CacheableResultTransformer getResultTransformer() {
		return customTransformer;
	}

	/**
	 * Provide (unmodifiable) access to the named parameters that are part of this query.
	 *
	 * @return The (unmodifiable) map of named parameters
	 */
	@SuppressWarnings("unchecked")
	public Map getNamedParameters() {
		return Collections.unmodifiableMap( namedParameters );
	}

	/**
	 * Deserialization hook used to re-init the cached hashcode which is needed for proper clustering support.
	 *
	 * @param in The object input stream.
	 *
	 * @throws IOException Thrown by normal deserialization
	 * @throws ClassNotFoundException Thrown by normal deserialization
	 */
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		this.hashCode = generateHashCode();
	}

	private int generateHashCode() {
		int result = 13;
		result = 37 * result + ( firstRow==null ? 0 : firstRow.hashCode() );
		result = 37 * result + ( maxRows==null ? 0 : maxRows.hashCode() );
		for ( int i=0; i< positionalParameterValues.length; i++ ) {
			result = 37 * result + ( positionalParameterValues[i]==null ? 0 : positionalParameterTypes[i].getHashCode( positionalParameterValues[i] ) );
		}
		result = 37 * result + ( namedParameters==null ? 0 : namedParameters.hashCode() );
		result = 37 * result + ( filterKeys ==null ? 0 : filterKeys.hashCode() );
		result = 37 * result + ( customTransformer==null ? 0 : customTransformer.hashCode() );
		result = 37 * result + ( tenantIdentifier==null ? 0 : tenantIdentifier.hashCode() );
		result = 37 * result + sqlQueryString.hashCode();
		return result;
	}

	@Override
    public boolean equals(Object other) {
		if ( !( other instanceof QueryKey ) ) {
			return false;
		}
		QueryKey that = ( QueryKey ) other;
		if ( !sqlQueryString.equals( that.sqlQueryString ) ) {
			return false;
		}
		if ( !EqualsHelper.equals( firstRow, that.firstRow ) || !EqualsHelper.equals( maxRows, that.maxRows ) ) {
			return false;
		}
		if ( !EqualsHelper.equals( customTransformer, that.customTransformer ) ) {
			return false;
		}
		if ( positionalParameterTypes == null ) {
			if ( that.positionalParameterTypes != null ) {
				return false;
			}
		}
		else {
			if ( that.positionalParameterTypes == null ) {
				return false;
			}
			if ( positionalParameterTypes.length != that.positionalParameterTypes.length ) {
				return false;
			}
			for ( int i = 0; i < positionalParameterTypes.length; i++ ) {
				if ( positionalParameterTypes[i].getReturnedClass() != that.positionalParameterTypes[i].getReturnedClass() ) {
					return false;
				}
				if ( !positionalParameterTypes[i].isEqual( positionalParameterValues[i], that.positionalParameterValues[i] ) ) {
					return false;
				}
			}
		}

		return EqualsHelper.equals( filterKeys, that.filterKeys )
				&& EqualsHelper.equals( namedParameters, that.namedParameters )
				&& EqualsHelper.equals( tenantIdentifier, that.tenantIdentifier );
	}

	@Override
    public int hashCode() {
		return hashCode;
	}

	@Override
    public String toString() {
		StringBuilder buffer = new StringBuilder( "sql: " ).append( sqlQueryString );
		if ( positionalParameterValues != null ) {
			buffer.append( "; parameters: " );
			for ( Object positionalParameterValue : positionalParameterValues ) {
				buffer.append( positionalParameterValue ).append( ", " );
			}
		}
		if ( namedParameters != null ) {
			buffer.append( "; named parameters: " ).append( namedParameters );
		}
		if ( filterKeys != null ) {
			buffer.append( "; filterKeys: " ).append( filterKeys );
		}
		if ( firstRow != null ) {
			buffer.append( "; first row: " ).append( firstRow );
		}
		if ( maxRows != null ) {
			buffer.append( "; max rows: " ).append( maxRows );
		}
		if ( customTransformer != null ) {
			buffer.append( "; transformer: " ).append( customTransformer );
		}
		return buffer.toString();
	}

}
