/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

import static java.util.Arrays.asList;

/**
 * The semantic select clause.  Defined as a list of individual selections.
 *
 * @author Steve Ebersole
 */
public class SqmSelectClause extends AbstractSqmNode implements SqmAliasedExpressionContainer<SqmSelection>, JpaSelection<Object> {
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
			List<SqmSelection<?>> selections,
			NodeBuilder nodeBuilder) {
		this( distinct, nodeBuilder );
		this.selections = selections;
	}

	public SqmSelectClause(
			boolean distinct,
			NodeBuilder nodeBuilder,
			SqmSelection<?>... selections) {
		this( distinct, asList( selections ), nodeBuilder );
	}

	public boolean isDistinct() {
		return distinct;
	}

	public void makeDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public List<SqmSelection> getSelections() {
		if ( selections == null ) {
			return Collections.emptyList();
		}
		else {
			return Collections.unmodifiableList( selections );
		}
	}

	public void addSelection(SqmSelection selection) {
		if ( selections == null ) {
			selections = new ArrayList<>();
		}
		selections.add( selection );
	}

	@Override
	public SqmSelection add(SqmExpression<?> expression, String alias) {
		final SqmSelection selection = new SqmSelection<>( expression, alias, nodeBuilder()  );
		addSelection( selection );
		return selection;
	}

	@Override
	public void add(SqmSelection aliasExpression) {
		addSelection( aliasExpression );
	}

	public void setSelection(SqmSelection sqmSelection) {
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
		if ( selections == null || selections.size() != 1 ) {
			return this;
		}
		else {
			return selections.get( 0 ).getSelectableNode();
		}
	}

	@Override
	public List<SqmSelectableNode<?>> getSelectionItems() {
		final List<SqmSelectableNode<?>> subSelections = new ArrayList<>();

		//TODO: this has gotta be wrong!!
		if ( this.selections != null || this.selections.size() != 1 ) {
			this.selections.get( 0 ).getSelectableNode().visitSubSelectableNodes( subSelections::add );
		}
		else {
			for ( SqmSelection<?> selection : this.selections ) {
				selection.getSelectableNode().visitSubSelectableNodes( subSelections::add );
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
		return false;
	}

	@Override
	public JavaTypeDescriptor<Object> getJavaTypeDescriptor() {
		return null;
	}

	@Override
	public String getAlias() {
		return null;
	}
}
