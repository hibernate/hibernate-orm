/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.tools.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.persistence.criteria.JoinType;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.function.OrderByFragmentFunction;
import org.hibernate.envers.internal.entities.RevisionTypeType;
import org.hibernate.envers.internal.entities.mapper.id.QueryParameterData;
import org.hibernate.envers.internal.tools.MutableInteger;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.envers.query.criteria.AuditFunction;
import org.hibernate.envers.query.criteria.AuditId;
import org.hibernate.envers.query.criteria.AuditProperty;
import org.hibernate.envers.query.criteria.internal.CriteriaTools;
import org.hibernate.envers.query.order.NullPrecedence;
import org.hibernate.envers.tools.Pair;
import org.hibernate.internal.util.QuotingHelper;
import org.hibernate.query.Query;
import org.hibernate.type.BasicType;

/**
 * A class for incrementally building a HQL query.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class QueryBuilder {
	private final String entityName;
	private final String alias;

	/**
	 * For use by alias generator (in case an alias is not provided by the user).
	 */
	private final MutableInteger aliasCounter;
	/**
	 * For use by parameter generator, in {@link Parameters}. This counter must be
	 * the same in all parameters and sub-queries of this query.
	 */
	private final MutableInteger paramCounter;
	/**
	 * "where" parameters for this query. Each parameter element of the list for one alias from the "from" part.
	 */
	private final List<Parameters> parameters = new ArrayList<>();

	/**
	 * A list of triples (from entity name, alias name, whether to select the entity).
	 */
	private final List<JoinParameter> froms;
	/**
	 * A list of order by clauses.
	 */
	private final List<OrderByClause> orders;
	/**
	 * A list of complete projection definitions: either a sole property name, or a function(property name).
	 */
	private final List<String> projections;
	/**
	 * Values of parameters used in projections.
	 */
	private final Map<String, Object> projectionQueryParamValues;

	private final List<Pair<String, String>> orderFragments;

	private final SessionFactoryImplementor sessionFactory;

	private final BasicType<?> revisionType;

	/**
	 * @param entityName Main entity which should be selected.
	 * @param alias Alias of the entity
	 * @param sessionFactory Session factory
	 */
	public QueryBuilder(String entityName, String alias, SessionFactoryImplementor sessionFactory) {
		this( entityName, alias, new MutableInteger(), new MutableInteger(), sessionFactory );
	}

	private QueryBuilder(
			String entityName,
			String alias,
			MutableInteger aliasCounter,
			MutableInteger paramCounter,
			SessionFactoryImplementor sessionFactory) {
		this.entityName = entityName;
		this.alias = alias;
		this.aliasCounter = aliasCounter;
		this.paramCounter = paramCounter;
		this.sessionFactory = sessionFactory;

		this.revisionType = sessionFactory.getTypeConfiguration()
				.getBasicTypeRegistry()
				.getRegisteredType( RevisionTypeType.class );

		final Parameters rootParameters = new Parameters( alias, "and", paramCounter );
		parameters.add( rootParameters );

		froms = new ArrayList<>();
		orders = new ArrayList<>();
		projections = new ArrayList<>();
		projectionQueryParamValues = new HashMap<>();
		orderFragments = new ArrayList<>();

		addFrom( entityName, alias, true );
	}

	// Only for deep copy purpose.
	private QueryBuilder(QueryBuilder other) {
		this.entityName = other.entityName;
		this.alias = other.alias;
		this.sessionFactory = other.sessionFactory;
		this.revisionType = other.revisionType;
		this.aliasCounter = other.aliasCounter.deepCopy();
		this.paramCounter = other.paramCounter.deepCopy();
		for (final Parameters params : other.parameters) {
			this.parameters.add( params.deepCopy() );
		}

		froms = new ArrayList<>( other.froms );
		orders = new ArrayList<>( other.orders );
		projections = new ArrayList<>( other.projections );
		projectionQueryParamValues = new HashMap<>( other.projectionQueryParamValues );
		orderFragments = new ArrayList<>( other.orderFragments );
	}

	public QueryBuilder deepCopy() {
		return new QueryBuilder( this );
	}

	/**
	 * @return the main alias of this query builder
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * Add an entity from which to select.
	 *
	 * @param entityName Name of the entity from which to select.
	 * @param alias Alias of the entity. Should be different than all other aliases.
	 * @param select whether the entity should be selected
	 */
	public void addFrom(String entityName, String alias, boolean select) {
		CrossJoinParameter joinParameter = new CrossJoinParameter( entityName, alias, select );
		froms.add( joinParameter );
	}

	public Parameters addJoin(JoinType joinType, String entityName, String alias, boolean select) {
		Parameters joinConditionParameters = new Parameters( alias, Parameters.AND, paramCounter );
		InnerOuterJoinParameter joinParameter = new InnerOuterJoinParameter(
				joinType,
				entityName,
				alias,
				select,
				joinConditionParameters
		);
		froms.add( joinParameter );
		return joinConditionParameters;
	}

	public String generateAlias() {
		return "_e" + aliasCounter.getAndIncrease();
	}

	/**
	 * @param entityName Entity name, which will be the main entity for the sub-query.
	 * @param alias Alias of the entity, which can later be used in parameters.
	 *
	 * @return A sub-query builder for the given entity, with the given alias. The sub-query can
	 *         be later used as a value of a parameter.
	 */
	public QueryBuilder newSubQueryBuilder(String entityName, String alias) {
		return new QueryBuilder( entityName, alias, aliasCounter, paramCounter, sessionFactory );
	}

	public Parameters getRootParameters() {
		return parameters.get( 0 );
	}

	public Parameters addParameters(final String alias) {
		final Parameters result = new Parameters( alias, Parameters.AND, paramCounter);
		parameters.add( result );
		return result;
	}

	public void addOrder(String alias, String propertyName, boolean ascending, NullPrecedence nullPrecedence) {
		orders.add( new OrderByClause( alias, propertyName, ascending, nullPrecedence ) );
	}

	public void addOrderFragment(String alias, String orderByCollectionRole) {
		orderFragments.add( Pair.make( alias, orderByCollectionRole ) );
	}

	public void addProjection(String function, String alias, String propertyName, boolean distinct) {
		final String effectivePropertyName = propertyName == null ? "" : ".".concat( propertyName );
		if ( function == null ) {
			projections.add( (distinct ? "distinct " : "") + alias + effectivePropertyName );
		}
		else {
			projections.add(
					function + "(" + (distinct ? "distinct " : "") + alias +
					effectivePropertyName + ")"
			);
		}
	}

	public void addProjection(
			Configuration configuration,
			Map<String, String> aliasToEntityNameMap,
			Map<String, String> aliasToComponentPropertyNameMap,
			AuditFunction function) {
		final StringBuilder expression = new StringBuilder();
		appendFunctionArgument(
				configuration,
				aliasToEntityNameMap,
				aliasToComponentPropertyNameMap,
				paramCounter,
				projectionQueryParamValues,
				alias,
				expression,
				function
		);
		projections.add( expression.toString() );
	}

	protected static void appendFunctionArgument(
			Configuration configuration,
			Map<String, String> aliasToEntityNameMap,
			Map<String, String> aliasToComponentPropertyNameMap,
			MutableInteger paramCounter,
			Map<String, Object> queryParamValues,
			String alias,
			StringBuilder expression,
			Object argument) {
		if ( argument instanceof AuditFunction ) {
			AuditFunction function = (AuditFunction) argument;
			expression.append( function.getFunction() ).append( '(' );
			boolean first = true;
			for ( final Object innerArg : function.getArguments() ) {
				if ( !first ) {
					expression.append( ',' );
				}
				appendFunctionArgument(
						configuration,
						aliasToEntityNameMap,
						aliasToComponentPropertyNameMap,
						paramCounter,
						queryParamValues,
						alias,
						expression,
						innerArg
				);
				first = false;
			}
			expression.append( ')' );
		}
		else if ( argument instanceof AuditId ) {
			AuditId<?> id = (AuditId<?>) argument;
			String prefix = configuration.getOriginalIdPropertyName();
			String idAlias = id.getAlias( alias );
			String entityName = aliasToEntityNameMap.get( idAlias );
			/*
			 * Resolve the name of the id property by reusing the IdMapper.mapToQueryParametersFromId() method. Null is
			 * passed as value because only the name of the property is of interest. TODO: is there a better way to
			 * obtain the name of the id property?
			 */
			EnversService enversService = configuration.getEnversService();
			List<QueryParameterData> parameters = enversService.getEntitiesConfigurations().get( entityName )
					.getIdMapper()
					.mapToQueryParametersFromId( null );
			if ( parameters.size() != 1 ) {
				throw new HibernateException( "Cannot add id property as function argument when id property is not a single column property" );
			}
			String propertyName = parameters.get( 0 ).getProperty( prefix );
			if ( idAlias != null ) {
				expression.append( idAlias ).append( '.' );
			}
			expression.append( propertyName );
		}
		else if ( argument instanceof AuditProperty ) {
			AuditProperty<?> property = (AuditProperty<?>) argument;
			String propertyAlias = property.getAlias( alias );
			if ( propertyAlias != null ) {
				expression.append( propertyAlias ).append( '.' );
			}
			String propertyPrefix = CriteriaTools.determineComponentPropertyPrefix(
					configuration.getEnversService(),
					aliasToEntityNameMap,
					aliasToComponentPropertyNameMap,
					propertyAlias
			);
			String propertyName = property.getPropertyNameGetter().get( configuration );
			expression.append( propertyPrefix.concat( propertyName ) );
		}
		else {
			String queryParam = "_p" + paramCounter.getAndIncrease();
			queryParamValues.put( queryParam, argument );
			expression.append( ':' ).append( queryParam );
		}
	}

	/**
	 * Builds the given query, appending results to the given string buffer, and adding all query parameter values
	 * that are used to the map provided.
	 *
	 * @param sb String builder to which the query will be appended.
	 * @param queryParamValues Map to which name and values of parameters used in the query should be added.
	 */
	public void build(StringBuilder sb, Map<String, Object> queryParamValues) {
		sb.append( "select " );
		if ( projections.size() > 0 ) {
			// all projections separated with commas
			StringTools.append( sb, projections.iterator(), ", " );
		}
		else {
			// all aliases separated with commas
			StringTools.append( sb, getSelectAliasList().iterator(), ", " );
		}
		queryParamValues.putAll( projectionQueryParamValues );
		sb.append( " from " );
		// all from entities with aliases
		boolean first = true;
		for (final JoinParameter joinParameter : froms) {
			joinParameter.appendJoin( first, sb, queryParamValues );
			first = false;
		}
		// where part - rootParameters
		first = true;
		for (final Parameters params : parameters) {
			if (!params.isEmpty()) {
				if (first) {
					sb.append( " where " );
					first = false;
				}
				else {
					sb.append( " and " );
				}
				params.build( sb, queryParamValues );
			}
		}
		// orders
		if ( !orders.isEmpty() ) {
			sb.append( " order by " );
			StringTools.append( sb, getOrderList().iterator(), ", " );
		}
		else if ( !orderFragments.isEmpty() ) {
			sb.append( " order by " );

			final Iterator<Pair<String, String>> fragmentIterator = orderFragments.iterator();
			while( fragmentIterator.hasNext() ) {
				final Pair<String, String> fragment = fragmentIterator.next();
				sb.append( OrderByFragmentFunction.FUNCTION_NAME ).append( '(' );
				// The first argument is the sqm alias of the from node
				QuotingHelper.appendSingleQuoteEscapedString( sb, fragment.getFirst() );
				sb.append( ", " );
				// The second argument is the collection role that contains the order by fragment
				QuotingHelper.appendSingleQuoteEscapedString( sb, fragment.getSecond() );
				sb.append( ')' );
				if ( fragmentIterator.hasNext() ) {
					sb.append( ", " );
				}
			}
		}
	}

	private List<String> getSelectAliasList() {
		final List<String> aliasList = new ArrayList<>();
		for ( JoinParameter from : froms ) {
			if ( from.isSelect() ) {
				aliasList.add( from.getAlias() );
			}
		}

		return aliasList;
	}

	public String getRootAlias() {
		return alias;
	}

	private List<String> getOrderList() {
		final List<String> orderList = new ArrayList<>();
		for ( OrderByClause orderByClause : orders ) {
			orderList.add( orderByClause.renderToHql() );
		}
		return orderList;
	}

	public Query toQuery(Session session) {
		final StringBuilder querySb = new StringBuilder();
		final Map<String, Object> queryParamValues = new HashMap<>();

		build( querySb, queryParamValues );

		final Query query = session.createQuery( querySb.toString() );
		for ( Map.Entry<String, Object> paramValue : queryParamValues.entrySet() ) {
			if ( paramValue.getValue() instanceof RevisionType ) {
				// this is needed when the ClassicQueryTranslatorFactory is used
				query.setParameter( paramValue.getKey(), paramValue.getValue(), revisionType );
			}
			else {
				query.setParameter( paramValue.getKey(), paramValue.getValue() );
			}
		}
		return query;
	}

	private abstract static class JoinParameter {

		private final String alias;
		private final boolean select;

		protected JoinParameter(String alias, boolean select) {
			this.alias = alias;
			this.select = select;
		}

		public String getAlias() {
			return alias;
		}

		public boolean isSelect() {
			return select;
		}

		public abstract void appendJoin(boolean firstFromElement, StringBuilder builder, Map<String, Object> queryParamValues);

	}

	private static class CrossJoinParameter extends JoinParameter {

		private final String entityName;

		public CrossJoinParameter(String entityName, String alias, boolean select) {
			super( alias, select );
			this.entityName = entityName;
		}

		@Override
		public void appendJoin(boolean firstFromElement, StringBuilder builder, Map<String, Object> queryParamValues) {
			if ( !firstFromElement ) {
				builder.append( ", " );
			}
			builder.append( entityName ).append( ' ' ).append( getAlias() );
		}

	}

	private static class InnerOuterJoinParameter extends JoinParameter {

		private final JoinType joinType;
		private final String entityName;
		private final Parameters joinConditionParameters;

		public InnerOuterJoinParameter(
				JoinType joinType,
				String entityName,
				String alias,
				boolean select,
				Parameters joinConditionParameters) {
			super(alias, select);
			this.joinType = joinType;
			this.entityName = entityName;
			this.joinConditionParameters = joinConditionParameters;
		}

		@Override
		public void appendJoin(boolean firstFromElement, StringBuilder builder, Map<String, Object> queryParamValues) {
			if (firstFromElement) {
				throw new IllegalArgumentException( "An inner/outer join cannot come as first 'from element'" );
			}
			builder.append( ' ' ).append( joinType.name()
					.toLowerCase( Locale.US ) ).append( " join " )
					.append( entityName ).append( ' ' )
					.append( getAlias() ).append( " on " );
			joinConditionParameters.build( builder, queryParamValues );
		}

	}

	private static class OrderByClause {
		private String alias;
		private String propertyName;
		private boolean ascending;
		private NullPrecedence nullPrecedence;

		public OrderByClause(String alias, String propertyName, boolean ascending, NullPrecedence nullPrecedence) {
			this.alias = alias;
			this.propertyName = propertyName;
			this.ascending = ascending;
			this.nullPrecedence = nullPrecedence;
		}

		public String renderToHql() {
			StringBuilder hql = new StringBuilder();
			hql.append( alias ).append( "." ).append( propertyName ).append( " " );
			hql.append( ascending ? "asc" : "desc" );
			if ( nullPrecedence != null ) {
				if ( NullPrecedence.FIRST.equals( nullPrecedence ) ) {
					hql.append( " nulls first" );
				}
				else {
					hql.append( " nulls last" );
				}
			}
			return hql.toString();
		}
	}
}
