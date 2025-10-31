/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.util;

import org.hibernate.query.criteria.JpaEntityJoin;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import static org.hibernate.processor.util.NullnessUtil.castNonNull;


public final class SqmTypeUtils {
	private SqmTypeUtils() {
	}

	public static String resultType(SqmSelectStatement<?> selectStatement) {
		// HQL based queries are ensured to have a select clause by SemanticQueryBuilder
		final JpaSelection<?> selection = castNonNull( selectStatement.getSelection() );
		if (selection instanceof SqmSelectClause from) {
			return from.getSelectionItems().size() > 1
					? "Object[]"
					: from.getSelectionItems().get(0).getJavaTypeName();
		}
		else if (selection instanceof JpaRoot<?> root) {
			return root.getJavaTypeName();
		}
		else if (selection instanceof JpaEntityJoin<?, ?> join) {
			return join.getJavaTypeName();
		}
		else {
			return selection.getJavaTypeName();
		}
	}

}
