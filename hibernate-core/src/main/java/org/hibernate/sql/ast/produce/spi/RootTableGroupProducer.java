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
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * Contract for things that can produce the {@link TableGroup} that is the root of a
 * from-clause
 *
 * @author Steve Ebersole
 */
public interface RootTableGroupProducer extends TableGroupProducer {
	/**
	 * Create a root TableGroup as defined by this producer
	 */
	TableGroup createRootTableGroup(
			String uid,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			JoinType tableReferenceJoinType,
			LockMode lockMode,
			SqlAstCreationState creationState);
}
