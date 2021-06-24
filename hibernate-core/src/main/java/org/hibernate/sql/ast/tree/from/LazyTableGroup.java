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
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAliasBase;

/**
 * @author Christian Beikov
 */
public class LazyTableGroup extends AbstractColumnReferenceQualifier implements TableGroup {

	private final NavigablePath navigablePath;
	private final TableGroupProducer producer;
	private final String sourceAlias;
	private final SqlAliasBase sqlAliasBase;
	private final SessionFactoryImplementor sessionFactory;
	private final Supplier<TableGroup> tableGroupSupplier;
	private final TableGroup parentTableGroup;
	private final Predicate<NavigablePath> navigablePathChecker;
	private Consumer<TableGroup> tableGroupConsumer;
	private TableGroup tableGroup;

	public LazyTableGroup(
			NavigablePath navigablePath,
			Supplier<TableGroup> tableGroupSupplier,
			Predicate<NavigablePath> navigablePathChecker,
			TableGroupProducer tableGroupProducer,
			String sourceAlias,
			SqlAliasBase sqlAliasBase,
			SessionFactoryImplementor sessionFactory,
			TableGroup parentTableGroup) {
		this.navigablePath = navigablePath;
		this.producer = tableGroupProducer;
		this.sourceAlias = sourceAlias;
		this.sqlAliasBase = sqlAliasBase;
		this.tableGroupSupplier = tableGroupSupplier;
		this.navigablePathChecker = navigablePathChecker;
		this.parentTableGroup = parentTableGroup;
		this.sessionFactory = sessionFactory;
	}

	public TableGroup getUnderlyingTableGroup() {
		return tableGroup;
	}

	public TableGroup getTableGroup() {
		if ( tableGroup != null ) {
			return tableGroup;
		}

		tableGroup = tableGroupSupplier.get();
		if ( tableGroupConsumer != null ) {
			tableGroupConsumer.accept( tableGroup );
		}
		return tableGroup;
	}

	public void setTableGroupInitializerCallback(Consumer<TableGroup> tableGroupConsumer) {
		this.tableGroupConsumer = tableGroupConsumer;
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		if ( tableGroup != null ) {
			tableGroup.applyAffectedTableNames( nameCollector );
		}
	}

	@Override
	public TableReference getPrimaryTableReference() {
		return getTableGroup().getPrimaryTableReference();
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return tableGroup == null ? Collections.emptyList() : tableGroup.getTableReferenceJoins();
	}

	@Override
	public List<TableGroupJoin> getTableGroupJoins() {
		return tableGroup == null ? Collections.emptyList() : tableGroup.getTableGroupJoins();
	}

	@Override
	public boolean hasTableGroupJoins() {
		return tableGroup != null && tableGroup.hasTableGroupJoins();
	}

	@Override
	public void addTableGroupJoin(TableGroupJoin join) {
		getTableGroup().addTableGroupJoin( join );
	}

	@Override
	public void visitTableGroupJoins(Consumer<TableGroupJoin> consumer) {
		if ( tableGroup != null ) {
			tableGroup.visitTableGroupJoins( consumer );
		}
	}

	@Override
	public boolean isOuterJoined() {
		if ( tableGroup != null ) {
			return parentTableGroup.isOuterJoined() || tableGroup.isOuterJoined();
		}
		return parentTableGroup.isOuterJoined();
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
	protected SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public boolean isInnerJoinPossible() {
		return false;
	}

	@Override
	public boolean isRealTableGroup() {
		return false;
	}

	@Override
	public TableReference getTableReferenceInternal(
			NavigablePath navigablePath,
			String tableExpression) {
		if ( navigablePath == null || navigablePathChecker.test( navigablePath ) ) {
			final TableReference reference = parentTableGroup.getTableReference(
					navigablePath,
					tableExpression
			);
			if ( reference != null ) {
				return reference;
			}
		}
		return getTableGroup().getTableReference( navigablePath, tableExpression );
	}

}
