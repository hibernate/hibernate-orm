/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.query.spi.NavigablePath;

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
				underlyingTableGroup.getSourceAlias(),
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
	public TableReference getTableReferenceInternal(
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
		return super.getTableReferenceInternal( navigablePath, tableExpression, allowFkOptimization, resolve );
	}

}
