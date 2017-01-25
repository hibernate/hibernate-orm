/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.from;

import org.hibernate.sql.ast.predicate.Predicate;
import org.hibernate.sqm.query.SqmJoinType;

/**
 * @author Steve Ebersole
 */
public class TableGroupJoin {
	private final SqmJoinType joinType;
	private final TableGroup joinedGroup;
	private final Predicate predicate;

	public TableGroupJoin(
			SqmJoinType joinType,
			TableGroup joinedGroup,
			Predicate predicate) {
		this.joinType = joinType;
		this.joinedGroup = joinedGroup;
		this.predicate = predicate;
	}

	public SqmJoinType getJoinType() {
		return joinType;
	}

	public TableGroup getJoinedGroup() {
		return joinedGroup;
	}

	public Predicate getPredicate() {
		return predicate;
	}
}
