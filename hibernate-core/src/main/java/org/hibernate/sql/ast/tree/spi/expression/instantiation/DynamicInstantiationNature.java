/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.instantiation;

import java.util.List;
import java.util.Map;

/**
 * Represents the type of instantiation to be performed.
 *
 * @author Steve Ebersole
 */
public enum DynamicInstantiationNature {
	/**
	 * The target names a Class to be instantiated.  This is the only form
	 * of dynamic instantiation that is JPA-compliant.
	 */
	CLASS,
	/**
	 * The target identified a {@link Map} instantiation.  The
	 * result for each "row" will be a Map whose key is the alias (or name
	 * of the selected attribute is no alias) and whose value is the
	 * corresponding value read from the JDBC results.  Similar to JPA's
	 * named-Tuple support.
	 */
	MAP,
	/**
	 * The target identified a {@link List} instantiation.  The
	 * result for each "row" will be a List rather than an array.  Similar
	 * to JPA's positional-Tuple support.
	 */
	LIST
}
