/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTableGroup extends AbstractColumnReferenceQualifier implements TableGroup {

	private final NavigablePath navigablePath;
	private final TableGroupProducer producer;
	private final LockMode lockMode;

	private Set<TableGroupJoin> tableGroupJoins;
	private boolean isInnerJoinPossible;
	private final SessionFactoryImplementor sessionFactory;

	@SuppressWarnings("WeakerAccess")
	public AbstractTableGroup(
			NavigablePath navigablePath,
			TableGroupProducer producer,
			LockMode lockMode,
			SessionFactoryImplementor sessionFactory) {
		this( navigablePath, producer, lockMode, false, sessionFactory );
	}

	@SuppressWarnings("WeakerAccess")
	public AbstractTableGroup(
			NavigablePath navigablePath,
			TableGroupProducer producer,
			LockMode lockMode,
			boolean isInnerJoinPossible,
			SessionFactoryImplementor sessionFactory) {
		super();
		this.navigablePath = navigablePath;
		this.producer = producer;
		this.lockMode = lockMode;
		this.isInnerJoinPossible = isInnerJoinPossible;
		this.sessionFactory = sessionFactory;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public TableGroupProducer getModelPart() {
		return producer;
	}

	@Override
	public LockMode getLockMode() {
		return lockMode;
	}

	@Override
	protected SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public Set<TableGroupJoin> getTableGroupJoins() {
		return tableGroupJoins == null ? Collections.emptySet() : Collections.unmodifiableSet( tableGroupJoins );
	}

	@Override
	public boolean hasTableGroupJoins() {
		return tableGroupJoins != null && !tableGroupJoins.isEmpty();
	}

	@Override
	public void setTableGroupJoins(Set<TableGroupJoin> joins) {
		if ( tableGroupJoins == null ) {
			tableGroupJoins = new HashSet<>( joins );
		}
		else {
			tableGroupJoins.addAll( joins );
		}
	}

	@Override
	public void addTableGroupJoin(TableGroupJoin join) {
		if ( tableGroupJoins == null ) {
			tableGroupJoins = new HashSet<>();
		}
		tableGroupJoins.add( join );
	}

	@Override
	public void visitTableGroupJoins(Consumer<TableGroupJoin> consumer) {
		if ( tableGroupJoins != null ) {
			tableGroupJoins.forEach( consumer );
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected void renderTableReference(
			TableReference tableBinding,
			SqlAppender sqlAppender,
			@SuppressWarnings("unused") SqlAstWalker walker) {
		sqlAppender.appendSql( tableBinding.getTableExpression() );

		final String identificationVariable = tableBinding.getIdentificationVariable();
		if ( identificationVariable != null ) {
			sqlAppender.appendSql( " as " + identificationVariable );
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
