/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

	private Set<TableGroupJoin> tableGroupJoins;

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
	public Set<TableGroupJoin> getTableGroupJoins() {
		return tableGroupJoins == null ? Collections.emptySet() : tableGroupJoins;
	}

	@Override
	public boolean hasTableGroupJoins() {
		return tableGroupJoins != null && ! tableGroupJoins.isEmpty();
	}

	@Override
	public void setTableGroupJoins(Set<TableGroupJoin> joins) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addTableGroupJoin(TableGroupJoin join) {

	}

	@Override
	public void visitTableGroupJoins(Consumer<TableGroupJoin> consumer) {
		if ( tableGroupJoins == null ) {
			return;
		}

		tableGroupJoins.forEach( consumer );
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
	public TableReference resolveTableReference(
			String tableExpression, Supplier<TableReference> creator) {
		return primaryTableReference;
	}

	@Override
	public TableReference resolveTableReference(String tableExpression) {
		return primaryTableReference;
	}

	@Override
	public TableReference getTableReference(String tableExpression) {
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
