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

import org.hibernate.QueryException;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.collections.LinkedIdentityHashMap;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.query.BindableType;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.UnknownParameterException;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.tree.expression.SqmParameter;

import jakarta.persistence.Parameter;

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

	private final Map<QueryParameterImplementor<?>, List<SqmParameter<?>>> queryParameters;

	private final Set<String> names;
	private final Set<Integer> labels;


	private ParameterMetadataImpl() {
		this.queryParameters = Collections.emptyMap();
		this.names = Collections.emptySet();
		this.labels = Collections.emptySet();
	}

	public ParameterMetadataImpl(Map<QueryParameterImplementor<?>, List<SqmParameter<?>>> queryParameters) {
		this.queryParameters = queryParameters;

		// if we have any ordinal parameters, make sure the numbers
		// start with 1 and are contiguous

		Set<String> names = null;
		Set<Integer> labels = null;

		for ( QueryParameterImplementor<?> queryParameter : queryParameters.keySet() ) {
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
			this.queryParameters = Collections.emptyMap();
			this.names = Collections.emptySet();
			this.labels = Collections.emptySet();
		}
		else {
			this.queryParameters = new LinkedIdentityHashMap<>();
			if ( positionalQueryParameters != null ) {
				for ( QueryParameterImplementor<?> value : positionalQueryParameters.values() ) {
					this.queryParameters.put( value, Collections.emptyList() );
				}
				this.labels = positionalQueryParameters.keySet();
				verifyOrdinalParamLabels( labels );
			}
			else {
				labels = null;
			}
			if ( namedQueryParameters != null ) {
				for ( QueryParameterImplementor<?> value : namedQueryParameters.values() ) {
					this.queryParameters.put( value, Collections.emptyList() );
				}
				this.names = namedQueryParameters.keySet();
			}
			else {
				this.names = null;
			}
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
	public <T> BindableType<T> getInferredParameterType(QueryParameter<T> parameter) {
		final List<SqmParameter<?>> sqmParameters = queryParameters.get( parameter );
		if ( sqmParameters == null || sqmParameters.isEmpty() ) {
			return null;
		}
		for ( SqmParameter<?> sqmParameter : sqmParameters ) {
			final BindableType<T> nodeType = (BindableType<T>) sqmParameter.getNodeType();
			if ( nodeType != null ) {
				return nodeType;
			}
		}
		return null;
	}

	@Override
	public boolean containsReference(QueryParameter<?> parameter) {
		//noinspection SuspiciousMethodCalls
		return queryParameters.containsKey( parameter );
	}

	@Override
	public void visitParameters(Consumer<QueryParameterImplementor<?>> consumer) {
		queryParameters.keySet().forEach( consumer );
	}

	@Override
	public Set<QueryParameterImplementor<?>> getRegistrations() {
		return Collections.unmodifiableSet( queryParameters.keySet() );
	}

	@Override
	public boolean hasAnyMatching(Predicate<QueryParameterImplementor<?>> filter) {
		for ( QueryParameterImplementor<?> queryParameter : queryParameters.keySet() ) {
			if ( filter.test( queryParameter ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public <P> QueryParameterImplementor<P> resolve(Parameter<P> param) {
		if ( param instanceof QueryParameterImplementor ) {
			return (QueryParameterImplementor<P>) param;
		}

		final String errorMessage = "Could not resolve jakarta.persistence.Parameter `" + param + "` to org.hibernate.query.QueryParameter";
		throw new IllegalArgumentException(
				errorMessage,
				new UnknownParameterException( errorMessage )
		);
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
	public QueryParameterImplementor<?> findQueryParameter(String name) {
		for ( QueryParameterImplementor<?> queryParameter : queryParameters.keySet() ) {
			if ( name.equals( queryParameter.getName() ) ) {
				return queryParameter;
			}
		}
		return null;
	}

	@Override
	public QueryParameterImplementor<?> getQueryParameter(String name) {
		final QueryParameterImplementor<?> parameter = findQueryParameter( name );

		if ( parameter != null ) {
			return parameter;
		}

		final String errorMessage = String.format(
				Locale.ROOT,
				"Could not locate named parameter [%s], expecting one of [%s]",
				name,
				String.join( ", ", names )
		);
		throw new IllegalArgumentException(
				errorMessage,
				new UnknownParameterException( errorMessage )
		);
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
	public QueryParameterImplementor<?> findQueryParameter(int positionLabel) {
		for ( QueryParameterImplementor<?> queryParameter : queryParameters.keySet() ) {
			if ( queryParameter.getPosition() != null && queryParameter.getPosition() == positionLabel ) {
				return queryParameter;
			}
		}
		return null;
	}

	@Override
	public QueryParameterImplementor<?> getQueryParameter(int positionLabel) {
		final QueryParameterImplementor<?> queryParameter = findQueryParameter( positionLabel );

		if ( queryParameter != null ) {
			return queryParameter;
		}

		final String errorMessage = String.format(
				Locale.ROOT,
				"Could not locate ordinal parameter [%s], expecting one of [%s]",
				positionLabel,
				StringHelper.join( ", ", labels )
		);
		throw new IllegalArgumentException(
				errorMessage,
				new UnknownParameterException( errorMessage )
		);
	}
}
