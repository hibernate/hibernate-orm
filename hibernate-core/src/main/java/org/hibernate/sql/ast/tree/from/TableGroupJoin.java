/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;

/**
 * @author Steve Ebersole
 */
public class TableGroupJoin implements SqlAstNode, DomainResultProducer {
	private final NavigablePath navigablePath;
	private final JoinType joinType;
	private final TableGroup joinedGroup;
	private final Predicate predicate;

	public TableGroupJoin(
			NavigablePath navigablePath,
			JoinType joinType,
			TableGroup joinedGroup,
			Predicate predicate) {
		this.navigablePath = navigablePath;
		this.joinType = joinType;
		this.joinedGroup = joinedGroup;
		this.predicate = predicate;
	}

	public JoinType getJoinType() {
		return joinType;
	}

	public TableGroup getJoinedGroup() {
		return joinedGroup;
	}

	public Predicate getPredicate() {
		return predicate;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitTableGroupJoin( this );
	}

	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public DomainResult createDomainResult(
			int valuesArrayPosition,
			String resultVariable,
			DomainResultCreationState creationState) {
		return getJoinedGroup().createDomainResult( valuesArrayPosition, resultVariable, creationState );
	}
}
