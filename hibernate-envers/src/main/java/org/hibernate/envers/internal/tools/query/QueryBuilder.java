/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.internal.tools.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.envers.internal.tools.MutableInteger;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.envers.tools.Pair;

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
	 * Main "where" parameters for this query.
	 */
	private final Parameters rootParameters;

	/**
	 * A list of pairs (from entity name, alias name).
	 */
	private final List<Pair<String, String>> froms;
	/**
	 * A list of pairs (property name, order ascending?).
	 */
	private final List<Pair<String, Boolean>> orders;
	/**
	 * A list of complete projection definitions: either a sole property name, or a function(property name).
	 */
	private final List<String> projections;

	/**
	 * @param entityName Main entity which should be selected.
	 * @param alias Alias of the entity
	 */
	public QueryBuilder(String entityName, String alias) {
		this( entityName, alias, new MutableInteger(), new MutableInteger() );
	}

	private QueryBuilder(String entityName, String alias, MutableInteger aliasCounter, MutableInteger paramCounter) {
		this.entityName = entityName;
		this.alias = alias;
		this.aliasCounter = aliasCounter;
		this.paramCounter = paramCounter;

		rootParameters = new Parameters( alias, "and", paramCounter );

		froms = new ArrayList<Pair<String, String>>();
		orders = new ArrayList<Pair<String, Boolean>>();
		projections = new ArrayList<String>();

		addFrom( entityName, alias );
	}

	// Only for deep copy purpose.
	private QueryBuilder(QueryBuilder other) {
		this.entityName = other.entityName;
		this.alias = other.alias;
		this.aliasCounter = other.aliasCounter.deepCopy();
		this.paramCounter = other.paramCounter.deepCopy();
		this.rootParameters = other.rootParameters.deepCopy();

		froms = new ArrayList<Pair<String, String>>( other.froms );
		orders = new ArrayList<Pair<String, Boolean>>( other.orders );
		projections = new ArrayList<String>( other.projections );
	}

	public QueryBuilder deepCopy() {
		return new QueryBuilder( this );
	}

	/**
	 * Add an entity from which to select.
	 *
	 * @param entityName Name of the entity from which to select.
	 * @param alias Alias of the entity. Should be different than all other aliases.
	 */
	public void addFrom(String entityName, String alias) {
		froms.add( Pair.make( entityName, alias ) );
	}

	private String generateAlias() {
		return "_e" + aliasCounter.getAndIncrease();
	}

	/**
	 * @return A sub-query builder for the same entity (with an auto-generated alias). The sub-query can
	 *         be later used as a value of a parameter.
	 */
	public QueryBuilder newSubQueryBuilder() {
		return newSubQueryBuilder( entityName, generateAlias() );
	}

	/**
	 * @param entityName Entity name, which will be the main entity for the sub-query.
	 * @param alias Alias of the entity, which can later be used in parameters.
	 *
	 * @return A sub-query builder for the given entity, with the given alias. The sub-query can
	 *         be later used as a value of a parameter.
	 */
	public QueryBuilder newSubQueryBuilder(String entityName, String alias) {
		return new QueryBuilder( entityName, alias, aliasCounter, paramCounter );
	}

	public Parameters getRootParameters() {
		return rootParameters;
	}

	public void addOrder(String propertyName, boolean ascending) {
		orders.add( Pair.make( propertyName, ascending ) );
	}

	public void addProjection(String function, String propertyName, boolean distinct) {
		addProjection( function, propertyName, distinct, true );
	}

	public void addProjection(String function, String propertyName, boolean distinct, boolean addAlias) {
		if ( function == null ) {
			projections.add( (distinct ? "distinct " : "") + (addAlias ? alias + "." : "") + propertyName );
		}
		else {
			projections.add(
					function + "(" + (distinct ? "distinct " : "") + (addAlias ?
							alias + "." :
							"") + propertyName + ")"
			);
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
			StringTools.append( sb, getAliasList().iterator(), ", " );
		}
		sb.append( " from " );
		// all from entities with aliases, separated with commas
		StringTools.append( sb, getFromList().iterator(), ", " );
		// where part - rootParameters
		if ( !rootParameters.isEmpty() ) {
			sb.append( " where " );
			rootParameters.build( sb, queryParamValues );
		}
		// orders
		if ( orders.size() > 0 ) {
			sb.append( " order by " );
			StringTools.append( sb, getOrderList().iterator(), ", " );
		}
	}

	private List<String> getAliasList() {
		final List<String> aliasList = new ArrayList<String>();
		for ( Pair<String, String> from : froms ) {
			aliasList.add( from.getSecond() );
		}

		return aliasList;
	}

	public String getRootAlias() {
		return alias;
	}

	private List<String> getFromList() {
		final List<String> fromList = new ArrayList<String>();
		for ( Pair<String, String> from : froms ) {
			fromList.add( from.getFirst() + " " + from.getSecond() );
		}

		return fromList;
	}

	private List<String> getOrderList() {
		final List<String> orderList = new ArrayList<String>();
		for ( Pair<String, Boolean> order : orders ) {
			orderList.add( alias + "." + order.getFirst() + " " + (order.getSecond() ? "asc" : "desc") );
		}

		return orderList;
	}

	public Query toQuery(Session session) {
		final StringBuilder querySb = new StringBuilder();
		final Map<String, Object> queryParamValues = new HashMap<String, Object>();

		build( querySb, queryParamValues );

		final Query query = session.createQuery( querySb.toString() );
		for ( Map.Entry<String, Object> paramValue : queryParamValues.entrySet() ) {
			query.setParameter( paramValue.getKey(), paramValue.getValue() );
		}

		return query;
	}
}
