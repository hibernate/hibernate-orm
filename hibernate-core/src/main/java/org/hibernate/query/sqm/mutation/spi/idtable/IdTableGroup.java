/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.idtable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.mapping.EntityTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.from.AbstractTableGroup;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;

/**
 * Wraps a {@link IdTableReference} and adapts it to
 * {@link org.hibernate.sql.ast.tree.from.TableGroup} for use in SQL AST
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class IdTableGroup extends AbstractTableGroup {
	private final IdTableReference idTableReference;

	public IdTableGroup(EntityTypeDescriptor entityDescriptor, IdTableReference idTableReference) {
		super(
				new NavigablePath( entityDescriptor.getEntityName() ),
				entityDescriptor,
				LockMode.NONE
		);
		this.idTableReference = idTableReference;
	}

	@Override
	public void render(SqlAppender sqlAppender, SqlAstWalker walker) {
		sqlAppender.appendSql( idTableReference.getTable().getTableExpression() );
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		nameCollector.accept( idTableReference.getTable().getTableExpression() );
	}

	@Override
	public IdTableReference getPrimaryTableReference() {
		return idTableReference;
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return Collections.emptyList();
	}

	@Override
	public Column resolveColumn(String columnName) {
		return idTableReference.resolveColumn( columnName );
	}
}
