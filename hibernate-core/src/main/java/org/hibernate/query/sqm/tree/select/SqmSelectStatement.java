/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.tree.AbstractSqmStatement;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;

/**
 * @author Steve Ebersole
 */
public class SqmSelectStatement extends AbstractSqmStatement {
	private SqmQuerySpec querySpec;
	private Map<NavigablePath, Set<SqmNavigableJoin>> fetchJoinsByParentPath;

	public SqmSelectStatement() {
	}

	public SqmSelectStatement(SqmQuerySpec querySpec) {
		this.querySpec = querySpec;
	}

	public SqmQuerySpec getQuerySpec() {
		return querySpec;
	}

	public void setQuerySpec(SqmQuerySpec querySpec) {
		this.querySpec = querySpec;
	}

	public Map<NavigablePath, Set<SqmNavigableJoin>> getFetchJoinsByParentPath() {
		return fetchJoinsByParentPath == null ? Collections.emptyMap() : Collections.unmodifiableMap( fetchJoinsByParentPath );
	}

	public void applyFetchJoinsByParentPath(Map<NavigablePath, Set<SqmNavigableJoin>> fetchJoinsByParentPath) {
		this.fetchJoinsByParentPath = fetchJoinsByParentPath;
	}
}
