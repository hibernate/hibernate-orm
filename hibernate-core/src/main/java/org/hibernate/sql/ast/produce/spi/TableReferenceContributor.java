/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;

/**
 * @author Steve Ebersole
 */
public interface TableReferenceContributor {
	/**
	 * Apply the Tables mapped by this producer to the collector as TableReferences
	 */
	void applyTableReferenceJoins(
			ColumnReferenceSource lhs,
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector,
			TableGroupContext tableGroupContext);
}
