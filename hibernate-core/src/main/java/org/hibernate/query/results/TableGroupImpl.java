/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;

/**
 * A TableGroup created with regards to a SQL ResultSet mapping
 *
 * @author Steve Ebersole
 */
public class TableGroupImpl implements TableGroup {
	private final NavigablePath navigablePath;
	private final String alias;

	private final TableReferenceImpl primaryTableReference;

	private final ModelPartContainer container;
	private final LockMode lockMode;


	public TableGroupImpl(
			NavigablePath navigablePath,
			String alias,
			TableReferenceImpl primaryTableReference,
			ModelPartContainer container,
			LockMode lockMode) {
		this.navigablePath = navigablePath;
		this.alias = alias;
		this.primaryTableReference = primaryTableReference;
		this.container = container;
		this.lockMode = lockMode;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public String getGroupAlias() {
		return alias;
	}

	@Override
	public ModelPartContainer getModelPart() {
		return container;
	}

	@Override
	public ModelPartContainer getExpressionType() {
		return getModelPart();
	}

	@Override
	public LockMode getLockMode() {
		return lockMode;
	}

	@Override
	public List<TableGroupJoin> getTableGroupJoins() {
		return Collections.emptyList();
	}

	@Override
	public boolean hasTableGroupJoins() {
		return false;
	}

	@Override
	public void addTableGroupJoin(TableGroupJoin join) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitTableGroupJoins(Consumer<TableGroupJoin> consumer) {
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {

	}

	@Override
	public TableReference getPrimaryTableReference() {
		return primaryTableReference;
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return Collections.emptyList();
	}

	@Override
	public boolean isInnerJoinPossible() {
		return false;
	}

	@Override
	public TableReference resolveTableReference(NavigablePath navigablePath, String tableExpression) {
		return primaryTableReference;
	}

	@Override
	public TableReference getTableReference(NavigablePath navigablePath, String tableExpression) {
		return primaryTableReference;
	}

	public static class TableReferenceImpl extends TableReference {
		public TableReferenceImpl(
				String tableExpression,
				String identificationVariable,
				boolean isOptional,
				SessionFactoryImplementor sessionFactory) {
			super( tableExpression, identificationVariable, isOptional, sessionFactory );
		}
	}
}
