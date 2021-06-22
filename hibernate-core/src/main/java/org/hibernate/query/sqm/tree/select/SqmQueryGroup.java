/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.util.Collections;
import java.util.List;

import org.hibernate.query.FetchClauseType;
import org.hibernate.query.SetOperator;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.criteria.JpaQueryGroup;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;

/**
 * A grouped list of queries connected through a certain set operator.
 *
 * @author Christian Beikov
 */
public class SqmQueryGroup<T> extends SqmQueryPart<T> implements JpaQueryGroup<T> {

	private final List<SqmQueryPart<T>> queryParts;
	private SetOperator setOperator;

	public SqmQueryGroup(SqmQueryPart<T> queryPart) {
		this( queryPart.nodeBuilder(), null, CollectionHelper.listOf( queryPart ) );
	}

	public SqmQueryGroup(
			NodeBuilder nodeBuilder,
			SetOperator setOperator,
			List<SqmQueryPart<T>> queryParts) {
		super( nodeBuilder );
		this.setOperator = setOperator;
		this.queryParts = queryParts;
	}

	public List<SqmQueryPart<T>> queryParts() {
		return queryParts;
	}

	@Override
	public SqmQuerySpec<T> getFirstQuerySpec() {
		return queryParts.get( 0 ).getFirstQuerySpec();
	}

	@Override
	public SqmQuerySpec<T> getLastQuerySpec() {
		return queryParts.get( queryParts.size() - 1 ).getLastQuerySpec();
	}

	@Override
	public boolean isSimpleQueryPart() {
		return setOperator == null && queryParts.size() == 1 && queryParts.get( 0 ).isSimpleQueryPart();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitQueryGroup( this );
	}

	@Override
	public List<SqmQueryPart<T>> getQueryParts() {
		return Collections.unmodifiableList( queryParts );
	}

	@Override
	public SetOperator getSetOperator() {
		return setOperator;
	}

	@Override
	public void setSetOperator(SetOperator setOperator) {
		if ( setOperator == null ) {
			throw new IllegalArgumentException();
		}
		this.setOperator = setOperator;
	}


	@Override
	public SqmQueryGroup<T> setSortSpecifications(List<? extends JpaOrder> sortSpecifications) {
		return (SqmQueryGroup<T>) super.setSortSpecifications( sortSpecifications );
	}

	@Override
	public SqmQueryGroup<T> setOffset(JpaExpression<?> offset) {
		return (SqmQueryGroup<T>) super.setOffset( offset );
	}

	@Override
	public SqmQueryGroup<T> setFetch(JpaExpression<?> fetch) {
		return (SqmQueryGroup<T>) super.setFetch( fetch );
	}

	@Override
	public SqmQueryGroup<T> setFetch(JpaExpression<?> fetch, FetchClauseType fetchClauseType) {
		return (SqmQueryGroup<T>) super.setFetch( fetch, fetchClauseType );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		appendQueryPart( queryParts.get( 0 ), sb );
		for ( int i = 1; i < queryParts.size(); i++ ) {
			sb.append( ' ' );
			sb.append( setOperator.sqlString() );
			sb.append( ' ' );
			appendQueryPart( queryParts.get( i ), sb );
		}
		super.appendHqlString( sb );
	}

	private static void appendQueryPart(SqmQueryPart<?> queryPart, StringBuilder sb) {
		final boolean needsParenthesis = !queryPart.isSimpleQueryPart();
		if ( needsParenthesis ) {
			sb.append( '(' );
		}
		queryPart.appendHqlString( sb );
		if ( needsParenthesis ) {
			sb.append( ')' );
		}
	}
}
