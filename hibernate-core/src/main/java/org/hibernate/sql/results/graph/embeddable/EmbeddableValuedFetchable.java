/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
