/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.cte;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;

/**
 * Wraps a {@link TableReference} representing the CTE and adapts it to
 * {@link org.hibernate.sql.ast.tree.from.TableGroup} for use in SQL AST
 *
 * @author Steve Ebersole
 */
public class CteTableGroup implements TableGroup {
	private final NavigablePath navigablePath;
	private final TableReference cteTableReference;

	@SuppressWarnings("WeakerAccess")
	public CteTableGroup(TableReference cteTableReference) {
		this.navigablePath = new NavigablePath( cteTableReference.getTableExpression() );
		this.cteTableReference = cteTableReference;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public String getSourceAlias() {
		return null;
	}

	@Override
	public ModelPartContainer getModelPart() {
		return null;
	}

	@Override
	public ModelPart getExpressionType() {
		return getModelPart();
	}

	@Override
	public List<TableGroupJoin> getTableGroupJoins() {
		return Collections.emptyList();
	}

	@Override
	public boolean canUseInnerJoins() {
		return false;
	}

	@Override
	public TableReference getTableReference(NavigablePath navigablePath, String tableExpression) {
		if ( cteTableReference.getTableExpression().equals( tableExpression ) ) {
			return cteTableReference;
		}
		return null;
	}

	@Override
	public TableReference resolveTableReference(NavigablePath navigablePath, String tableExpression) {
		return cteTableReference;
	}

	@Override
	public void visitTableGroupJoins(Consumer<TableGroupJoin> consumer) {
	}

	@Override
	public String getGroupAlias() {
		return null;
	}

	@Override
	public boolean hasTableGroupJoins() {
		return false;
	}

	@Override
	public boolean isInnerJoinPossible() {
		return false;
	}

	@Override
	public void addTableGroupJoin(TableGroupJoin join) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		nameCollector.accept( cteTableReference.getTableExpression() );
	}

	@Override
	public TableReference getPrimaryTableReference() {
		return cteTableReference;
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return Collections.emptyList();
	}
}
