/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Contract for resolving an entity-name from a given entity instance.
 *
 * @author Steve Ebersole
 */
public interface EntityNameResolver {
	/**
	 * Given an entity instance, determine its entity-name.
	 *
	 * @param entity The entity instance.
	 *
	 * @return The corresponding entity-name, or null if this impl does not know how to perform resolution
	 *         for the given entity instance.
	 */
	public String resolveEntityName(Object entity);
}
