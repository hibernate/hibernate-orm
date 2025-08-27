/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.from;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.OwnedValuedModelPart;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.spi.NavigablePath;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Christian Beikov
 */
public class StandardVirtualTableGroup extends AbstractTableGroup implements VirtualTableGroup {
	private final TableGroup underlyingTableGroup;
	private final boolean fetched;

	public StandardVirtualTableGroup(
			NavigablePath navigablePath,
			ModelPartContainer modelPart,
			TableGroup underlyingTableGroup,
			boolean fetched) {
		super(
				underlyingTableGroup.canUseInnerJoins(),
				navigablePath,
				modelPart,
				castNonNull( navigablePath.getRealParent() ).getAlias(),
				null,
				null
		);
		this.underlyingTableGroup = underlyingTableGroup;
		this.fetched = fetched;
	}

	@Override
	public ModelPartContainer getExpressionType() {
		return getModelPart();
	}

	@Override
	public TableGroup getUnderlyingTableGroup() {
		return underlyingTableGroup;
	}

	@Override
	public boolean isFetched() {
		return fetched;
	}

	@Override
	public String getSourceAlias() {
		return underlyingTableGroup.getSourceAlias();
	}

	@Override
	public boolean canUseInnerJoins() {
		return underlyingTableGroup.canUseInnerJoins();
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
	public void addTableGroupJoin(TableGroupJoin join) {
		super.addTableGroupJoin( join );
		registerPredicateOnCorrelatedTableGroup( join );
	}

	@Override
	public void prependTableGroupJoin(NavigablePath navigablePath, TableGroupJoin join) {
		super.prependTableGroupJoin( navigablePath, join );
		registerPredicateOnCorrelatedTableGroup( join );
	}

	@Override
	public void addNestedTableGroupJoin(TableGroupJoin join) {
		super.addNestedTableGroupJoin( join );
		registerPredicateOnCorrelatedTableGroup( join );
	}

	private void registerPredicateOnCorrelatedTableGroup(TableGroupJoin join) {
		TableGroup tableGroup = underlyingTableGroup;
		while ( tableGroup instanceof StandardVirtualTableGroup standardVirtualTableGroup ) {
			tableGroup = standardVirtualTableGroup.underlyingTableGroup;
		}
		if ( tableGroup instanceof CorrelatedTableGroup correlatedTableGroup ) {
			correlatedTableGroup.getJoinPredicateConsumer().accept( join.getPredicate() );
		}
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean resolve) {
		final TableReference tableReference = underlyingTableGroup.getTableReference(
				navigablePath,
				tableExpression,
				resolve
		);
		if ( tableReference != null ) {
			return tableReference;
		}

		for ( TableReferenceJoin tableJoin : getTableReferenceJoins() ) {
			final TableReference joinedTableReference = tableJoin.getJoinedTableReference().getTableReference(
					navigablePath,
					tableExpression,
					resolve
			);
			if ( joinedTableReference != null) {
				return joinedTableReference;
			}
		}
		return null;
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			ValuedModelPart modelPart,
			String tableExpression,
			boolean resolve) {
		final ValuedModelPart parentModelPart =
				modelPart instanceof OwnedValuedModelPart ownedValuedModelPart
					&& ownedValuedModelPart.getDeclaringType() instanceof EmbeddableMappingType declaringType
						? declaringType.getEmbeddedValueMapping()
						: modelPart;
		final TableReference tableReference = underlyingTableGroup.getTableReference(
				navigablePath,
				parentModelPart,
				tableExpression,
				resolve
		);
		if ( tableReference != null ) {
			return tableReference;
		}

		for ( TableReferenceJoin tableJoin : getTableReferenceJoins() ) {
			final TableReference joinedTableReference = tableJoin.getJoinedTableReference().getTableReference(
					navigablePath,
					modelPart,
					tableExpression,
					resolve
			);
			if ( joinedTableReference != null) {
				return joinedTableReference;
			}
		}
		return null;
	}
}
