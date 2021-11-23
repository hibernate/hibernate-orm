/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * @author Steve Ebersole
 */
public class SqmCorrelatedRoot<T> extends SqmRoot<T> implements SqmPathWrapper<T,T>, SqmCorrelation<T,T> {

	private final SqmRoot<T> correlationParent;

	public SqmCorrelatedRoot(SqmRoot<T> correlationParent) {
		super(
				correlationParent.getNavigablePath(),
				correlationParent.getReferencedPathSource(),
				null,
				correlationParent.nodeBuilder()
		);
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
	public String getExplicitAlias() {
		return correlationParent.getExplicitAlias();
	}

	@Override
	public void setExplicitAlias(String explicitAlias) {
		throw new UnsupportedOperationException( "Can't set alias on a correlated root" );
	}

	@Override
	public JpaSelection<T> alias(String name) {
		setAlias( name );
		return this;
	}

	@Override
	public boolean isCorrelated() {
		return true;
	}

	@Override
	public SqmRoot<T> getCorrelatedRoot() {
		return this;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCorrelation( this );
	}
}
