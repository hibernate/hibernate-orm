/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.internal;

import org.hibernate.loader.spi.Loadable;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.sql.ast.tree.select.SelectStatement;

/**
 * Common contract for SQL AST based loading
 *
 * @author Steve Ebersole
 */
public interface LoadPlan {
	Loadable getLoadable();
	ModelPart getRestrictivePart();
	SelectStatement getSqlAst();
}
