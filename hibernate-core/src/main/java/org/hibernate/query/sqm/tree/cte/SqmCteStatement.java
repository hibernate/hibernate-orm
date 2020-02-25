/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.cte;

import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.tree.AbstractSqmStatement;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;

/**
 * @author Steve Ebersole
 */
public class SqmCteStatement extends AbstractSqmStatement implements SqmStatement {
	private final SqmCteTable cteTable;
	private final String cteLabel;
	private final SqmQuerySpec cteDefinition;
	private final SqmCteConsumer cteConsumer;

	public SqmCteStatement(
			SqmCteTable cteTable,
			String cteLabel,
			SqmQuerySpec cteDefinition,
			SqmCteConsumer cteConsumer,
			SqmQuerySource querySource,
			NodeBuilder nodeBuilder) {
		super( querySource, nodeBuilder );
		this.cteTable = cteTable;
		this.cteLabel = cteLabel;
		this.cteDefinition = cteDefinition;
		this.cteConsumer = cteConsumer;
	}

	public SqmCteTable getCteTable() {
		return cteTable;
	}

	public String getCteLabel() {
		return cteLabel;
	}

	public SqmQuerySpec getCteDefinition() {
		return cteDefinition;
	}

	public SqmCteConsumer getCteConsumer() {
		return cteConsumer;
	}

	@Override
	public <U> JpaSubQuery<U> subquery(Class<U> type) {
		return new SqmSubQuery<>(
				this,
				new SqmQuerySpec<>( nodeBuilder() ),
				nodeBuilder()
		);
	}

	@Override
	public JpaPredicate getRestriction() {
		return null;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCteStatement( this );
	}
}
