/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.select;

import org.hibernate.sql.ast.expression.Expression;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.convert.results.spi.ReturnResolutionContext;

/**
 * Represents something that is selectable at the Object level.  This is
 * distinctly different from {@link SqlSelectable} which represents something
 * selectable at the SQL/JDBC level.
 *
 * @author Steve Ebersole
 */
public interface Selectable {
	Expression getSelectedExpression();

	Return toQueryReturn(ReturnResolutionContext returnResolutionContext, String resultVariable);
}
