/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.util.List;

import org.hibernate.SetOperator;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;

/**
 * A grouped list of queries connected through a certain set operator.
 *
 * @author Christian Beikov
 */
public class SqmQueryGroup<T> extends SqmQueryPart<T> {

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

	public List<SqmQueryPart<T>> getQueryParts() {
		return queryParts;
	}

	public SetOperator getSetOperator() {
		return setOperator;
	}

	public void setSetOperator(SetOperator setOperator) {
		this.setOperator = setOperator;
	}
}
