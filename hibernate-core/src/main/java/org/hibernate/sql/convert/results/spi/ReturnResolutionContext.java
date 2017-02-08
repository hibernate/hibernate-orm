/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.convert.results.spi;

import org.hibernate.sql.ast.expression.domain.NavigablePath;
import org.hibernate.sql.ast.select.SqlSelectable;
import org.hibernate.sql.ast.select.SqlSelection;

/**
 * @author Steve Ebersole
 */
public interface ReturnResolutionContext {
	boolean isShallowQuery();

	SqlSelection resolveSqlSelection(SqlSelectable sqlSelectable);

	NavigablePath currentNavigablePath();
}
