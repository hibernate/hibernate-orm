/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.annotations.Remove;

/**
 * Represents a grouping of SqlSelection references, generally related to a
 * single Navigable
 *
 * @author Steve Ebersole
 */
@Remove
public interface SqlSelectionGroup extends SqlSelectionGroupNode {
//	/**
//	 * Get the SqlSelections associated with the given StateArrayContributor
//	 *
//	 * The return type being a List is very important here. {@link SqlSelection#getValuesArrayPosition()}
//	 * is in relation to the JDBC values array as a whole while the order here is specific to the
//	 * StateArrayContributor.  Ultimately this List is used to build the array (for multiple selections) to
//	 * {@link StateArrayContributor#hydrate} and the indexes between them are expected to match
//	 */
//	List<SqlSelection> getSqlSelections(StateArrayContributor contributor);
//
//	void visitSelections(BiConsumer<StateArrayContributor, List<SqlSelection>> action);
}
