/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.from;

import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.SqlTreeCreationLogger;
import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.PredicateContainer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * @author Steve Ebersole
 */
public class TableGroupJoin implements TableJoin, PredicateContainer, DomainResultProducer {
	private final NavigablePath navigablePath;
	private final TableGroup joinedGroup;

	private SqlAstJoinType joinType;
	private Predicate predicate;

	public TableGroupJoin(
			NavigablePath navigablePath,
			SqlAstJoinType joinType,
			TableGroup joinedGroup) {
		this( navigablePath, joinType, joinedGroup, null );
	}

	public TableGroupJoin(
			NavigablePath navigablePath,
			SqlAstJoinType joinType,
			TableGroup joinedGroup,
			Predicate predicate) {
		assert !joinedGroup.isLateral() || ( joinType == SqlAstJoinType.INNER
				|| joinType == SqlAstJoinType.LEFT
				|| joinType == SqlAstJoinType.CROSS )
				: "Lateral is only allowed with inner, left or cross joins";
		this.navigablePath = navigablePath;
		this.joinType = joinType;
		this.joinedGroup = joinedGroup;
		this.predicate = predicate;
	}

	@Override
	public SqlAstJoinType getJoinType() {
		return joinType;
	}

	public void setJoinType(SqlAstJoinType joinType) {
		SqlTreeCreationLogger.LOGGER.tracef(
				"Adjusting join-type for TableGroupJoin(%s) : %s -> %s",
				navigablePath,
				this.joinType,
				joinType
		);
		this.joinType = joinType;
	}

	public TableGroup getJoinedGroup() {
		return joinedGroup;
	}

	@Override
	public SqlAstNode getJoinedNode() {
		return joinedGroup;
	}

	@Override
	public Predicate getPredicate() {
		return predicate;
	}

	@Override
	public void applyPredicate(Predicate predicate) {
		this.predicate = SqlAstTreeHelper.combinePredicates( this.predicate, predicate );
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitTableGroupJoin( this );
	}

	@Override
	public boolean isInitialized() {
		return joinedGroup.isInitialized();
	}

	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	public boolean isImplicit() {
		return !navigablePath.isAliased();
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		return getJoinedGroup().createDomainResult( resultVariable, creationState );
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		getJoinedGroup().applySqlSelections( creationState );
	}
}
