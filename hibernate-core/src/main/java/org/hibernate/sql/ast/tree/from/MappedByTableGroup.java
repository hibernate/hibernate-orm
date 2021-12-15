/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.query.NavigablePath;

/**
 * @author Christian Beikov
 */
public class MappedByTableGroup extends DelegatingTableGroup implements VirtualTableGroup {

	private final NavigablePath navigablePath;
	private final ModelPartContainer modelPart;
	private final TableGroup underlyingTableGroup;
	private final boolean fetched;
	private final TableGroup parentTableGroup;
	private final BiPredicate<NavigablePath, String> navigablePathChecker;

	public MappedByTableGroup(
			NavigablePath navigablePath,
			ModelPartContainer modelPart,
			TableGroup underlyingTableGroup,
			boolean fetched,
			TableGroup parentTableGroup,
			BiPredicate<NavigablePath, String> navigablePathChecker) {
		this.navigablePath = navigablePath;
		this.modelPart = modelPart;
		this.underlyingTableGroup = underlyingTableGroup;
		this.fetched = fetched;
		this.parentTableGroup = parentTableGroup;
		this.navigablePathChecker = navigablePathChecker;
	}

	@Override
	protected TableGroup getTableGroup() {
		return underlyingTableGroup;
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

	// Don't provide access to table group joins as this is table group is just a "named reference"
	// The underlying table group contains the joins and will render them

	@Override
	public boolean isRealTableGroup() {
		return false;
	}

	@Override
	public boolean isLateral() {
		return false;
	}

	@Override
	public List<TableGroupJoin> getTableGroupJoins() {
		return Collections.emptyList();
	}

	@Override
	public List<TableGroupJoin> getNestedTableGroupJoins() {
		return Collections.emptyList();
	}

	@Override
	public void visitTableGroupJoins(Consumer<TableGroupJoin> consumer) {
		// No-op
	}

	@Override
	public void visitNestedTableGroupJoins(Consumer<TableGroupJoin> consumer) {
		// No-op
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return Collections.emptyList();
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
		if ( allowFkOptimization && ( navigablePath == null || navigablePathChecker.test( navigablePath, tableExpression ) ) ) {
			final TableReference reference = parentTableGroup.getTableReference(
					navigablePath,
					tableExpression,
					allowFkOptimization,
					resolve
			);
			if ( reference != null ) {
				return reference;
			}
		}

		return underlyingTableGroup.getTableReference(
				navigablePath,
				tableExpression,
				allowFkOptimization,
				resolve
		);
	}
}
