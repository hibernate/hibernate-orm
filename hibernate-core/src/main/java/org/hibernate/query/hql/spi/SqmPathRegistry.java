/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.spi;

import java.util.function.Function;

import org.hibernate.Incubating;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.select.SqmAliasedNode;

/**
 * Registry for SqmPath references providing the ability to access them
 * in multiple ways - by alias, by NavigablePath, etc
 *
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
@Incubating
public interface SqmPathRegistry {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqmPath

	/**
	 * Register an SqmPath
	 */
	void register(SqmPath<?> sqmPath);

	/**
	 * Register an SqmFrom by alias only.
	 * Effectively, this makes the from node only resolvable via the alias,
	 * which means that the from node is ignored in {@link #findFromExposing(String)}.
	 */
	void registerByAliasOnly(SqmFrom<?, ?> sqmFrom);

	/**
	 * Used with {@linkplain JpaCompliance#isJpaQueryComplianceEnabled() JPA compliance}
	 * to treat secondary query roots as cross-joins.  Here we will replace the {@code sqmRoot}
	 * with the {@code sqmJoin}
	 *
	 * @apiNote Care should be taken when calling this method to ensure that nothing
	 * has used the previous registration between its registration and this call.
	 * Generally, most callers want {@link #register(SqmPath)} instead.
	 */
	<E> void replace(SqmEntityJoin<E> sqmJoin, SqmRoot<E> sqmRoot);

	/**
	 * Find a SqmFrom by its identification variable (alias).
	 * If the SqmFrom is found in a parent context, the correlation for the path will be returned.
	 *
	 * @return matching SqmFrom or {@code null}
	 */
	<X extends SqmFrom<?, ?>> X findFromByAlias(String identificationVariable, boolean searchParent);

	/**
	 * Find a SqmFrom by its NavigablePath.  Will search any parent contexts as well
	 *
	 * @return matching SqmFrom or {@code null}
	 */
	<X extends SqmFrom<?, ?>> X findFromByPath(NavigablePath navigablePath);

	/**
	 * Find a SqmFrom which exposes a Navigable by the given name.  Will search any
	 * parent contexts as well
	 *
	 * @return matching SqmFrom or {@code null}
	 */
	<X extends SqmFrom<?, ?>> X findFromExposing(String navigableName);

	/**
	 * Similar to {@link #findFromByPath}, but accepting a producer to be used
	 * to create and register a SqmFrom if none yet registered.
	 *
	 * @return The existing or just-created SqmFrom
	 */
	<X extends SqmFrom<?, ?>> X resolveFrom(NavigablePath path, Function<NavigablePath, SqmFrom<?, ?>> creator);

	/**
	 * Similar to {@link #resolveFrom}, but accepting a SqmPath to be used
	 * to create and register a SqmFrom if none yet registered.
	 *
	 * @return The existing or just-created SqmFrom
	 */
	<X extends SqmFrom<?, ?>> X resolveFrom(SqmPath<?> path);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqmSelection

	/**
	 * Register a node aliased within the select-clause
	 */
	void register(SqmAliasedNode<?> aliasedNode);

	/**
	 * Find a node (if one) by the explicit alias assigned to it
	 * within the select-clause
	 *
	 * @return The matching node, or null
	 */
	SqmAliasedNode<?> findAliasedNodeByAlias(String alias);

	/**
	 * Find the position of a node with the given alias, relative to the
	 * underlying SQL select-list.
	 *
	 * @return The position, or null
	 */
	Integer findAliasedNodePosition(String alias);

	/**
	 * Find an SqmSelection by its position in the SqmSelectClause
	 *
	 * @return The matching node, or null
	 */
	SqmAliasedNode<?> findAliasedNodeByPosition(int position);
}
