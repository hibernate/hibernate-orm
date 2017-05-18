/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.queryable.spi;

import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;

/**
 * Contract for things that can produce the {@link TableGroup} that is the root of a
 * {@link TableSpace}.
 *
 * @author Steve Ebersole
 */
public interface RootTableGroupProducer {
	/**
	 * Create the root TableGroup as defined by this producer given the
	 * NavigableReferenceInfo, being sure to add it to the passed
	 * RootTableGroupContext.
	 *
	 * @param navigableReferenceInfo Information about the TableGroup to be built (alias, etc)
	 * @param tableGroupContext Access to information about the context that the TableGroup is being applied to
	 * @param sqlAliasBaseResolver Access to the component responsible for determining the "SQL alias base"
	 *
	 * @return The generated EntityTableGroup
	 */
	TableGroup createRootTableGroup(
			NavigableReferenceInfo navigableReferenceInfo,
			RootTableGroupContext tableGroupContext,
			SqlAliasBaseResolver sqlAliasBaseResolver);
}
