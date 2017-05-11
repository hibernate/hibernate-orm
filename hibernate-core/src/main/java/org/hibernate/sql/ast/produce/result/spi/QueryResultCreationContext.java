/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.spi;

import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.tree.spi.select.SqlSelectable;
import org.hibernate.sql.ast.tree.spi.select.SqlSelection;

/**
 * todo (6.0) : superseded by SqlSelectionResolver
 *
 * @author Steve Ebersole
 */
public interface QueryResultCreationContext {
	/**
	 * todo (6.0) : is this really needed?  can't this just be passed in to the specific QueryResult impl ctors?
	 */
	NavigablePath currentNavigablePath();

	// todo (6.0) : isn't this really a SqlSelectionResolutionContext?

	/**
	 * Resolve the SqlSelectable to an actual SqlSelection.  This is the process responsible for
	 * "uniqueing" the SqlSelection per SqlSelectable.
	 */
	SqlSelection resolveSqlSelection(SqlSelectable sqlSelectable);
}
