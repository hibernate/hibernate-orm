/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;

/**
 * @author Steve Ebersole
 */
public interface TableGroupContext {
	QuerySpec getQuerySpec();
	TableSpace getTableSpace();

	/**
	 * Access to the delegate for {@link SqlAliasBase} generation
	 */
	SqlAliasBaseGenerator getSqlAliasBaseGenerator();

	/**
	 * The join type to be used for joins between TableReferences within a TableGroup.
	 * Note that some table-reference joins will automatically use the least restrictive
	 * LEFT join type if it is mapped as optional.
	 */
	JoinType getTableReferenceJoinType();
}
