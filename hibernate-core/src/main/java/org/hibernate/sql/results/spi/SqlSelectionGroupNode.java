/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.function.Consumer;

import org.hibernate.annotations.Remove;

/**
 * @author Steve Ebersole
 */
@Remove
public interface SqlSelectionGroupNode {
	Object hydrateStateArray(RowProcessingState currentRowState);
	void visitSqlSelections(Consumer<SqlSelection> action);
}
