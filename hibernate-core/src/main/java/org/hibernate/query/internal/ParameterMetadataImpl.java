/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.ArrayList;
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
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryParameterImplementor;

/**
 * Encapsulates metadata about parameters encountered within a query.
 *
 * @author Steve Ebersole
 */
public class ParameterMetadataImpl implements ParameterMetadataImplementor {
	/**
	 * Singleton access
	 */
	public static final ParameterMetadataImpl EMPTY = new ParameterMetadataImpl();

	private final Set<QueryParameterImplementor<?>> queryParameters;

	private final Set<String> names;
	private final Set<Integer> labels;


	private ParameterMetadataImpl() {
		this.queryParameters = Collections.emptySet();
		this.names = Collections.emptySet();
		this.labels = Collections.emptySet();
	}

	public ParameterMetadataImpl(Set<QueryParameterImplementor<?>> queryParameters) {
		this.queryParameters = queryParameters;

		// if we have any ordinal parameters, make sure the numbers
		// start with 1 and are contiguous

		Set<String> names = null;
		Set<Integer> labels = null;

		for ( QueryParameterImplementor<?> queryParameter : queryParameters ) {
			if ( queryParameter.getPosition() != null ) {
				if ( labels == null ) {
					labels = new HashSet<>();
				}
				labels.add( queryParameter.getPosition() );
			}
			else if ( queryParameter.getName() != null ) {
				if ( names == null ) {
					names = new HashSet<>();
				}
				names.add( queryParameter.getName() );
			}
		}

		this.labels = labels == null
				? Collections.emptySet()
				: labels;

		this.names = names == null
				? Collections.emptySet()
				: names;

		verifyOrdinalParamLabels( labels );
	}

	public ParameterMetadataImpl(
			Map<Integer, QueryParameterImplementor<?>> positionalQueryParameters,
			Map<String, QueryParameterImplementor<?>> namedQueryParameters) {
		if ( CollectionHelper.isEmpty( positionalQueryParameters )
				&& CollectionHelper.isEmpty( namedQueryParameters ) ) {
			// no parameters
			this.queryParameters = Collections.emptySet();
			this.names = Collections.emptySet();
			this.labels = Collections.emptySet();
		}
		else {
			this.queryParameters = new IdentitySet<>();
			this.queryParameters.addAll( positionalQueryParameters.values() );
			this.queryParameters.addAll( namedQueryParameters.values() );

			this.names = namedQueryParameters.keySet();
			this.labels = positionalQueryParameters.keySet();

			verifyOrdinalParamLabels( labels );
		}

	}

	private static void verifyOrdinalParamLabels(Set<Integer> labels) {
		if ( CollectionHelper.isEmpty( labels ) ) {
			return;
		}

		final List<Integer> sortedLabels = new ArrayList<>( labels );
		sortedLabels.sort( ComparableComparator.instance() );

		int lastPosition = -1;
		for ( Integer sortedPosition : sortedLabels ) {
			if ( lastPosition == -1 ) {
				if ( sortedPosition != 1 ) {
					throw new QueryException(
							String.format(
									Locale.ROOT,
									"Expected ordinal parameter labels to start with 1, but found - %s",
									sortedPosition
							)
					);
				}

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
								StringHelper.join( ",", sortedLabels.iterator() )
						)
				);
			}

			lastPosition = sortedPosition;
		}
	}

	@Override
	public int getParameterCount() {
		return queryParameters.size();
	}

	@Override
	public boolean containsReference(QueryParameter<?> parameter) {
		//noinspection SuspiciousMethodCalls
		return queryParameters.contains( parameter );
	}

	@Override
	public void visitParameters(Consumer<QueryParameterImplementor<?>> consumer) {
		queryParameters.forEach( consumer );
	}

	@Override
	public Set<QueryParameterImplementor<?>> getRegistrations() {
		return Collections.unmodifiableSet( queryParameters );
	}

	@Override
	public boolean hasAnyMatching(Predicate<QueryParameterImplementor<?>> filter) {
		for ( QueryParameterImplementor<?> queryParameter : queryParameters ) {
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
		return ! names.isEmpty();
	}

	@Override
	public Set<String> getNamedParameterNames() {
		return  names;
	}

	@Override
	public QueryParameterImplementor<?> getQueryParameter(String name) {
		for ( QueryParameterImplementor<?> queryParameter : queryParameters ) {
			if ( name.equals( queryParameter.getName() ) ) {
				return queryParameter;
			}
		}

		return null;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Positional parameter handling

	@Override
	public boolean hasPositionalParameters() {
		return ! labels.isEmpty();
	}

	public Set<Integer> getOrdinalParameterLabels() {
		return labels;
	}

	@Override
	public QueryParameterImplementor<?> getQueryParameter(int positionLabel) {
		for ( QueryParameterImplementor<?> queryParameter : queryParameters ) {
			if ( queryParameter.getPosition() != null && queryParameter.getPosition() == positionLabel ) {
				return queryParameter;
			}
		}

		return null;
	}
}
