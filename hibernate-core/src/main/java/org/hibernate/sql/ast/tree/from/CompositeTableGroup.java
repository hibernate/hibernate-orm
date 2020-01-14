/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.query.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class CompositeTableGroup implements VirtualTableGroup {
	private final NavigablePath navigablePath;
	private final EmbeddableValuedModelPart compositionMapping;

	private final TableGroup underlyingTableGroup;

	private Set<TableGroupJoin> tableGroupJoins;

	public CompositeTableGroup(
			NavigablePath navigablePath,
			EmbeddableValuedModelPart compositionMapping,
			TableGroup underlyingTableGroup) {
		this.navigablePath = navigablePath;
		this.compositionMapping = compositionMapping;
		this.underlyingTableGroup = underlyingTableGroup;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public EmbeddableValuedModelPart getExpressionType() {
		return getModelPart();
	}

	@Override
	public String getGroupAlias() {
		// none, although we could also delegate to the underlyingTableGroup's group-alias
		return null;
	}

	@Override
	public EmbeddableValuedModelPart getModelPart() {
		return compositionMapping;
	}

	@Override
	public LockMode getLockMode() {
		return underlyingTableGroup.getLockMode();
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
	public boolean isInnerJoinPossible() {
		return underlyingTableGroup.isInnerJoinPossible();
	}

	@Override
	public TableReference getTableReference(String tableExpression) {
		return underlyingTableGroup.getTableReference( tableExpression );
	}

	@Override
	public TableReference resolveTableReference(
			String tableExpression,
			Supplier<TableReference> creator) {
		return underlyingTableGroup.resolveTableReference( tableExpression, creator );
	}

	@Override
	public TableReference resolveTableReference(String tableExpression) {
		TableReference tableReference = underlyingTableGroup.getTableReference( tableExpression );
		if ( tableReference != null ) {
			return tableReference;
		}
		for ( TableGroupJoin tableGroupJoin : getTableGroupJoins() ) {
			final TableReference primaryTableReference = tableGroupJoin.getJoinedGroup().getPrimaryTableReference();
			if ( primaryTableReference.getTableExpression().equals( tableExpression ) ) {
				return primaryTableReference;
			}
		}
		throw new IllegalStateException( "Could not resolve binding for table `" + tableExpression + "`" );
	}

}
