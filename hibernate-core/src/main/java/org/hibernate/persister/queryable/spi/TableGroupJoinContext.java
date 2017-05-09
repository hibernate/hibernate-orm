/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.queryable.spi;

import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;

/**
 * Parameter object passed to {@link TableGroupJoinProducer#applyTableGroupJoin}
 * giving access to information about the context into which the TableGroup is
 * being applied.  This can be used to query that context and to some degree to
 * alter it.  Generally speaking, though, restrictions should be encoded into
 * the TableGroupJoin predicate.
 *
 * @author Steve Ebersole
 */
public interface TableGroupJoinContext {
	QuerySpec getQuerySpec();
	TableSpace getTableSpace();
}
