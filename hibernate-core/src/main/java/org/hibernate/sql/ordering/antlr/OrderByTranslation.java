/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ordering.antlr;

/**
 * Represents the result of an order-by translation by {@link @OrderByTranslator}
 *
 * @author Steve Ebersole
 */
public interface OrderByTranslation {
	/**
	 * Inject table aliases into the translated fragment to properly qualify column references, using
	 * the given 'aliasResolver' to determine the the proper table alias to use for each column reference.
	 *
	 * @param aliasResolver The strategy to resolver the proper table alias to use per column
	 *
	 * @return The fully translated and replaced fragment.
	 */
	public String injectAliases(OrderByAliasResolver aliasResolver);
}
