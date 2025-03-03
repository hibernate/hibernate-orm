/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.from;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.spi.NavigablePath;

/**
 * @author Christian Beikov
 */
public class MappedByTableGroup extends DelegatingTableGroup implements VirtualTableGroup {

	private final NavigablePath navigablePath;
	private final TableGroupProducer producer;
	private final TableGroup underlyingTableGroup;
	private final boolean fetched;
	private final TableGroup parentTableGroup;
	private final LazyTableGroup.ParentTableGroupUseChecker parentTableGroupUseChecker;

	public MappedByTableGroup(
			NavigablePath navigablePath,
			TableGroupProducer producer,
			TableGroup underlyingTableGroup,
			boolean fetched,
			TableGroup parentTableGroup,
			LazyTableGroup.ParentTableGroupUseChecker parentTableGroupUseChecker) {
		this.navigablePath = navigablePath;
		this.producer = producer;
		this.underlyingTableGroup = underlyingTableGroup;
		this.fetched = fetched;
		this.parentTableGroup = parentTableGroup;
		this.parentTableGroupUseChecker = parentTableGroupUseChecker;
	}

	@Override
	protected TableGroup getTableGroup() {
		return underlyingTableGroup;
	}

	@Override
	public TableGroup getUnderlyingTableGroup() {
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
		return producer;
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
			String tableExpression) {
		final TableReference tableReference = getTableReference(
				navigablePath,
				tableExpression,
				true
		);

		if ( tableReference == null ) {
			throw new UnknownTableReferenceException(
					tableExpression,
					String.format(
							Locale.ROOT,
							"Unable to determine TableReference (`%s`) for `%s`",
							tableExpression,
							navigablePath
					)
			);
		}

		return tableReference;
	}

	@Override
	public TableReference resolveTableReference(
			NavigablePath navigablePath,
			ValuedModelPart modelPart,
			String tableExpression) {
		assert modelPart != null;

		final TableReference tableReference = getTableReference(
				navigablePath,
				modelPart,
				tableExpression,
				true
		);

		if ( tableReference == null ) {
			throw new UnknownTableReferenceException(
					tableExpression,
					String.format(
							Locale.ROOT,
							"Unable to determine TableReference (`%s`) for `%s`",
							tableExpression,
							navigablePath
					)
			);
		}

		return tableReference;
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean resolve) {
		return getTableGroup().getTableReference(
				navigablePath,
				tableExpression,
				resolve
		);
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			ValuedModelPart modelPart,
			String tableExpression,
			boolean resolve) {
		if ( parentTableGroupUseChecker.canUseParentTableGroup( producer, navigablePath, modelPart ) ) {
			final TableReference reference = parentTableGroup.getTableReference(
					navigablePath,
					(ValuedModelPart) producer,
					tableExpression,
					resolve
			);
			if ( reference != null ) {
				return reference;
			}
		}
		return getTableGroup().getTableReference( navigablePath, modelPart, tableExpression, resolve );
	}
}
