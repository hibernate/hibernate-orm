/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.update;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.sql.ast.tree.expression.ColumnReference;

/**
 * @author Steve Ebersole
 */
public interface Assignable {
	List<ColumnReference> getColumnReferences();

	default void visitColumnReferences(Consumer<ColumnReference> columnReferenceConsumer) {
		getColumnReferences().forEach( columnReferenceConsumer );
	}

}
