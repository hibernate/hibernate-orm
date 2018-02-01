/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.path.internal;

import org.hibernate.query.sqm.produce.internal.hql.SemanticQueryBuilder;
import org.hibernate.query.sqm.produce.path.spi.AbstractNavigableJoinBuilder;
import org.hibernate.query.sqm.produce.spi.ParsingContext;
import org.hibernate.query.sqm.produce.spi.QuerySpecProcessingState;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;

/**
 * @author Steve Ebersole
 */
public class NavigableJoinBuilderOrderBy extends AbstractNavigableJoinBuilder {
	private final SemanticQueryBuilder queryBuilder;

	public NavigableJoinBuilderOrderBy(SemanticQueryBuilder queryBuilder) {
		this.queryBuilder = queryBuilder;
	}

	@Override
	protected ParsingContext getParsingContext() {
		return queryBuilder.getParsingContext();
	}

	@Override
	protected QuerySpecProcessingState getQuerySpecProcessingState() {
		return queryBuilder.getQuerySpecProcessingState();
	}

	@Override
	public void buildNavigableJoinIfNecessary(
			SqmNavigableReference navigableReference,
			boolean isTerminal) {
		// todo (6.0) : how should we handle a join being triggered just for an order-by?
		//		for now just build it...
		super.buildNavigableJoinIfNecessary( navigableReference, isTerminal );
	}


}
