/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.ordering;

import java.util.List;

import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.SortSpecification;

/**
 * Represents the translation result
 *
 * @author Steve Ebersole
 */
public interface OrderByFragment {
	// Something like:

	List<SortSpecification> toSqlAst(TableGroup tableGroup, SqlAstCreationState creationState);

	/**
	 * Inject table aliases into the translated fragment to properly qualify column references, using
	 * the given 'aliasResolver' to determine the the proper table alias to use for each column reference.
	 *
	 * @param aliasResolver The strategy to resolver the proper table alias to use per column
	 *
	 * @return The fully translated and replaced fragment.
	 */
	String injectAliases(AliasResolver aliasResolver);
}
