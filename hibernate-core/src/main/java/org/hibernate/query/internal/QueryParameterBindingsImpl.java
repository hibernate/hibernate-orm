/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.Incubating;
import org.hibernate.QueryException;
import org.hibernate.QueryParameterException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.collections.IdentityMap;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;

/**
 * Manages the group of QueryParameterBinding for a particular query.
 *
 * @author Steve Ebersole
 * @author Chris Cranford
 */
@Incubating
public class QueryParameterBindingsImpl implements QueryParameterBindings {
	private final SessionFactoryImplementor sessionFactory;
	private final ParameterMetadataImplementor parameterMetadata;
	private final boolean queryParametersValidationEnabled;

	private Map<QueryParameter, QueryParameterBinding> parameterBindingMap;

	/**
	 * Constructs a QueryParameterBindings based on the passed information
	 *
	 * @apiNote Calls {@link #from(ParameterMetadataImplementor,SessionFactoryImplementor,boolean)}
	 * using {@link org.hibernate.boot.spi.SessionFactoryOptions#isQueryParametersValidationEnabled}
	 * as `queryParametersValidationEnabled`
	 */
	public static QueryParameterBindingsImpl from(
			ParameterMetadataImplementor parameterMetadata,
			SessionFactoryImplementor sessionFactory) {
		return from(
				parameterMetadata,
				sessionFactory,
				sessionFactory.getSessionFactoryOptions().isQueryParametersValidationEnabled()
		);
	}

	/**
	 * Constructs a QueryParameterBindings based on the passed information
	 */
	public static QueryParameterBindingsImpl from(
			ParameterMetadataImplementor parameterMetadata,
			SessionFactoryImplementor sessionFactory,
			boolean queryParametersValidationEnabled) {
		if ( parameterMetadata == null ) {
			throw new QueryParameterException( "Query parameter metadata cannot be null" );
		}

		return new QueryParameterBindingsImpl(
				sessionFactory,
				parameterMetadata,
				queryParametersValidationEnabled
		);
	}

	private QueryParameterBindingsImpl(
			SessionFactoryImplementor sessionFactory,
			ParameterMetadataImplementor parameterMetadata,
			boolean queryParametersValidationEnabled) {
		this.sessionFactory = sessionFactory;
		this.parameterMetadata = parameterMetadata;
		this.queryParametersValidationEnabled = queryParametersValidationEnabled;

		this.parameterBindingMap = CollectionHelper.concurrentMap( parameterMetadata.getParameterCount() );
	}

	@SuppressWarnings({"WeakerAccess", "unchecked"})
	protected QueryParameterBinding makeBinding(QueryParameterImplementor queryParameter) {
		if ( parameterBindingMap == null ) {
			parameterBindingMap = new IdentityHashMap<>();
		}
		else {
			assert ! parameterBindingMap.containsKey( queryParameter );
		}

		if ( ! parameterMetadata.containsReference( queryParameter ) ) {
			throw new IllegalArgumentException(
					"Cannot create binding for parameter reference [" + queryParameter + "] - reference is not a parameter of this query"
			);
		}

		final QueryParameterBinding binding = new QueryParameterBindingImpl( queryParameter, sessionFactory, null, queryParametersValidationEnabled );
		parameterBindingMap.put( queryParameter, binding );

		return binding;
	}

	@Override
	public boolean isBound(QueryParameterImplementor parameter) {
		//noinspection unchecked
		return getBinding( parameter ).isBound();
	}

	@Override
	public <P> QueryParameterBinding<P> getBinding(QueryParameterImplementor<P> parameter) {
		if ( parameterBindingMap == null ) {
			//noinspection unchecked
			return makeBinding( parameter );
		}

		QueryParameterBinding binding = parameterBindingMap.get( parameter );

		if ( binding == null ) {
			binding = makeBinding( parameter );
		}

		//noinspection unchecked
		return binding;
	}

	@Override
	public <P> QueryParameterBinding<P> getBinding(int position) {
		//noinspection unchecked
		return (QueryParameterBinding<P>) getBinding( parameterMetadata.getQueryParameter( position ) );
	}

	@Override
	public <P> QueryParameterBinding<P> getBinding(String name) {
		//noinspection unchecked
		return (QueryParameterBinding<P>) getBinding( parameterMetadata.getQueryParameter( name ) );
	}

	@Override
	public void validate() {
		parameterMetadata.visitRegistrations(
				queryParameter -> {
					if ( ! parameterBindingMap.containsKey( queryParameter ) ) {
						if ( queryParameter.getName() != null ) {
							throw new QueryException( "Named parameter not bound : " + queryParameter.getName() );
						}
						else {
							throw new QueryException( "Ordinal parameter not bound : " + queryParameter.getPosition() );
						}
					}
				}
		);
	}

	@Override
	public boolean hasAnyMultiValuedBindings() {
		for ( QueryParameterBinding binding : parameterBindingMap.values() ) {
			if ( binding.isMultiValued() ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void visitBindings(BiConsumer action) {
		parameterMetadata.visitRegistrations(
				queryParameterImplementor -> {
					//noinspection unchecked
					action.accept( queryParameterImplementor, parameterBindingMap.get( queryParameterImplementor ) );
				}
		);
	}
}
