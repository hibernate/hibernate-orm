/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * @author Steve Ebersole
 */
public class TableGroupJoin implements TableJoin, DomainResultProducer {
	private final NavigablePath navigablePath;
	private final SqlAstJoinType sqlAstJoinType;
	private final TableGroup joinedGroup;

	private Predicate predicate;

	public TableGroupJoin(
			NavigablePath navigablePath,
			SqlAstJoinType sqlAstJoinType,
			TableGroup joinedGroup) {
		this( navigablePath, sqlAstJoinType, joinedGroup, null );
	}

	public TableGroupJoin(
			NavigablePath navigablePath,
			SqlAstJoinType sqlAstJoinType,
			TableGroup joinedGroup,
			Predicate predicate) {
		assert !joinedGroup.isLateral() || ( sqlAstJoinType == SqlAstJoinType.INNER
				|| sqlAstJoinType == SqlAstJoinType.LEFT
				|| sqlAstJoinType == SqlAstJoinType.CROSS )
				: "Lateral is only allowed with inner, left or cross joins";
		this.navigablePath = navigablePath;
		this.sqlAstJoinType = sqlAstJoinType;
		this.joinedGroup = joinedGroup;
		this.predicate = predicate;
	}

	@Override
	public SqlAstJoinType getJoinType() {
		return sqlAstJoinType;
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

	public void applyPredicate(Predicate predicate) {
		this.predicate = SqlAstTreeHelper.combinePredicates( this.predicate, predicate );
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
			String resultVariable,
			DomainResultCreationState creationState) {
		return getJoinedGroup().createDomainResult( resultVariable, creationState );
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		getJoinedGroup().applySqlSelections( creationState );
	}
}
