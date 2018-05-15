/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import java.util.Map;
import java.util.Set;

import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;

/**
 * @author Steve Ebersole
 */
public interface SqmSelectStatement extends SqmStatement {
	/**
	 * The SQM AST
	 */
	SqmQuerySpec getQuerySpec();

	/**
	 * Any explicit fetch joins defined in the query
	 */
	Map<NavigablePath, Set<SqmNavigableJoin>> getFetchJoinsByParentPath();
}
