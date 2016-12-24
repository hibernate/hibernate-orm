/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.expression;

import org.hibernate.sql.ast.select.Selectable;
import org.hibernate.sql.ast.select.SqlSelectable;

/**
 * Marker interface to more readily identify "aggregate functions".
 *
 * @author Steve Ebersole
 */
public interface AggregateFunction extends Expression, Selectable, SqlSelectable {
	Expression getArgument();
	boolean isDistinct();
}
