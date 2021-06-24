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

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTableGroup extends AbstractColumnReferenceQualifier implements TableGroup {
	private final NavigablePath navigablePath;
	private final TableGroupProducer producer;
	private final String sourceAlias;
	private final SqlAliasBase sqlAliasBase;

	private List<TableGroupJoin> tableGroupJoins;
	private boolean isInnerJoinPossible;
	private boolean isOuterJoined;

	private final SessionFactoryImplementor sessionFactory;

	@SuppressWarnings("WeakerAccess")
	public AbstractTableGroup(
			NavigablePath navigablePath,
			TableGroupProducer producer,
			String sourceAlias,
			SqlAliasBase sqlAliasBase,
			SessionFactoryImplementor sessionFactory) {
		this( navigablePath, producer, sourceAlias, sqlAliasBase, false, sessionFactory );
	}

	@SuppressWarnings("WeakerAccess")
	public AbstractTableGroup(
			NavigablePath navigablePath,
			TableGroupProducer producer,
			String sourceAlias,
			SqlAliasBase sqlAliasBase,
			boolean isInnerJoinPossible,
			SessionFactoryImplementor sessionFactory) {
		super();
		this.navigablePath = navigablePath;
		this.producer = producer;
		this.sourceAlias = sourceAlias;
		this.sqlAliasBase = sqlAliasBase;
		this.isInnerJoinPossible = isInnerJoinPossible;
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
	public List<TableGroupJoin> getTableGroupJoins() {
		return tableGroupJoins == null ? Collections.emptyList() : Collections.unmodifiableList( tableGroupJoins );
	}

	@Override
	public boolean isOuterJoined() {
		return isOuterJoined;
	}

	@Override
	public boolean hasTableGroupJoins() {
		return tableGroupJoins != null && !tableGroupJoins.isEmpty();
	}

	@Override
	public void addTableGroupJoin(TableGroupJoin join) {
		if ( tableGroupJoins == null ) {
			tableGroupJoins = new ArrayList<>();
		}
		if ( join.getJoinType() != SqlAstJoinType.INNER ) {
			isOuterJoined = true;
		}
		if ( !tableGroupJoins.contains( join ) ) {
			tableGroupJoins.add( join );
		}
	}

	@Override
	public void visitTableGroupJoins(Consumer<TableGroupJoin> consumer) {
		if ( tableGroupJoins != null ) {
			tableGroupJoins.forEach( consumer );
		}
	}

	@Override
	public boolean isInnerJoinPossible() {
		return isInnerJoinPossible;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '(' + getNavigablePath() + ')';
	}
}
