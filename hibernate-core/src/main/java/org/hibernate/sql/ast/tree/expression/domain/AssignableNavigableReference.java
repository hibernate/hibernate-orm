/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression.domain;

import java.util.function.Consumer;

import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.sqm.spi.SqmUpdateToSqlAstConverterMultiTable;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.update.Assignment;

/**
 * @author Steve Ebersole
 */
public interface AssignableNavigableReference extends NavigableReference {
	// need to be able to collect assignments per-table, including
	// 		SqmParameter -> JdbcParameter mapping


	void applySqlAssignments(
			Expression newValueExpression,
			SqmUpdateToSqlAstConverterMultiTable.AssignmentContext assignmentProcessingState,
			Consumer<Assignment> assignmentConsumer,
			SqlAstCreationContext creationContext);
}
