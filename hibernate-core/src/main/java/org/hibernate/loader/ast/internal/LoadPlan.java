/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;

/**
 * Common contract for SQL AST based loading
 *
 * @author Steve Ebersole
 */
public interface LoadPlan {
	/**
	 * The thing being loaded
	 */
	Loadable getLoadable();

	/**
	 * The part of the thing being loaded used to restrict which loadables get loaded
	 */
	ModelPart getRestrictivePart();

	/**
	 * The JdbcSelect for the load
	 */
	JdbcOperationQuerySelect getJdbcSelect();
}
