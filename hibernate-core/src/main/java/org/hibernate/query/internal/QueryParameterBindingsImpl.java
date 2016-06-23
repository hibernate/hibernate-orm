/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.QueryException;
import org.hibernate.QueryParameterException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.query.spi.NamedParameterDescriptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.hql.internal.classic.ParserHelper;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterListBinding;
import org.hibernate.type.SerializableType;
import org.hibernate.type.Type;

/**
 * Manages the group of QueryParameterBinding for a particular query.
 *
 * @author Steve Ebersole
 * @author Chris Cranford
 */
@Incubating
public class QueryParameterBindingsImpl implements QueryParameterBindings {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( QueryParameterBindingsImpl.class );

	private final SessionFactoryImplementor sessionFactory;
	private final ParameterMetadata parameterMetadata;

	private Map<QueryParameter, QueryParameterBinding> parameterBindingMap;
	private Map<QueryParameter, QueryParameterListBinding> parameterListBindingMap;
	private Map<Integer, QueryParameterBinding> positionalParameterBindings;

	public static QueryParameterBindingsImpl from(ParameterMetadata parameterMetadata,
			SessionFactoryImplementor sessionFactory) {
		if ( parameterMetadata == null ) {
			return new QueryParameterBindingsImpl( sessionFactory, parameterMetadata );
		}
		else {
			return new QueryParameterBindingsImpl( sessionFactory, parameterMetadata.collectAllParameters(), parameterMetadata );
		}
	}

	public QueryParameterBindingsImpl(SessionFactoryImplementor sessionFactory, ParameterMetadata parameterMetadata) {
		this( sessionFactory, Collections.emptySet(), parameterMetadata );
	}

	public QueryParameterBindingsImpl(SessionFactoryImplementor sessionFactory,
			Set<QueryParameter<?>> queryParameters,
			ParameterMetadata parameterMetadata) {
		this.sessionFactory = sessionFactory;
		this.parameterMetadata = parameterMetadata;
		this.positionalParameterBindings = new TreeMap<>(  );

		if ( queryParameters == null || queryParameters.isEmpty() ) {
			parameterBindingMap = Collections.emptyMap();
		}
		else {
			parameterBindingMap = new HashMap<>();

			for ( QueryParameter queryParameter : queryParameters ) {
				if ( queryParameter.getPosition() == null ) {
					// only cache the non-positional parameters in this map
					// positional parameters will be bound dynamically with getBinding(int)
					parameterBindingMap.put( queryParameter, makeBinding( queryParameter ) );
				}
			}
		}

		parameterListBindingMap = new HashMap<>();
	}

	protected QueryParameterBinding makeBinding(QueryParameter queryParameter) {
		return makeBinding( queryParameter.getType() );
	}

	protected QueryParameterBinding makeBinding(Type bindType) {
		return new QueryParameterBindingImpl( bindType, sessionFactory );
	}

	public boolean isBound(QueryParameter parameter) {
		final QueryParameterBinding binding = locateBinding( parameter );
		if ( binding != null ) {
			return binding.getBindValue() != null;
		}

		final QueryParameterListBinding listBinding = locateQueryParameterListBinding( parameter );
		if ( listBinding != null ) {
			return listBinding.getBindValues() != null;
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	public <T> QueryParameterBinding<T> getBinding(QueryParameter<T> parameter) {
		final QueryParameterBinding<T> binding = locateBinding( parameter );

		if ( binding == null ) {
			throw new IllegalArgumentException(
					"Could not resolve QueryParameter reference [" + parameter + "] to QueryParameterBinding"
			);
		}

		return binding;
	}

	@SuppressWarnings("unchecked")
	public <T> QueryParameterBinding<T> locateBinding(QueryParameter<T> parameter) {
		// see if this exact instance is known as a key
		if ( parameterBindingMap.containsKey( parameter ) ) {
			return parameterBindingMap.get( parameter );
		}

		// if the incoming parameter has a name, try to find it by name
		if ( StringHelper.isNotEmpty( parameter.getName() ) ) {
			final QueryParameterBinding binding = locateBinding( parameter.getName() );
			if ( binding != null ) {
				return binding;
			}
		}

		// if the incoming parameter has a position, try to find it by position
		if ( parameter.getPosition() != null ) {
			final QueryParameterBinding binding = locateBinding( parameter.getPosition() );
			if ( binding != null ) {
				return binding;
			}
		}

		return null;
	}

	protected QueryParameterBinding locateBinding(String name) {
		for ( Map.Entry<QueryParameter, QueryParameterBinding> entry : parameterBindingMap.entrySet() ) {
			if ( name.equals( entry.getKey().getName() ) ) {
				return entry.getValue();
			}
		}

		return null;
	}

	protected QueryParameterBinding locateAndRemoveBinding(String name) {
		final Iterator<Map.Entry<QueryParameter, QueryParameterBinding>> entryIterator = parameterBindingMap.entrySet().iterator();
		while ( entryIterator.hasNext() ) {
			final Map.Entry<QueryParameter, QueryParameterBinding> entry = entryIterator.next();
			if ( name.equals( entry.getKey().getName() ) ) {
				entryIterator.remove();
				return entry.getValue();
			}
		}

		return null;
	}

	protected QueryParameterBinding locateBinding(int position) {
		if ( position < positionalParameterBindings.size() ) {
			return positionalParameterBindings.get( position );
		}

		return null;
	}

	public QueryParameterBinding getBinding(String name) {
		final QueryParameterBinding binding = locateBinding( name );
		if ( binding == null ) {
			throw new IllegalArgumentException( "Unknown parameter name : " + name );
		}

		return binding;
	}

	public QueryParameterBinding getBinding(int position) {
		int positionAdjustment = 0;
		if ( !parameterMetadata.isOrdinalParametersZeroBased() ) {
			positionAdjustment = -1;
		}
		QueryParameterBinding binding = null;
		if ( parameterMetadata != null ) {
			if ( !parameterMetadata.hasPositionalParameters() ) {
				// no positional parameters, assume jpa named.
				binding = locateBinding( Integer.toString( position ) );
			}
			else {
				try {
					binding = positionalParameterBindings.get( position + positionAdjustment );
					if ( binding == null ) {
						binding = makeBinding( parameterMetadata.getQueryParameter( position ) );
						positionalParameterBindings.put( position + positionAdjustment, binding );
					}
				}
				catch (QueryParameterException e) {
					// treat this as null binding
				}
			}
		}

		if ( binding == null ) {
			throw new IllegalArgumentException( "Unknown parameter position: " + position );
		}

		return binding;
	}

	public void verifyParametersBound(boolean reserveFirstParameter) {
		// verify named parameters bound
		for ( Map.Entry<QueryParameter, QueryParameterBinding> bindEntry : parameterBindingMap.entrySet() ) {
			if ( !bindEntry.getValue().isBound() ) {
				if ( bindEntry.getKey().getName() != null ) {
					throw new QueryException( "Named parameter [" + bindEntry.getKey().getName() + "] not set" );
				}
				else {
					throw new QueryException( "Parameter memento [" + bindEntry.getKey() + "] not set" );
				}
			}
		}
		// verify position parameters bound
		int startIndex = 0;
		if ( !parameterMetadata.isOrdinalParametersZeroBased() ) {
			startIndex = 1;
		}
		for ( int i = startIndex; i < positionalParameterBindings.size(); i++ ) {
			QueryParameterBinding binding = null;
			if ( parameterMetadata.isOrdinalParametersZeroBased() ) {
				binding = positionalParameterBindings.get( i );
			}
			else {
				binding = positionalParameterBindings.get( i - 1 );
			}
			if ( binding == null || !binding.isBound() ) {
				throw new QueryException( "Positional parameter [" + i + "] not set" );
			}
		}
		// verify position parameter count is correct
		final int positionalValueSpan = calculatePositionalValueSpan( reserveFirstParameter );
		final int positionCounts = parameterMetadata.getPositionalParameterCount();
		if ( positionCounts != positionalValueSpan ) {
			if ( reserveFirstParameter && positionCounts - 1 != positionalValueSpan ) {
				throw new QueryException(
						"Expected positional parameter count: " +
								( positionCounts - 1 ) +
								", actually detected " + positionalValueSpan
				);
			}
			else if ( !reserveFirstParameter ) {
				throw new QueryException(
						"Expected positional parameter count: " +
								( positionCounts ) +
								", actually detected " + positionalValueSpan
				);
			}
		}
	}

	private int calculatePositionalValueSpan(boolean reserveFirstParameter) {
		int positionalValueSpan = 0;
		for ( QueryParameterBinding binding : positionalParameterBindings.values() ) {
			if ( binding.isBound() ) {
				Type bindType = binding.getBindType();
				if ( bindType == null ) {
					bindType = SerializableType.INSTANCE;
				}
				positionalValueSpan += bindType.getColumnSpan( sessionFactory );
			}
		}
		return positionalValueSpan;
	}

	/**
	 * @deprecated (since 5.2) expect a different approach to org.hibernate.engine.spi.QueryParameters in 6.0
	 */
	@Deprecated
	public Collection<Type> collectBindTypes() {
		return parameterBindingMap.values()
				.stream()
				.map( QueryParameterBinding::getBindType )
				.collect( Collectors.toList() );
	}

	/**
	 * @deprecated (since 5.2) expect a different approach to org.hibernate.engine.spi.QueryParameters in 6.0
	 */
	@Deprecated
	public Collection<Object> collectBindValues() {
		return parameterBindingMap.values()
				.stream()
				.map( QueryParameterBinding::getBindValue )
				.collect( Collectors.toList() );
	}

	/**
	 * @deprecated (since 5.2) expect a different approach to org.hibernate.engine.spi.QueryParameters in 6.0
	 */
	@Deprecated
	public Type[] collectPositionalBindTypes() {
		Type[] types = new Type[ positionalParameterBindings.size() ];

		// NOTE : bindings should be ordered by position by nature of a TreeMap...
		// NOTE : we also assume the contiguity of the positions

		for ( Map.Entry<Integer, QueryParameterBinding> entry : positionalParameterBindings.entrySet() ) {
			final int position = entry.getKey();

			Type type = entry.getValue().getBindType();
			if ( type == null ) {
				log.debugf( "Binding for positional-parameter [%s] did not define type, using SerializableType", position );
				type = SerializableType.INSTANCE;
			}

			types[ position ] = type;
		}

		return types;
	}

	/**
	 * @deprecated (since 5.2) expect a different approach to org.hibernate.engine.spi.QueryParameters in 6.0
	 */
	@Deprecated
	public Object[] collectPositionalBindValues() {
		Object[] values = new Object[ positionalParameterBindings.size() ];

		// NOTE : bindings should be ordered by position by nature of a TreeMap...
		// NOTE : we also assume the contiguity of the positions

		for ( Map.Entry<Integer, QueryParameterBinding> entry : positionalParameterBindings.entrySet() ) {
			final int position = entry.getKey();
			values[ position ] = entry.getValue().getBindValue();
		}

		return values;
	}

	/**
	 * @deprecated (since 5.2) expect a different approach to org.hibernate.engine.spi.QueryParameters in 6.0
	 */
	@Deprecated
	public Map<String, TypedValue> collectNamedParameterBindings() {
		Map<String, TypedValue> collectedBindings = new HashMap<>();
		for ( Map.Entry<QueryParameter, QueryParameterBinding> entry : parameterBindingMap.entrySet() ) {
			if ( entry.getKey().getName() == null ) {
				continue;
			}

			Type bindType = entry.getValue().getBindType();
			if ( bindType == null ) {
				log.debugf( "Binding for named-parameter [%s] did not define type", entry.getKey().getName() );
				bindType = SerializableType.INSTANCE;
			}

			collectedBindings.put(
					entry.getKey().getName(),
					new TypedValue( bindType, entry.getValue().getBindValue() )
			);
		}

		return collectedBindings;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter list binding - expect changes in 6.0

	/**
	 * @deprecated (since 5.2) expected changes to "collection-valued parameter binding" in 6.0
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	public <T> QueryParameterListBinding<T> getQueryParameterListBinding(QueryParameter<T> queryParameter) {
		QueryParameterListBinding result = parameterListBindingMap.get( queryParameter );
		if ( result == null ) {
			result = transformQueryParameterBindingToQueryParameterListBinding( queryParameter );
		}
		return result;
	}

	/**
	 * @deprecated (since 5.2) expected changes to "collection-valued parameter binding" in 6.0
	 */
	@Deprecated
	private QueryParameterListBinding locateQueryParameterListBinding(QueryParameter queryParameter) {
		QueryParameterListBinding result = parameterListBindingMap.get( queryParameter );

		if ( result == null && queryParameter.getName() != null ) {
			for ( Map.Entry<QueryParameter, QueryParameterListBinding> entry : parameterListBindingMap.entrySet() ) {
				if ( queryParameter.getName().equals( entry.getKey().getName() ) ) {
					result = entry.getValue();
					break;
				}
			}
		}

		return result;
	}

	/**
	 * @deprecated (since 5.2) expected changes to "collection-valued parameter binding" in 6.0
	 */
	@Deprecated
	private <T> QueryParameterListBinding<T> transformQueryParameterBindingToQueryParameterListBinding(QueryParameter<T> queryParameter) {
		log.debugf( "Converting QueryParameterBinding to QueryParameterListBinding for given QueryParameter : %s", queryParameter );
		final QueryParameterBinding binding = getAndRemoveBinding( queryParameter );
		if ( binding == null ) {
			throw new IllegalArgumentException(
					"Could not locate QueryParameterBinding for given QueryParameter : " + queryParameter +
							"; parameter list must be defined using named parameter"
			);
		}

		final QueryParameterListBinding<T> convertedBinding = new QueryParameterListBindingImpl<>( binding.getBindType() );
		parameterListBindingMap.put( queryParameter, convertedBinding );

		return convertedBinding;
	}

	/**
	 * @deprecated (since 5.2) expected changes to "collection-valued parameter binding" in 6.0
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	private <T> QueryParameterBinding<T> getAndRemoveBinding(QueryParameter<T> parameter) {
		// see if this exact instance is known as a key
		if ( parameterBindingMap.containsKey( parameter ) ) {
			return parameterBindingMap.remove( parameter );
		}

		// if the incoming parameter has a name, try to find it by name
		if ( StringHelper.isNotEmpty( parameter.getName() ) ) {
			final QueryParameterBinding binding = locateAndRemoveBinding( parameter.getName() );
			if ( binding != null ) {
				return binding;
			}
		}

		// NOTE : getAndRemoveBinding is only intended for usage from #transformQueryParameterBindingToQueryParameterListBinding
		//		which only supports named parameters, so there is no need to look into legacy positional parameters

		throw new IllegalArgumentException(
				"Could not resolve QueryParameter reference [" + parameter + "] to QueryParameterBinding"
		);
	}

	/**
	 * @deprecated (since 5.2) expected changes to "collection-valued parameter binding" in 6.0
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	public <T> QueryParameterListBinding<T> getQueryParameterListBinding(String name) {
		// find the QueryParameter instance for the given name
		final QueryParameter<T> queryParameter = resolveQueryParameter( name );
		return getQueryParameterListBinding( queryParameter );
	}

	/**
	 * @deprecated (since 5.2) expected changes to "collection-valued parameter binding" in 6.0
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	private <T> QueryParameter<T> resolveQueryParameter(String name) {
		for ( QueryParameter queryParameter : parameterListBindingMap.keySet() ) {
			if ( name.equals( queryParameter.getName() ) ) {
				return queryParameter;
			}
		}

		for ( QueryParameter queryParameter : parameterBindingMap.keySet() ) {
			if ( name.equals( queryParameter.getName() ) ) {
				return queryParameter;
			}
		}

		throw new IllegalArgumentException(
				"Unable to resolve given parameter name [" + name + "] to QueryParameter reference"
		);
	}

	/**
	 * @deprecated (since 5.2) expected changes to "collection-valued parameter binding" in 6.0
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	public String expandListValuedParameters(String queryString, SharedSessionContractImplementor session) {
		if ( queryString == null ) {
			return null;
		}

		// more-or-less... for each entry in parameterListBindingMap we will create an
		//		entry in parameterBindingMap for each of the values in the bound value list.  afterwards
		//		we will clear the parameterListBindingMap.
		//
		// NOTE that this is essentially the legacy logical prior to modeling QueryParameterBinding/QueryParameterListBinding.
		// 		Fully expect the details of how this is handled in 6.0

		// HHH-1123
		// Some DBs limit number of IN expressions.  For now, warn...
		final Dialect dialect = session.getFactory().getServiceRegistry().getService( JdbcServices.class ).getJdbcEnvironment().getDialect();
		final int inExprLimit = dialect.getInExpressionCountLimit();

		for ( Map.Entry<QueryParameter, QueryParameterListBinding> entry : parameterListBindingMap.entrySet() ) {
			final NamedParameterDescriptor sourceParam = (NamedParameterDescriptor) entry.getKey();
			final Collection bindValues = entry.getValue().getBindValues();

			if ( inExprLimit > 0 && bindValues.size() > inExprLimit ) {
				log.tooManyInExpressions( dialect.getClass().getName(), inExprLimit, sourceParam.getName(), bindValues.size() );
			}

			final boolean isJpaPositionalParam = sourceParam.isJpaPositionalParameter();
			final String paramPrefix = isJpaPositionalParam ? "?" : ParserHelper.HQL_VARIABLE_PREFIX;
			final String placeholder = paramPrefix + sourceParam.getName();
			final int loc = queryString.indexOf( placeholder );

			if ( loc < 0 ) {
				continue;
			}

			final String beforePlaceholder = queryString.substring( 0, loc );
			final String afterPlaceholder = queryString.substring( loc + placeholder.length() );

			// check if placeholder is already immediately enclosed in parentheses
			// (ignoring whitespace)
			boolean isEnclosedInParens =
					StringHelper.getLastNonWhitespaceCharacter( beforePlaceholder ) == '(' &&
							StringHelper.getFirstNonWhitespaceCharacter( afterPlaceholder ) == ')';

			if ( bindValues.size() == 1 && isEnclosedInParens ) {
				// short-circuit for performance when only 1 value and the
				// placeholder is already enclosed in parentheses...
				final QueryParameterBinding syntheticBinding = makeBinding( entry.getValue().getBindType() );
				syntheticBinding.setBindValue( bindValues.iterator().next() );
				parameterBindingMap.put( sourceParam, syntheticBinding );
				continue;
			}

			StringBuilder expansionList = new StringBuilder();

			int i = 0;
			for ( Object bindValue : entry.getValue().getBindValues() ) {
				// for each value in the bound list-of-values we:
				//		1) create a synthetic named parameter
				//		2) expand the queryString to include each synthetic named param in place of the original
				//		3) create a new synthetic binding for just that single value under the synthetic name
				final String syntheticName = ( isJpaPositionalParam ? 'x' : "" ) + sourceParam.getName() + '_' + i;
				if ( i > 0 ) {
					expansionList.append( ", " );
				}
				expansionList.append( ParserHelper.HQL_VARIABLE_PREFIX ).append( syntheticName );
				final QueryParameter syntheticParam = new QueryParameterNamedImpl<>(
						syntheticName,
						sourceParam.getSourceLocations(),
						sourceParam.isJpaPositionalParameter(),
						sourceParam.getType()
				);
				final QueryParameterBinding syntheticBinding = makeBinding( entry.getValue().getBindType() );
				syntheticBinding.setBindValue( bindValue );
				parameterBindingMap.put( syntheticParam, syntheticBinding );
				i++;
			}

			queryString = StringHelper.replace(
					beforePlaceholder,
					afterPlaceholder,
					placeholder,
					expansionList.toString(),
					true,
					true
			);
		}

		return queryString;
	}
}
