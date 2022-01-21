/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.spi;

import java.util.function.Function;

import org.hibernate.Incubating;
import org.hibernate.query.spi.NavigablePath;
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
	 * Find a SqmFrom by its identification variable (alias).  Will search any
	 * parent contexts as well
	 *
	 * @return matching SqmFrom or {@code null}
	 */
	<X extends SqmFrom<?, ?>> X findFromByAlias(String identificationVariable);

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
