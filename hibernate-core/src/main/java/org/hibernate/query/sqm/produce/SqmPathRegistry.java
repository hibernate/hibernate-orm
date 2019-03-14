/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce;

import java.util.function.Function;

import org.hibernate.Incubating;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * Registry for SqmPath references providing the ability to access them
 * in multiple ways - by alias, by NavigablePath, etc
 *
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
@Incubating
public interface SqmPathRegistry {
	void register(SqmPath sqmPath);
	void register(NavigablePath alternatePath, SqmPath sqmPath);

	/**
	 * Find a SqmFrom by its identification variable (alias).  Will search any
	 * parent contexts as well
	 *
	 * @return matching SqmFrom or {@code null}
	 */
	SqmFrom findFromByAlias(String identificationVariable);

	/**
	 * Find a SqmFrom by its NavigablePath.  Will search any parent contexts as well
	 *
	 * @return matching SqmFrom or {@code null}
	 */
	SqmFrom findFromByPath(NavigablePath navigablePath);

	/**
	 * Find a SqmFrom which exposes a Navigable by the given name.  Will search any
	 * parent contexts as well
	 *
	 * @return matching SqmFrom or {@code null}
	 */
	SqmFrom findFromExposing(String navigableName);

	/**
	 * Find an SqmPath by its NavigablePath.  Will return a SqmFrom if the NavigablePath
	 * has (yet) been resolved to a SqmFrom.  Otherwise, it will be a non-SqmFrom SqmPath
	 *
	 * @return matching SqmPath or {@code null}
	 */
	SqmPath findPath(NavigablePath path);

	/**
	 * Similar to {@link #findPath}, but accepting a producer to be used
	 * to create and register a SqmPath if none yet registered.
	 *
	 * @return The existing or just-created SqmPath
	 */
	SqmPath resolvePath(NavigablePath path, Function<NavigablePath,SqmPath> creator);
}
