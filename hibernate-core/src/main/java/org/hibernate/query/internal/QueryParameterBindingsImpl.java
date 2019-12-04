/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.persistence.Parameter;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.QueryException;
import org.hibernate.QueryParameterException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.query.spi.NamedParameterDescriptor;
import org.hibernate.engine.query.spi.OrdinalParameterDescriptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.MathHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
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
	private final boolean queryParametersValidationEnabled;

	private final int ordinalParamValueOffset;

	private final int jdbcStyleOrdinalCountBase;

	private Map<QueryParameter, QueryParameterBinding> parameterBindingMap;
	private Map<QueryParameter, QueryParameterListBinding> parameterListBindingMap;
	private Set<QueryParameter> parametersConvertedToListBindings;

	public static QueryParameterBindingsImpl from(
			ParameterMetadata parameterMetadata,
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
			ParameterMetadata parameterMetadata,
			boolean queryParametersValidationEnabled) {
		this.sessionFactory = sessionFactory;
		this.parameterMetadata = parameterMetadata;
		this.queryParametersValidationEnabled = queryParametersValidationEnabled;

		this.parameterBindingMap = CollectionHelper.concurrentMap( parameterMetadata.getParameterCount() );

		this.jdbcStyleOrdinalCountBase = sessionFactory.getSessionFactoryOptions().jdbcStyleParamsZeroBased() ? 0 : 1;

		if ( parameterMetadata.hasPositionalParameters() ) {
			int smallestOrdinalParamLabel = Integer.MAX_VALUE;
			for ( QueryParameter queryParameter : parameterMetadata.getPositionalParameters() ) {
				if ( queryParameter.getPosition() == null ) {
					throw new HibernateException( "Non-ordinal parameter ended up in ordinal param list" );
				}

				if ( queryParameter.getPosition() < smallestOrdinalParamLabel ) {
					smallestOrdinalParamLabel = queryParameter.getPosition();
				}
			}
			ordinalParamValueOffset = smallestOrdinalParamLabel;
		}
		else {
			ordinalParamValueOffset = 0;
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected QueryParameterBinding makeBinding(QueryParameter queryParameter) {
		assert ! parameterBindingMap.containsKey( queryParameter );

		if ( ! parameterMetadata.containsReference( queryParameter ) ) {
			throw new IllegalArgumentException(
					"Cannot create binding for parameter reference [" + queryParameter + "] - reference is not a parameter of this query"
			);
		}

		final QueryParameterBinding binding = makeBinding( queryParameter.getHibernateType() );
		parameterBindingMap.put( queryParameter, binding );

		return binding;
	}

	@SuppressWarnings("WeakerAccess")
	protected QueryParameterBinding makeBinding(Type bindType) {
		return new QueryParameterBindingImpl( bindType, sessionFactory, shouldValidateBindingValue() );
	}

	@SuppressWarnings({"unchecked", "WeakerAccess"})
	protected <T> QueryParameterListBinding<T> makeListBinding(QueryParameter<T> param) {
		if ( parametersConvertedToListBindings == null ) {
			parametersConvertedToListBindings = new HashSet<>();
		}

		parametersConvertedToListBindings.add( param );

		if ( parameterListBindingMap == null ) {
			parameterListBindingMap = new HashMap<>();
		}

		return parameterListBindingMap.computeIfAbsent(
				param,
				p -> new QueryParameterListBindingImpl(
						param.getHibernateType(),
						shouldValidateBindingValue()
				)
		);
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public boolean isBound(QueryParameter parameter) {
		final QueryParameterBinding binding = getBinding( parameter );

		return binding.isBound();
	}

	@SuppressWarnings("unchecked")
	public <T> QueryParameterBinding<T> getBinding(QueryParameter<T> parameter) {
		QueryParameterBinding<T> binding = parameterBindingMap.get( parameter );

		if ( binding == null ) {
			if ( ! parameterMetadata.containsReference( parameter ) ) {
				throw new IllegalArgumentException(
						"Could not resolve QueryParameter reference [" + parameter + "] to QueryParameterBinding"
				);
			}

			binding = makeBinding( parameter );
		}

		return binding;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryParameterBinding getBinding(int position) {
		return getBinding( parameterMetadata.getQueryParameter( position ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryParameterBinding getBinding(String name) {
		return getBinding( parameterMetadata.getQueryParameter( name ) );
	}

	public void verifyParametersBound(boolean reserveFirstParameter) {
		for ( QueryParameter<?> parameter : parameterMetadata.collectAllParameters() ) {
			// check the "normal" bindings
			if ( parameterBindingMap.containsKey( parameter ) ) {
				continue;
			}

			// next check the "list" bindings
			if ( parameterListBindingMap != null
					&& parameterListBindingMap.containsKey( parameter ) ) {
				continue;
			}

			if ( parametersConvertedToListBindings != null
					&& parametersConvertedToListBindings.contains( parameter ) ) {
				continue;
			}

			if ( parameter.getName() != null ) {
				throw new QueryException( "Named parameter not bound : " + parameter.getName() );
			}
			else {
				throw new QueryException( "Ordinal parameter not bound : " + parameter.getPosition() );
			}
		}
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
		return ArrayHelper.EMPTY_TYPE_ARRAY;
//		if ( ! parameterMetadata.hasPositionalParameters() ) {
//			return ArrayHelper.EMPTY_TYPE_ARRAY;
//		}
//
//		// callers expect these in ordinal order.  In a way that is natural, but at the same
//		// time long term a way to find types/values by name/position would be better
//
//		final TreeMap<QueryParameter, QueryParameterBinding> sortedPositionalParamBindings = getSortedPositionalParamBindingMap();
//		final List<Type> types = CollectionHelper.arrayList( sortedPositionalParamBindings.size() );
//
//		for ( Map.Entry<QueryParameter, QueryParameterBinding> entry : sortedPositionalParamBindings.entrySet() ) {
//			if ( entry.getKey().getPosition() == null ) {
//				continue;
//			}
//
//			Type type = entry.getValue().getBindType();
//			if ( type == null ) {
//				type = entry.getKey().getType();
//			}
//
//			if ( type == null ) {
//				log.debugf(
//						"Binding for positional-parameter [%s] did not define type, using SerializableType",
//						entry.getKey().getPosition()
//				);
//				type = SerializableType.INSTANCE;
//			}
//
//			types.add( type );
//		}
//
//		return types.toArray( new Type[ types.size() ] );
	}

	private TreeMap<QueryParameter, QueryParameterBinding> getSortedPositionalParamBindingMap() {
		final TreeMap<QueryParameter, QueryParameterBinding> map = new TreeMap<>( Comparator.comparing( Parameter::getPosition ) );

		for ( Map.Entry<QueryParameter, QueryParameterBinding> entry : parameterBindingMap.entrySet() ) {
			if ( entry.getKey().getPosition() == null ) {
				continue;
			}

			map.put( entry.getKey(), entry.getValue() );
		}

		return map;
	}

	private static final Object[] EMPTY_VALUES = new Object[0];

	/**
	 * @deprecated (since 5.2) expect a different approach to org.hibernate.engine.spi.QueryParameters in 6.0
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	public Object[] collectPositionalBindValues() {
		return EMPTY_VALUES;
//		if ( ! parameterMetadata.hasPositionalParameters() ) {
//			return EMPTY_VALUES;
//		}
//
//		final TreeMap<QueryParameter, QueryParameterBinding> sortedPositionalParamBindings = getSortedPositionalParamBindingMap();
//		final List values = CollectionHelper.arrayList( sortedPositionalParamBindings.size() );
//
//		for ( Map.Entry<QueryParameter, QueryParameterBinding> entry : sortedPositionalParamBindings.entrySet() ) {
//			if ( entry.getKey().getPosition() == null ) {
//				continue;
//			}
//			values.add( entry.getValue().getBindValue() );
//		}
//
//		return values.toArray( new Object[values.size()] );
	}

	/**
	 * @deprecated (since 5.2) expect a different approach to org.hibernate.engine.spi.QueryParameters in 6.0
	 */
	@Deprecated
	public Map<String, TypedValue> collectNamedParameterBindings() {
		final Map<String, TypedValue> collectedBindings = new HashMap<>();

		for ( Map.Entry<QueryParameter, QueryParameterBinding> entry : parameterBindingMap.entrySet() ) {
			final String key;
			if ( entry.getKey().getPosition() != null ) {
				key = Integer.toString( entry.getKey().getPosition() );
			}
			else {
				key = entry.getKey().getName();
			}

			Type bindType = entry.getValue().getBindType();
			if ( bindType == null ) {
				log.debugf( "Binding for parameter [%s] did not define type", key );
				bindType = SerializableType.INSTANCE;
			}

			collectedBindings.put(
					key,
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
		if ( parameterListBindingMap == null ) {
			parameterListBindingMap = new HashMap<>();
		}

		return transformQueryParameterBindingToQueryParameterListBinding( queryParameter );
	}

	/**
	 * @deprecated (since 5.2) expected changes to "collection-valued parameter binding" in 6.0
	 */
	@Deprecated
	private QueryParameterListBinding locateQueryParameterListBinding(QueryParameter queryParameter) {
		if ( parameterListBindingMap == null ) {
			parameterListBindingMap = new HashMap<>();
		}

		QueryParameterListBinding binding = parameterListBindingMap.get( queryParameter );
		if ( binding == null ) {
			QueryParameter resolved = resolveParameter( queryParameter );
			if ( resolved != queryParameter ) {
				binding = parameterListBindingMap.get( resolved );
			}
		}

		if ( binding == null ) {
			throw new IllegalArgumentException( "Could not locate parameter list binding" );
		}

		return binding;
	}

	private QueryParameter resolveParameter(QueryParameter queryParameter) {
		if ( queryParameter.getName() != null ) {
			return parameterMetadata.getQueryParameter( queryParameter.getName() );
		}
		else {
			return parameterMetadata.getQueryParameter( queryParameter.getPosition() );
		}
	}

	/**
	 * @deprecated (since 5.2) expected changes to "collection-valued parameter binding" in 6.0
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	private <T> QueryParameterListBinding<T> transformQueryParameterBindingToQueryParameterListBinding(QueryParameter<T> queryParameter) {
		log.debugf( "Converting QueryParameterBinding to QueryParameterListBinding for given QueryParameter : %s", queryParameter );

		getAndRemoveBinding( queryParameter );

		return makeListBinding( queryParameter );
	}

	private boolean shouldValidateBindingValue() {
		return sessionFactory.getSessionFactoryOptions().isJpaBootstrap() && queryParametersValidationEnabled;
	}

	/**
	 * @deprecated (since 5.2) expected changes to "collection-valued parameter binding" in 6.0
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	private <T> QueryParameterBinding<T> getAndRemoveBinding(QueryParameter<T> parameter) {
		QueryParameterBinding<T> binding = parameterBindingMap.remove( parameter );

		if ( binding == null ) {
			if ( parameter.getName() != null ) {
				parameter = parameterMetadata.getQueryParameter( parameter.getName() );
			}
			else {
				parameter = parameterMetadata.getQueryParameter( parameter.getPosition() );
			}

			if ( parameter == null ) {
				throw new HibernateException( "Unable to resolve QueryParameter" );
			}
		}
		binding = parameterBindingMap.remove( parameter );

		return binding;
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
		final QueryParameter<Object> param = parameterMetadata.getQueryParameter( name );

		if ( param == null ) {
			throw new IllegalArgumentException(
					"Unable to resolve given parameter name [" + name + "] to QueryParameter reference"
			);
		}

		return (QueryParameter<T>) param;
	}

	/**
	 * @deprecated (since 5.2) expected changes to "collection-valued parameter binding" in 6.0
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	public <T> QueryParameterListBinding<T> getQueryParameterListBinding(int name) {
		// find the QueryParameter instance for the given name
		final QueryParameter<T> queryParameter = resolveQueryParameter( name );
		return getQueryParameterListBinding( queryParameter );
	}

	/**
	 * @deprecated (since 5.2) expected changes to "collection-valued parameter binding" in 6.0
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	private <T> QueryParameter<T> resolveQueryParameter(int name) {
		final QueryParameter<Object> param = parameterMetadata.getQueryParameter( name );

		if ( param == null ) {
			throw new IllegalArgumentException(
					"Unable to resolve given parameter name [" + name + "] to QueryParameter reference"
			);
		}

		return (QueryParameter<T>) param;
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

		if ( parameterListBindingMap == null || parameterListBindingMap.isEmpty() ) {
			return queryString;
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

		int maxOrdinalPosition = getMaxOrdinalPosition();

		for ( Map.Entry<QueryParameter, QueryParameterListBinding> entry : parameterListBindingMap.entrySet() ) {
			final QueryParameter sourceParam = entry.getKey();
			final Collection bindValues = entry.getValue().getBindValues();

			int bindValueCount = bindValues.size();
			int bindValueMaxCount = bindValueCount;

			boolean inClauseParameterPaddingEnabled =
					session.getFactory().getSessionFactoryOptions().inClauseParameterPaddingEnabled() &&
					bindValueCount > 2;

			if ( inClauseParameterPaddingEnabled ) {
				int bindValuePaddingCount = MathHelper.ceilingPowerOfTwo( bindValueCount );

				if ( bindValueCount < bindValuePaddingCount && (inExprLimit == 0 || bindValuePaddingCount < inExprLimit) ) {
					bindValueMaxCount = bindValuePaddingCount;
				}
			}

			if ( inExprLimit > 0 && bindValueCount > inExprLimit ) {
				log.tooManyInExpressions( dialect.getClass().getName(), inExprLimit, sourceParam.getName(), bindValueCount );
			}

			final String sourceToken;
			if ( sourceParam instanceof NamedParameterDescriptor ) {
				sourceToken = ":" + NamedParameterDescriptor.class.cast( sourceParam ).getName();
			}
			else {
				sourceToken = "?" + OrdinalParameterDescriptor.class.cast( sourceParam ).getPosition();
			}

			final int loc = StringHelper.indexOfIdentifierWord( queryString, sourceToken );

			if ( loc < 0 ) {
				continue;
			}

			final String beforePlaceholder = queryString.substring( 0, loc );
			final String afterPlaceholder = queryString.substring( loc + sourceToken.length() );

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

			Iterator bindValueIterator = entry.getValue().getBindValues().iterator();
			Object bindValue = null;

			for ( int i = 0; i < bindValueMaxCount; i++ ) {

				if ( i < bindValueCount ) {
					bindValue = bindValueIterator.next();
				}

				if ( i > 0 ) {
					expansionList.append( ", " );
				}

				final QueryParameter syntheticParam;
				if ( sourceParam instanceof NamedParameterDescriptor ) {
					// in the case of a named parameter, for each value in the bound list-of-values we:
					//		1) create a synthetic named parameter
					//		2) expand the queryString to include each synthetic named param in place of the original
					//		3) create a new synthetic binding for just that single value under the synthetic name

					final String syntheticName = NamedParameterDescriptor.class.cast( sourceParam ).getName() + '_' + i;
					expansionList.append( ":" ).append( syntheticName );

					syntheticParam = new NamedParameterDescriptor(
							syntheticName,
							sourceParam.getHibernateType(),
							sourceParam.getSourceLocations()
					);
				}
				else {
					// in the case of an ordinal parameter, for each value in the bound list-of-values we:
					//		1) create a new ordinal parameter at a synthetic position of maxOrdinalPosition + 1
					//		2) expand the queryString to include each new ordinal param in place of the original
					//		3) create a new ordinal binding for just that single value under the synthetic position
					// for the first item, we reuse the original parameter to avoid gaps in the positions
					if ( i == 0 ) {
						syntheticParam = sourceParam;
					}
					else {
						int syntheticPosition = ++maxOrdinalPosition;
						syntheticParam = new OrdinalParameterDescriptor(
								syntheticPosition,
								syntheticPosition - jdbcStyleOrdinalCountBase,
								sourceParam.getHibernateType(),
								sourceParam.getSourceLocations()
						);
					}

					expansionList.append( "?" ).append( syntheticParam.getPosition() );
				}

				final QueryParameterBinding syntheticBinding = makeBinding( entry.getValue().getBindType() );
				syntheticBinding.setBindValue( bindValue );
				parameterBindingMap.put( syntheticParam, syntheticBinding );
			}

			String expansionListAsString = expansionList.toString();

			// HHH-8901
			if ( ! dialect.supportsEmptyInList() && expansionListAsString.isEmpty() ) {
				expansionListAsString = "null";
			}

			queryString = StringHelper.replace(
					beforePlaceholder,
					afterPlaceholder,
					sourceToken,
					expansionListAsString,
					true,
					true
			);
		}

		return queryString;
	}

	private int getMaxOrdinalPosition() {
		int maxOrdinalPosition = 0;
		for ( QueryParameter<?> queryParameter : parameterBindingMap.keySet() ) {
			if ( queryParameter instanceof OrdinalParameterDescriptor ) {
				maxOrdinalPosition = Math.max( maxOrdinalPosition, queryParameter.getPosition() );
			}
		}
		for ( QueryParameter<?> queryParameter : parameterListBindingMap.keySet() ) {
			if ( queryParameter instanceof OrdinalParameterDescriptor ) {
				maxOrdinalPosition = Math.max( maxOrdinalPosition, queryParameter.getPosition() );
			}
		}
		return maxOrdinalPosition;
	}
}
