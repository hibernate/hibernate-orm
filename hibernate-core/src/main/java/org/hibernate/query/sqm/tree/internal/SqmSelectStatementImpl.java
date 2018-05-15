/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.internal;

import java.util.Map;
import java.util.Set;

import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.tree.SqmQuerySpec;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;

/**
 * @author Steve Ebersole
 */
public class SqmSelectStatementImpl extends AbstractSqmStatement implements SqmSelectStatement {
	private Map<NavigablePath, Set<SqmNavigableJoin>> fetchJoinsByParentPath;
	private SqmQuerySpec querySpec;

	public SqmSelectStatementImpl() {
	}

	@Override
	public Map<NavigablePath, Set<SqmNavigableJoin>> getFetchJoinsByParentPath() {
		return fetchJoinsByParentPath;
	}

	@Override
	public SqmQuerySpec getQuerySpec() {
		return querySpec;
	}

	public void applyQuerySpec(
			SqmQuerySpec querySpec,
			Map<NavigablePath, Set<SqmNavigableJoin>> fetchJoinsByParentPath) {
		if ( this.querySpec != null ) {
			throw new IllegalStateException( "SqmQuerySpec was already defined for select-statement" );
		}

		this.querySpec = querySpec;
		this.fetchJoinsByParentPath = fetchJoinsByParentPath;
	}
}
