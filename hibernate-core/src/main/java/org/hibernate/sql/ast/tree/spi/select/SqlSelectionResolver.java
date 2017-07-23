/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.select;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.sql.exec.results.spi.SqlSelectionGroup;

/**
 * Resolves SqlSelectable references to SqlSelection.  This is the process responsible for
 * "uniqueing" the SqlSelection per SqlSelectable.
 *
 * @author Steve Ebersole
 */
public interface SqlSelectionResolver {
	SqlSelectionGroup resolveSqlSelectionGroup(Navigable navigable);
	SqlSelection resolveSqlSelection(SqlSelectable sqlSelectable);
}
