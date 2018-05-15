/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;

import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.SqlAstNode;

/**
 * @author Steve Ebersole
 */
public class TableGroupJoin implements SqlAstNode {
	private final JoinType joinType;
	private final TableGroup joinedGroup;
	private final Predicate predicate;

	public TableGroupJoin(
			JoinType joinType,
			TableGroup joinedGroup,
			Predicate predicate) {
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
	public void accept(SqlAstWalker  sqlTreeWalker) {
		sqlTreeWalker.visitTableGroupJoin( this );
	}
}
