/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import org.hibernate.sql.JoinType;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.hibernate.transform.DistinctRootEntityResultTransformer;
import org.hibernate.transform.PassThroughResultTransformer;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.RootEntityResultTransformer;

/**
 * Commonality between different types of Criteria.
 *
 * @author Gavin King
 */
public interface CriteriaSpecification {

	/**
	 * The alias that refers to the "root" entity of the criteria query.
	 */
	public static final String ROOT_ALIAS = "this";

	/**
	 * Each row of results is a <tt>Map</tt> from alias to entity instance
	 */
	public static final ResultTransformer ALIAS_TO_ENTITY_MAP = AliasToEntityMapResultTransformer.INSTANCE;

	/**
	 * Each row of results is an instance of the root entity
	 */
	public static final ResultTransformer ROOT_ENTITY = RootEntityResultTransformer.INSTANCE;

	/**
	 * Each row of results is a distinct instance of the root entity
	 */
	public static final ResultTransformer DISTINCT_ROOT_ENTITY = DistinctRootEntityResultTransformer.INSTANCE;

	/**
	 * This result transformer is selected implicitly by calling <tt>setProjection()</tt>
	 */
	public static final ResultTransformer PROJECTION = PassThroughResultTransformer.INSTANCE;

	/**
	 * Specifies joining to an entity based on an inner join.
	 *
	 * @deprecated use {@link org.hibernate.sql.JoinType#INNER_JOIN}
	 */
	@Deprecated
	public static final int INNER_JOIN = JoinType.INNER_JOIN.getJoinTypeValue();

	/**
	 * Specifies joining to an entity based on a full join.
	 *
	 * @deprecated use {@link org.hibernate.sql.JoinType#FULL_JOIN}
	 */
	@Deprecated
	public static final int FULL_JOIN = JoinType.FULL_JOIN.getJoinTypeValue();

	/**
	 * Specifies joining to an entity based on a left outer join.
	 *
	 * @deprecated use {@link org.hibernate.sql.JoinType#LEFT_OUTER_JOIN}
	 */
	@Deprecated
	public static final int LEFT_JOIN = JoinType.LEFT_OUTER_JOIN.getJoinTypeValue();


}
