/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.embeddable;

import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;

/**
 * @author Steve Ebersole
 */
public interface EmbeddableValuedFetchable extends EmbeddableValuedModelPart, Fetchable {
	@Override
	default SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup) {
		return SqlAstJoinType.LEFT;
	}

	@Override
	default boolean isSimpleJoinPredicate(Predicate predicate) {
		return predicate == null;
	}
}
