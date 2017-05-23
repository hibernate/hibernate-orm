/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfoSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;

/**
 * @author Steve Ebersole
 */
public interface TableGroupJoinProducer extends TableGroupProducer {

	// todo (6.0) : this should only be applied to "joinable Navigables"
	//		atm it is applied to EntityPersister e.g. which is wrong.  It
	//		should be applied to things like:
	//			1) SingularPersistentAttributeEntity
	//			2) ...

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
			TableGroupInfoSource tableGroupInfoSource,
			JoinType joinType,
			JoinedTableGroupContext tableGroupJoinContext);
}
