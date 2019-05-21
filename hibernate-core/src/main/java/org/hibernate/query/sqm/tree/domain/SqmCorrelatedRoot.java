/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;

/**
 * @author Steve Ebersole
 */
public class SqmCorrelatedRoot<T> extends SqmRoot<T> implements SqmPathWrapper<T,T>, SqmCorrelation<T,T> {
	private SqmRoot<T> correlationParent;

	public SqmCorrelatedRoot(
			SqmRoot<T> correlationParent,
			NodeBuilder nodeBuilder) {
		super( correlationParent.getReferencedPathSource(), correlationParent.getAlias(), nodeBuilder );
		this.correlationParent = correlationParent;
	}

	@Override
	public SqmRoot<T> getCorrelationParent() {
		return correlationParent;
	}

	@Override
	public SqmPath<T> getWrappedPath() {
		return getCorrelationParent();
	}

	@Override
	public boolean isCorrelated() {
		return true;
	}

	@Override
	public SqmFrom<T, T> correlateTo(JpaSubQuery<T> subquery) {
		final SqmSubQuery sqmSubQuery = (SqmSubQuery) subquery;
		final SqmCorrelatedRoot<T> correlation = new SqmCorrelatedRoot<>( this, nodeBuilder() );
		sqmSubQuery.getQuerySpec().getFromClause().addRoot( correlation );
		return correlation;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCorrelation( this );
	}
}
