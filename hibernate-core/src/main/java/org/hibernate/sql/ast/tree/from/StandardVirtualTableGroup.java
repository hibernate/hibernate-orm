/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.query.NavigablePath;

/**
 * @author Christian Beikov
 */
public class StandardVirtualTableGroup implements VirtualTableGroup {
	private final NavigablePath navigablePath;
	private final ModelPartContainer modelPart;
	private final TableGroup underlyingTableGroup;
	private final boolean fetched;

	private List<TableGroupJoin> tableGroupJoins;
	private List<TableGroupJoin> nestedTableGroupJoins;

	public StandardVirtualTableGroup(
			NavigablePath navigablePath,
			ModelPartContainer modelPart,
			TableGroup underlyingTableGroup,
			boolean fetched) {
		this.navigablePath = navigablePath;
		this.modelPart = modelPart;
		this.underlyingTableGroup = underlyingTableGroup;
		this.fetched = fetched;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public ModelPartContainer getExpressionType() {
		return getModelPart();
	}

	@Override
	public String getGroupAlias() {
		// none, although we could also delegate to the underlyingTableGroup's group-alias
		return null;
	}

	@Override
	public boolean isFetched() {
		return fetched;
	}

	@Override
	public ModelPartContainer getModelPart() {
		return modelPart;
	}

	@Override
	public String getSourceAlias() {
		return underlyingTableGroup.getSourceAlias();
	}

	@Override
	public List<TableGroupJoin> getTableGroupJoins() {
		return tableGroupJoins == null ? Collections.emptyList() : Collections.unmodifiableList( tableGroupJoins );
	}

	@Override
	public List<TableGroupJoin> getNestedTableGroupJoins() {
		return nestedTableGroupJoins == null ? Collections.emptyList() : Collections.unmodifiableList( nestedTableGroupJoins );
	}

	@Override
	public boolean isRealTableGroup() {
		return nestedTableGroupJoins != null && !nestedTableGroupJoins.isEmpty();
	}

	@Override
	public boolean canUseInnerJoins() {
		return underlyingTableGroup.canUseInnerJoins();
	}

	@Override
	public void addTableGroupJoin(TableGroupJoin join) {
		if ( tableGroupJoins == null ) {
			tableGroupJoins = new ArrayList<>();
		}
		assert !tableGroupJoins.contains( join );
		tableGroupJoins.add( join );
	}

	@Override
	public void addNestedTableGroupJoin(TableGroupJoin join) {
		if ( nestedTableGroupJoins == null ) {
			nestedTableGroupJoins = new ArrayList<>();
		}
		assert !nestedTableGroupJoins.contains( join );
		nestedTableGroupJoins.add( join );
	}

	@Override
	public void visitTableGroupJoins(Consumer<TableGroupJoin> consumer) {
		if ( tableGroupJoins != null ) {
			tableGroupJoins.forEach( consumer );
		}
	}

	@Override
	public void visitNestedTableGroupJoins(Consumer<TableGroupJoin> consumer) {
		if ( nestedTableGroupJoins != null ) {
			nestedTableGroupJoins.forEach( consumer );
		}
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		underlyingTableGroup.applyAffectedTableNames( nameCollector );
	}

	@Override
	public TableReference getPrimaryTableReference() {
		return underlyingTableGroup.getPrimaryTableReference();
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return underlyingTableGroup.getTableReferenceJoins();
	}

	@Override
	public TableReference resolveTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization) {
		final TableReference tableReference = getTableReference(
				navigablePath,
				tableExpression,
				allowFkOptimization,
				true
		);
		if ( tableReference == null ) {
			throw new IllegalStateException( "Could not resolve binding for table `" + tableExpression + "`" );
		}

		return tableReference;
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization,
			boolean resolve) {
		final TableReference tableReference = underlyingTableGroup.getTableReference(
				navigablePath,
				tableExpression,
				allowFkOptimization,
				resolve
		);
		if ( tableReference != null ) {
			return tableReference;
		}
		for ( TableGroupJoin tableGroupJoin : getNestedTableGroupJoins() ) {
			final TableReference primaryTableReference = tableGroupJoin.getJoinedGroup()
					.getPrimaryTableReference()
					.getTableReference( navigablePath, tableExpression, allowFkOptimization, resolve );
			if ( primaryTableReference != null ) {
				return primaryTableReference;
			}
		}
		for ( TableGroupJoin tableGroupJoin : getTableGroupJoins() ) {
			final TableReference primaryTableReference = tableGroupJoin.getJoinedGroup()
					.getPrimaryTableReference()
					.getTableReference( navigablePath, tableExpression, allowFkOptimization, resolve );
			if ( primaryTableReference != null ) {
				return primaryTableReference;
			}
		}
		return null;
	}

}
