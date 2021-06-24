/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;

/**
 * A TableGroup created with regards to a SQL ResultSet mapping
 *
 * @author Steve Ebersole
 */
public class TableGroupImpl implements TableGroup {
	private final NavigablePath navigablePath;
	private final String alias;

	private final TableReference primaryTableReference;
	private List<TableGroupJoin> tableGroupJoins;

	private final ModelPartContainer container;
	private final String sourceAlias;
	private boolean isOuterJoined;

	public TableGroupImpl(
			NavigablePath navigablePath,
			String alias,
			TableReference primaryTableReference,
			ModelPartContainer container,
			String sourceAlias) {
		this.navigablePath = navigablePath;
		this.alias = alias;
		this.primaryTableReference = primaryTableReference;
		this.container = container;
		this.sourceAlias = sourceAlias;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public String getGroupAlias() {
		return alias;
	}

	@Override
	public ModelPartContainer getModelPart() {
		return container;
	}

	@Override
	public ModelPartContainer getExpressionType() {
		return getModelPart();
	}

	@Override
	public String getSourceAlias() {
		return sourceAlias;
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
	public void applyAffectedTableNames(Consumer<String> nameCollector) {

	}

	@Override
	public TableReference getPrimaryTableReference() {
		return primaryTableReference;
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return Collections.emptyList();
	}

	@Override
	public boolean isInnerJoinPossible() {
		return false;
	}

	@Override
	public TableReference resolveTableReference(NavigablePath navigablePath, String tableExpression) {
		final TableReference tableReference = getTableReference( navigablePath, tableExpression );
		if ( tableReference == null ) {
			throw new IllegalStateException( "Could not resolve binding for table `" + tableExpression + "`" );
		}

		return tableReference;
	}

	@Override
	public TableReference getTableReference(NavigablePath navigablePath, String tableExpression) {
		if ( primaryTableReference.getTableReference( navigablePath , tableExpression ) != null ) {
			return primaryTableReference;
		}

		for ( TableGroupJoin tableGroupJoin : getTableGroupJoins() ) {
			final TableReference primaryTableReference = tableGroupJoin.getJoinedGroup().getPrimaryTableReference();
			if ( primaryTableReference.getTableReference( navigablePath, tableExpression ) != null ) {
				return primaryTableReference;
			}
		}

		return null;
	}

}
