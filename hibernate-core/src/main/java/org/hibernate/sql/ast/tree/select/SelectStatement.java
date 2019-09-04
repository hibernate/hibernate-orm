/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.select;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.results.spi.DomainResult;

/**
 * @author Steve Ebersole
 */
public class SelectStatement implements Statement {
	private final QuerySpec querySpec;
	private final List<DomainResult> domainResults;
	private final Set<String> affectedTableExpressions;

	public SelectStatement(
			QuerySpec querySpec,
			List<DomainResult> domainResults,
			Set<String> affectedTableExpressions) {
		this.querySpec = querySpec;
		this.domainResults = Collections.unmodifiableList( domainResults );
		this.affectedTableExpressions = Collections.unmodifiableSet( affectedTableExpressions );
	}

	public QuerySpec getQuerySpec() {
		return querySpec;
	}

	public List<DomainResult> getDomainResultDescriptors() {
		return domainResults;
	}

	public Set<String> getAffectedTableExpressions() {
		return affectedTableExpressions;
	}
}
