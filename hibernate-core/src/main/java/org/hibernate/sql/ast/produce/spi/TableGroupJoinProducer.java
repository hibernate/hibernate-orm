/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import org.hibernate.LockMode;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;

/**
 * @author Steve Ebersole
 */
public interface TableGroupJoinProducer extends TableGroupProducer {
	/**
	 * Create the TableGroupJoin as defined for this producer and given
	 * arguments, being sure to add the created TableGroupJoin to the
	 * context's TableSpace.
	 *
	 * @param tableGroupInfoSource Information about the TableGroupJoin to be built (alias, etc)
	 * @param joinType The type of SQL join to generate
	 * @param tableGroupJoinContext Access to information about the context that the TableGroupJoin is being applied to
	 *
	 * @return The generated TableGroupJoin
	 */
	TableGroupJoin createTableGroupJoin(
			TableGroupInfo tableGroupInfoSource,
			JoinType joinType,
			JoinedTableGroupContext tableGroupJoinContext);

	TableGroupJoin createTableGroupJoin(
			SqlAliasBaseGenerator sqlAliasBaseGenerator,
			NavigableContainerReference lhs,
			SqlExpressionResolver sqlExpressionResolver, NavigablePath navigablePath,
			JoinType joinType,
			String identificationVariable,
			LockMode lockMode,
			TableSpace tableSpace);
}
