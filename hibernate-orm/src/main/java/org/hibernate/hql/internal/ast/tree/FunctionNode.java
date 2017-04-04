/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.type.Type;

/**
 * Identifies a node which models a SQL function.
 *
 * @author Steve Ebersole
 */
public interface FunctionNode {
	public SQLFunction getSQLFunction();
	public Type getFirstArgumentType();
}
