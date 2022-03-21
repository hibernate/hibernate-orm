/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.spi.NavigablePath;

/**
 * @author Christian Beikov
 */
public class SyntheticVirtualTableGroup extends AbstractTableGroup implements VirtualTableGroup {
	private final TableGroup underlyingTableGroup;
	private final TableReference syntheticTableReference;

	public SyntheticVirtualTableGroup(
			NavigablePath navigablePath,
			ModelPartContainer modelPart,
			TableGroup underlyingTableGroup) {
		super(
				underlyingTableGroup.canUseInnerJoins(),
				navigablePath,
				modelPart,
				underlyingTableGroup.getSourceAlias(),
				null,
				null
		);
		this.underlyingTableGroup = underlyingTableGroup;
		this.syntheticTableReference = new NamedTableReference(
				navigablePath.getFullPath(),
				navigablePath.getLocalName(),
				false,
				null
		);
	}

	@Override
	public ModelPartContainer getExpressionType() {
		return getModelPart();
	}

	@Override
	public boolean isFetched() {
		return false;
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
	}

	@Override
	public TableReference getPrimaryTableReference() {
		return syntheticTableReference;
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return Collections.emptyList();
	}

	@Override
	public TableReference getTableReferenceInternal(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization,
			boolean resolve) {
		final TableReference tableReference = underlyingTableGroup.getPrimaryTableReference()
				.getTableReference( navigablePath, tableExpression, allowFkOptimization, resolve );
		if ( tableReference != null ) {
			return syntheticTableReference;
		}
		return null;
	}

}
