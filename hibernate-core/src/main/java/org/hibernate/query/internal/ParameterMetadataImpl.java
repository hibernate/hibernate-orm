/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.persistence.Parameter;

import org.hibernate.QueryException;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryParameterImplementor;

/**
 * Encapsulates metadata about parameters encountered within a query.
 *
 * @author Steve Ebersole
 */
public class ParameterMetadataImpl implements ParameterMetadataImplementor<QueryParameterImplementor<?>> {
	private final Map<Integer,QueryParameterImplementor<?>> ordinalDescriptorMap;
	private final Map<String,QueryParameterImplementor<?>> namedDescriptorMap;

	public ParameterMetadataImpl(
			Map<Integer,QueryParameterImplementor<?>> ordinalDescriptorMap,
			Map<String, QueryParameterImplementor<?>> namedDescriptorMap) {
		this.ordinalDescriptorMap = ordinalDescriptorMap == null
				? Collections.emptyMap()
				: Collections.unmodifiableMap( ordinalDescriptorMap );
		this.namedDescriptorMap = namedDescriptorMap == null
				? Collections.emptyMap()
				: Collections.unmodifiableMap( namedDescriptorMap );

		if (ordinalDescriptorMap != null &&  ! ordinalDescriptorMap.isEmpty() ) {
			final List<Integer> sortedPositions = new ArrayList<>( ordinalDescriptorMap.keySet() );
			sortedPositions.sort( ComparableComparator.INSTANCE );

			int lastPosition = -1;
			for ( Integer sortedPosition : sortedPositions ) {
				if ( lastPosition == -1 ) {
					lastPosition = sortedPosition;
					continue;
				}

				if ( sortedPosition != lastPosition + 1 ) {
					throw new QueryException(
							String.format(
									Locale.ROOT,
									"Unexpected gap in ordinal parameter labels [%s -> %s] : [%s]",
									lastPosition,
									sortedPosition,
									StringHelper.join( ",", sortedPositions.iterator() )
							)
					);
				}

				lastPosition = sortedPosition;
			}

		}
	}

	@Override
	public int getParameterCount() {
		return ordinalDescriptorMap.size() + namedDescriptorMap.size();
	}

	@Override
	@SuppressWarnings("SuspiciousMethodCalls")
	public boolean containsReference(QueryParameterImplementor<?> parameter) {
		return ordinalDescriptorMap.containsValue( parameter )
				|| namedDescriptorMap.containsValue( parameter );
	}

	@Override
	public void visitRegistrations(Consumer<QueryParameterImplementor<?>> action) {
		ordinalDescriptorMap.values().forEach( action );
		namedDescriptorMap.values().forEach( action );
	}

	@Override
	public void collectAllParameters(ParameterCollector<QueryParameterImplementor<?>> collector) {
		ordinalDescriptorMap.values().forEach( collector::collect );
		namedDescriptorMap.values().forEach( collector::collect );
	}

	@Override
	public Set<QueryParameterImplementor<?>> getRegistrations() {
		if ( ! hasNamedParameters() && ! hasPositionalParameters() ) {
			return Collections.emptySet();
		}

		final HashSet<QueryParameterImplementor<?>> allParameters = new HashSet<>();
		collectAllParameters( allParameters::add );
		return allParameters;
	}

	@Override
	public boolean hasAnyMatching(Predicate<QueryParameterImplementor<?>> filter) {
		for ( QueryParameterImplementor<?> queryParameter : ordinalDescriptorMap.values() ) {
			if ( filter.test( queryParameter ) ) {
				return true;
			}
		}

		for ( QueryParameterImplementor<?> queryParameter : namedDescriptorMap.values() ) {
			if ( filter.test( queryParameter ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public QueryParameterImplementor<?> resolve(Parameter param) {
		if ( param instanceof QueryParameterImplementor ) {
			return (QueryParameterImplementor) param;
		}

		throw new IllegalArgumentException( "Could not resolve javax.persistence.Parameter to org.hibernate.query.QueryParameter" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named parameter handling

	@Override
	public boolean hasNamedParameters() {
		return !namedDescriptorMap.isEmpty();
	}

	@Override
	public int getNamedParameterCount() {
		return namedDescriptorMap.size();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<QueryParameterImplementor<?>> getNamedParameters() {
		return Collections.unmodifiableCollection( namedDescriptorMap.values() );
	}

	@Override
	public Set<String> getNamedParameterNames() {
		return  namedDescriptorMap.keySet();
	}

	@Override
	public QueryParameterImplementor<?> getQueryParameter(String name) {
		return namedDescriptorMap.get( name );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Positional parameter handling

	@Override
	public boolean hasPositionalParameters() {
		return getPositionalParameterCount() > 0;
	}

	@Override
	public int getPositionalParameterCount() {
		return ordinalDescriptorMap.size();
	}


	public Set<Integer> getOrdinalParameterLabels() {
		return ordinalDescriptorMap.keySet();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<QueryParameterImplementor<?>> getPositionalParameters() {
		return Collections.unmodifiableCollection( ordinalDescriptorMap.values() );
	}

	@Override
	public QueryParameterImplementor<?> getQueryParameter(int positionLabel) {
		return ordinalDescriptorMap.get( positionLabel );
	}
}
