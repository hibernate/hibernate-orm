/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.path.internal;

import org.hibernate.query.sqm.produce.internal.hql.SemanticQueryBuilder;
import org.hibernate.query.sqm.produce.path.spi.AbstractStandardNavigableJoinBuilder;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;

/**
 * @author Steve Ebersole
 */
public class NavigableJoinBuilderFromElement extends AbstractStandardNavigableJoinBuilder {
	private final SqmJoinType joinType;
	private final String terminalAlias;
	private final boolean isFetched;

	public NavigableJoinBuilderFromElement(
			SqmJoinType joinType,
			String terminalAlias,
			boolean isFetched,
			SemanticQueryBuilder queryBuilder) {
		super( queryBuilder );
		this.joinType = joinType;
		this.isFetched = isFetched;
		this.terminalAlias = terminalAlias;
	}

	@Override
	protected boolean forceTerminalJoin() {
		return true;
	}

	@Override
	protected SqmJoinType getJoinType() {
		return joinType;
	}

	@Override
	protected String getTerminalJoinAlias() {
		return terminalAlias;
	}

	@Override
	protected boolean isFetched() {
		return isFetched;
	}

	@Override
	protected boolean canReuseJoins() {
		return false;
	}

	@Override
	public void buildNavigableJoinIfNecessary(SqmNavigableReference navigableReference, boolean isTerminal) {
		super.buildNavigableJoinIfNecessary( navigableReference, isTerminal );
	}
}
