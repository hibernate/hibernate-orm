/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.from;

import java.util.List;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.Table;
import org.hibernate.sql.ast.expression.domain.ColumnBindingSource;
import org.hibernate.sql.ast.expression.domain.DomainReferenceExpression;

/**
 * Group together related {@link TableBinding} references (generally related by EntityPersister or CollectionPersister),
 *
 * @author Steve Ebersole
 */
public interface TableGroup extends ColumnBindingSource, DomainReferenceExpression {
	TableSpace getTableSpace();
	String getUid();
	String getAliasBase();
	TableBinding getRootTableBinding();
	List<TableJoin> getTableJoins();

	TableBinding locateTableBinding(Table table);
	ColumnBinding resolveColumnBinding(Column column);
}
