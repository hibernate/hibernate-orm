/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.from;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAliasBase;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTableGroup extends AbstractColumnReferenceQualifier implements TableGroup {
	private final boolean canUseInnerJoins;
	private final NavigablePath navigablePath;
	private final ModelPartContainer modelPartContainer;
	private final String sourceAlias;
	private final SqlAliasBase sqlAliasBase;

	private List<TableGroupJoin> tableGroupJoins;
	private List<TableGroupJoin> nestedTableGroupJoins;

	private final SessionFactoryImplementor sessionFactory;

	public AbstractTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			ModelPartContainer modelPartContainer,
			String sourceAlias,
			SqlAliasBase sqlAliasBase,
			SessionFactoryImplementor sessionFactory) {
		super();
		this.canUseInnerJoins = canUseInnerJoins;
		this.navigablePath = navigablePath;
		this.modelPartContainer = modelPartContainer;
		this.sourceAlias = sourceAlias;
		this.sqlAliasBase = sqlAliasBase;
		this.sessionFactory = sessionFactory;
	}

	public SqlAliasBase getSqlAliasBase() {
		return sqlAliasBase;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public String getGroupAlias() {
		return sqlAliasBase == null ? null : sqlAliasBase.getAliasStem();
	}

	@Override
	public ModelPartContainer getModelPart() {
		return modelPartContainer;
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
		return canUseInnerJoins;
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
	public void prependTableGroupJoin(NavigablePath navigablePath, TableGroupJoin join) {
		int i = 0;
		if ( tableGroupJoins != null ) {
			for ( ; i < tableGroupJoins.size(); i++ ) {
				if ( tableGroupJoins.get( i ).getNavigablePath() == navigablePath ) {
					tableGroupJoins.add( i, join );
					return;
				}
			}
		}
		i = 0;
		if ( nestedTableGroupJoins != null ) {
			for ( ; i < nestedTableGroupJoins.size(); i++ ) {
				if ( nestedTableGroupJoins.get( i ).getNavigablePath() == navigablePath ) {
					nestedTableGroupJoins.add( i, join );
					return;
				}
			}
		}
		throw new NoSuchElementException("Table group for navigable path not found: " + navigablePath);
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
	public String toString() {
		return getClass().getSimpleName() + '(' + getNavigablePath() + ')';
	}
}
