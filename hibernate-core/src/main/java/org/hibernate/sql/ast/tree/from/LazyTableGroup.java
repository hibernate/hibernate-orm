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
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAliasBase;

/**
 * The purpose of this table group is to defer creating the actual table group until it is really needed.
 * If it is not needed, we can safely skip rendering it. This is useful for ToOneAttributeMapping and EntityCollectionPart,
 * where we need a table group for the association, but aren't sure which columns are needed yet.
 * Deferring initialization enables getting away with fewer joins in case only foreign key columns are used.
 *
 * @author Christian Beikov
 */
public class LazyTableGroup extends DelegatingTableGroup {

	private final boolean canUseInnerJoins;
	private final NavigablePath navigablePath;
	private final boolean fetched;
	private final TableGroupProducer producer;
	private final String sourceAlias;
	private final SqlAliasBase sqlAliasBase;
	private final Supplier<TableGroup> tableGroupSupplier;
	private final TableGroup parentTableGroup;
	private final BiPredicate<NavigablePath, String> navigablePathChecker;
	private List<TableGroupJoin> tableGroupJoins;
	private List<TableGroupJoin> nestedTableGroupJoins;
	private Consumer<TableGroup> tableGroupConsumer;
	private TableGroup tableGroup;

	public LazyTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			boolean fetched,
			Supplier<TableGroup> tableGroupSupplier,
			BiPredicate<NavigablePath, String> navigablePathChecker,
			TableGroupProducer tableGroupProducer,
			String sourceAlias,
			SqlAliasBase sqlAliasBase,
			SessionFactoryImplementor sessionFactory,
			TableGroup parentTableGroup) {
		this.canUseInnerJoins = canUseInnerJoins;
		this.navigablePath = navigablePath;
		this.fetched = fetched;
		this.producer = tableGroupProducer;
		this.sourceAlias = sourceAlias;
		this.sqlAliasBase = sqlAliasBase;
		this.tableGroupSupplier = tableGroupSupplier;
		this.navigablePathChecker = navigablePathChecker;
		this.parentTableGroup = parentTableGroup;
	}

	public TableGroup getUnderlyingTableGroup() {
		return tableGroup;
	}

	@Override
	public TableGroup getTableGroup() {
		if ( tableGroup != null ) {
			return tableGroup;
		}

		tableGroup = tableGroupSupplier.get();
		if ( tableGroupJoins != null ) {
			for ( TableGroupJoin tableGroupJoin : tableGroupJoins ) {
				tableGroup.addTableGroupJoin( tableGroupJoin );
			}
			tableGroupJoins = null;
		}
		if ( nestedTableGroupJoins != null ) {
			for ( TableGroupJoin tableGroupJoin : nestedTableGroupJoins ) {
				tableGroup.addNestedTableGroupJoin( tableGroupJoin );
			}
			nestedTableGroupJoins = null;
		}
		if ( tableGroupConsumer != null ) {
			tableGroupConsumer.accept( tableGroup );
			tableGroupConsumer = null;
		}
		return tableGroup;
	}

	public void setTableGroupInitializerCallback(Consumer<TableGroup> tableGroupConsumer) {
		if ( tableGroup != null ) {
			tableGroupConsumer.accept( tableGroup );
		}
		else {
			this.tableGroupConsumer = tableGroupConsumer;
		}
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		if ( tableGroup != null ) {
			tableGroup.applyAffectedTableNames( nameCollector );
		}
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return tableGroup == null ? Collections.emptyList() : tableGroup.getTableReferenceJoins();
	}

	@Override
	public List<TableGroupJoin> getTableGroupJoins() {
		if ( tableGroup == null ) {
			return nestedTableGroupJoins == null ? Collections.emptyList() : nestedTableGroupJoins;
		}
		else {
			return tableGroup.getTableGroupJoins();
		}
	}

	@Override
	public List<TableGroupJoin> getNestedTableGroupJoins() {
		if ( tableGroup == null ) {
			return tableGroupJoins == null ? Collections.emptyList() : tableGroupJoins;
		}
		else {
			return tableGroup.getNestedTableGroupJoins();
		}
	}

	@Override
	public void addTableGroupJoin(TableGroupJoin join) {
		if ( tableGroup == null ) {
			if ( tableGroupJoins == null ) {
				tableGroupJoins = new ArrayList<>();
			}
			tableGroupJoins.add( join );
		}
		else {
			getTableGroup().addTableGroupJoin( join );
		}
	}

	@Override
	public void addNestedTableGroupJoin(TableGroupJoin join) {
		if ( tableGroup == null ) {
			if ( nestedTableGroupJoins == null ) {
				nestedTableGroupJoins = new ArrayList<>();
			}
			nestedTableGroupJoins.add( join );
		}
		else {
			getTableGroup().addNestedTableGroupJoin( join );
		}
	}

	@Override
	public void visitTableGroupJoins(Consumer<TableGroupJoin> consumer) {
		if ( tableGroup == null ) {
			if ( tableGroupJoins != null ) {
				tableGroupJoins.forEach( consumer );
			}
		}
		else {
			tableGroup.visitTableGroupJoins( consumer );
		}
	}

	@Override
	public void visitNestedTableGroupJoins(Consumer<TableGroupJoin> consumer) {
		if ( tableGroup == null ) {
			if ( nestedTableGroupJoins != null ) {
				nestedTableGroupJoins.forEach( consumer );
			}
		}
		else {
			tableGroup.visitNestedTableGroupJoins( consumer );
		}
	}

	@Override
	public boolean canUseInnerJoins() {
		return canUseInnerJoins;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public String getGroupAlias() {
		return sqlAliasBase.getAliasStem();
	}

	@Override
	public TableGroupProducer getModelPart() {
		return producer;
	}

	@Override
	public ModelPart getExpressionType() {
		return getModelPart();
	}

	@Override
	public String getSourceAlias() {
		return sourceAlias;
	}

	@Override
	public boolean isRealTableGroup() {
		return tableGroup != null && tableGroup.isRealTableGroup();
	}

	@Override
	public boolean isFetched() {
		return fetched;
	}

	@Override
	public boolean isLateral() {
		return false;
	}

	@Override
	public TableReference resolveTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization) {
		assert tableExpression != null;

		final TableReference tableReference = getTableReferenceInternal(
				navigablePath,
				tableExpression,
				allowFkOptimization,
				true
		);

		if ( tableReference == null ) {
			throw new UnknownTableReferenceException(
					tableExpression,
					String.format(
							Locale.ROOT,
							"Unable to determine TableReference (`%s`) for `%s`",
							tableExpression,
							navigablePath.getFullPath()
					)
			);
		}

		return tableReference;
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization,
			boolean resolve) {
		return getTableReferenceInternal( navigablePath, tableExpression, allowFkOptimization, resolve );
	}

	protected TableReference getTableReferenceInternal(
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
		return getTableGroup().getTableReference( navigablePath, tableExpression, allowFkOptimization, resolve );
	}

}
