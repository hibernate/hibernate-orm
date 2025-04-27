/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * The semantic select clause.  Defined as a list of individual selections.
 *
 * @author Steve Ebersole
 */
public class SqmSelectClause extends AbstractSqmNode implements SqmAliasedExpressionContainer<SqmSelection<?>>, JpaSelection<Object> {
	private boolean distinct;
	private List<SqmSelection<?>> selections;

	public SqmSelectClause(
			boolean distinct,
			NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.distinct = distinct;
	}

	public SqmSelectClause(
			boolean distinct,
			int expectedNumberOfSelections,
			NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.distinct = distinct;
		this.selections = arrayList( expectedNumberOfSelections );
	}

	@Override
	public SqmSelectClause copy(SqmCopyContext context) {
		final SqmSelectClause selectClause = new SqmSelectClause( distinct, nodeBuilder() );
		if ( selections != null ) {
			selectClause.selections = new ArrayList<>( selections.size() );
			for ( SqmSelection<?> selection : selections ) {
				selectClause.selections.add( selection.copy( context ) );
			}
		}
		return selectClause;
	}

	public boolean isDistinct() {
		return distinct;
	}

	public void makeDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public List<SqmSelection<?>> getSelections() {
		if ( selections == null ) {
			return Collections.emptyList();
		}
		else {
			return Collections.unmodifiableList( selections );
		}
	}

	public void addSelection(SqmSelection<?> selection) {
		if ( selections == null ) {
			selections = new ArrayList<>();
		}
		selections.add( selection );
	}

	@Override
	public SqmSelection<?> add(SqmExpression<?> expression, String alias) {
		final SqmSelection<?> selection = new SqmSelection<>( expression, alias, nodeBuilder()  );
		addSelection( selection );
		return selection;
	}

	@Override
	public void add(SqmSelection<?> aliasExpression) {
		addSelection( aliasExpression );
	}

	public void setSelection(SqmSelection<?> sqmSelection) {
		if ( selections != null ) {
			selections.clear();
		}

		addSelection( sqmSelection );
	}

	public void setSelection(SqmSelectableNode<?> selectableNode) {
		setSelection( new SqmSelection<>( selectableNode, selectableNode.getAlias(), nodeBuilder() ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA stuff

	public JpaSelection<?> resolveJpaSelection() {
		// NOTE : JPA's `Selection` contract is really better named `Selectable`
		return selections != null && selections.size() == 1 ? selections.get( 0 ).getSelectableNode() : this;
	}

	@Override
	public List<SqmSelectableNode<?>> getSelectionItems() {
		final List<SqmSelectableNode<?>> subSelections = new ArrayList<>();
		if ( selections != null ) {
			if ( selections.size() == 1 ) {
				selections.get( 0 ).getSelectableNode().visitSubSelectableNodes( subSelections::add );
			}
			else {
				for ( SqmSelection<?> selection : selections ) {
					selection.getSelectableNode().visitSubSelectableNodes( subSelections::add );
				}
			}
		}
		return subSelections;
	}

	@Override
	public JpaSelection<Object> alias(String name) {
		return null;
	}

	@Override
	public boolean isCompoundSelection() {
		return true;
	}

	@Override
	public JavaType<Object> getJavaTypeDescriptor() {
		return null;
	}

	@Override
	public String getAlias() {
		return null;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof SqmSelectClause that
			&& distinct == that.distinct
			&& Objects.equals( this.selections, that.selections );
	}

	@Override
	public int hashCode() {
		return Objects.hash( distinct, selections );
	}
}
