/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;

/**
 * Parameter object passed to {@link TableGroupJoinProducer#createTableGroupJoin}
 * giving access to information about the context into which the TableGroup is
 * being applied.  This can be used to query that context and to some degree to
 * alter it.  Generally speaking, though, restrictions should be encoded into
 * the TableGroupJoin predicate.
 *
 * @author Steve Ebersole
 */
public interface JoinedTableGroupContext extends TableGroupContext {
	NavigableContainerReference getLhs();

	ColumnReferenceQualifier getColumnReferenceQualifier();

	SqlExpressionResolver getSqlExpressionResolver();

	NavigablePath getNavigablePath();

}
