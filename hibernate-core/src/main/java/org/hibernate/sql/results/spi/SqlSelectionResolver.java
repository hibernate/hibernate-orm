/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

/**
 * Resolution of a SqlSelection reference for a given SqlSelectable.
 *
 * Allows "unique-ing" of SqlSelectable references in a query to a single SqlSelection
 * reference - effectively a caching of SqlSelection instances keyed by the SqlSelectable
 * that it refers to.
 *
 * @author Steve Ebersole
 */
public interface SqlSelectionResolver {
	/**
	 * Resolve a SqlSelection reference for the given SqlSelectable.
	 *
	 * Effectively a caching of SqlSelection instances keyed
	 * by the SqlSelectable that it refers to.
	 *
	 * Generally implementations can simply use the passed `sqlSelectable`
	 * as the key for such unique-ing.  It represents a specific usable of
	 * something (e.g. a {@link PluralAttributeMappingNode}) as opposed to the
	 * general thing (e.g. {@link org.hibernate.metamodel.model.relational.spi.Column}).
	 */
	SqlSelection resolveSqlSelection(SqlSelectable sqlSelectable);
}
