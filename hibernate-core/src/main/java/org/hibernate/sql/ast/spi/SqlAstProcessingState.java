/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

/**
 * Generalized access to state information relative to the "current process" of
 * creating a SQL AST.
 *
 * @author Steve Ebersole
 */
public interface SqlAstProcessingState {
	SqlAstProcessingState getParentState();

	SqlExpressionResolver getSqlExpressionResolver();

	SqlAstCreationState getSqlAstCreationState();

	default boolean isTopLevel() {//todo: naming
		return getParentState() == null;
	}
}
