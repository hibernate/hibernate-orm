/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;

import java.util.List;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.Table;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReferenceExpression;

/**
 * Group together related {@link TableReference} references (generally related by EntityPersister or CollectionPersister),
 *
 * @author Steve Ebersole
 */
public interface TableGroup extends ColumnReferenceSource, NavigableReferenceExpression {
	TableSpace getTableSpace();
	String getUid();
	String getAliasBase();
	TableReference getRootTableReference();
	List<TableReferenceJoin> getTableReferenceJoins();

	TableReference locateTableReference(Table table);
	ColumnReference resolveColumnReference(Column column);
}
