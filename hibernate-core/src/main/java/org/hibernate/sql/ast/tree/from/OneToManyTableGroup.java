/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.from;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * A table group for one-to-many plural attributes.
 * Delegates by default to the element table group,
 * but also provides access to the index table group table references.
 *
 * @author Christian Beikov
 */
public class OneToManyTableGroup extends AbstractColumnReferenceQualifier implements TableGroup, PluralTableGroup {
	private final SessionFactoryImplementor sessionFactory;
	private final PluralAttributeMapping pluralAttributeMapping;
	private final TableGroup elementTableGroup;
	private TableGroup indexTableGroup;

	public OneToManyTableGroup(
			PluralAttributeMapping pluralAttributeMapping,
			TableGroup elementTableGroup,
			SessionFactoryImplementor sessionFactory) {
		this.pluralAttributeMapping = pluralAttributeMapping;
		this.elementTableGroup = elementTableGroup;
		this.sessionFactory = sessionFactory;
	}

	@Override
	public PluralAttributeMapping getExpressionType() {
		return pluralAttributeMapping;
	}

	@Override
	public PluralAttributeMapping getModelPart() {
		return pluralAttributeMapping;
	}

	@Override
	protected SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public TableGroup getElementTableGroup() {
		return elementTableGroup;
	}

	@Override
	public TableGroup getIndexTableGroup() {
		return indexTableGroup;
	}

	public void registerIndexTableGroup(TableGroupJoin indexTableGroupJoin) {
		registerIndexTableGroup( indexTableGroupJoin, true );
	}

	public void registerIndexTableGroup(TableGroupJoin indexTableGroupJoin, boolean nested) {
		assert this.indexTableGroup == null;
		this.indexTableGroup = indexTableGroupJoin.getJoinedGroup();
		if ( nested ) {
			addNestedTableGroupJoin( indexTableGroupJoin );
		}
		else {
			addTableGroupJoin( indexTableGroupJoin );
		}
	}

	@Override
	public String getGroupAlias() {
		return elementTableGroup.getGroupAlias();
	}

	@Override
	public String getSourceAlias() {
		return elementTableGroup.getSourceAlias();
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		elementTableGroup.applyAffectedTableNames( nameCollector );
	}

	@Override
	public TableReference getPrimaryTableReference() {
		return elementTableGroup.getPrimaryTableReference();
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return elementTableGroup.getTableReferenceJoins();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return elementTableGroup.getNavigablePath().getParent();
	}

	@Override
	public List<TableGroupJoin> getTableGroupJoins() {
		return elementTableGroup.getTableGroupJoins();
	}

	@Override
	public List<TableGroupJoin> getNestedTableGroupJoins() {
		return elementTableGroup.getNestedTableGroupJoins();
	}

	@Override
	public boolean canUseInnerJoins() {
		return elementTableGroup.canUseInnerJoins();
	}

	@Override
	public void addTableGroupJoin(TableGroupJoin join) {
		if ( join.getJoinedGroup() != elementTableGroup ) {
			elementTableGroup.addTableGroupJoin( join );
		}
	}

	@Override
	public void prependTableGroupJoin(NavigablePath navigablePath, TableGroupJoin join) {
		if ( join.getJoinedGroup() != elementTableGroup ) {
			elementTableGroup.prependTableGroupJoin( navigablePath, join );
		}
	}

	@Override
	public void addNestedTableGroupJoin(TableGroupJoin join) {
		if ( join.getJoinedGroup() != elementTableGroup ) {
			elementTableGroup.addNestedTableGroupJoin( join );
		}
	}

	@Override
	public void visitTableGroupJoins(Consumer<TableGroupJoin> consumer) {
		elementTableGroup.visitTableGroupJoins( consumer );
	}

	@Override
	public void visitNestedTableGroupJoins(Consumer<TableGroupJoin> consumer) {
		elementTableGroup.visitNestedTableGroupJoins( consumer );
	}

	@Override
	public DomainResult createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		return elementTableGroup.createDomainResult( resultVariable, creationState );
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		elementTableGroup.applySqlSelections( creationState );
	}

	@Override
	public boolean isRealTableGroup() {
		return elementTableGroup.isRealTableGroup();
	}

	@Override
	public boolean isFetched() {
		return elementTableGroup.isFetched();
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean resolve) {
		final TableReference tableReference = elementTableGroup.getTableReference(
				navigablePath,
				tableExpression,
				resolve
		);
		if ( tableReference != null || indexTableGroup == null
				|| navigablePath != null && indexTableGroup.getNavigablePath().isParent( navigablePath ) ) {
			return tableReference;
		}

		return indexTableGroup.getTableReference(
				navigablePath,
				tableExpression,
				resolve
		);
	}
}
