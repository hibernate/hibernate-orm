/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.select;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.AbstractStatement;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.results.graph.DomainResult;

/**
 * @author Steve Ebersole
 */
public class SelectStatement extends AbstractStatement {
	private final QueryPart queryPart;
	private final List<DomainResult<?>> domainResults;

	public SelectStatement(QueryPart queryPart) {
		this( queryPart, Collections.emptyList() );
	}

	public SelectStatement(QueryPart queryPart, List<DomainResult<?>> domainResults) {
		this( false, new LinkedHashMap<>(), queryPart, domainResults );
	}

	public SelectStatement(
			CteContainer cteContainer,
			QueryPart queryPart,
			List<DomainResult<?>> domainResults) {
		this( cteContainer.isWithRecursive(), cteContainer.getCteStatements(), queryPart, domainResults );
	}

	public SelectStatement(
			boolean withRecursive,
			Map<String, CteStatement> cteStatements,
			QueryPart queryPart,
			List<DomainResult<?>> domainResults) {
		super( cteStatements );
		this.queryPart = queryPart;
		this.domainResults = domainResults;
		setWithRecursive( withRecursive );
	}

	public QuerySpec getQuerySpec() {
		return queryPart.getFirstQuerySpec();
	}

	public QueryPart getQueryPart() {
		return queryPart;
	}

	public List<DomainResult<?>> getDomainResultDescriptors() {
		return domainResults;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitSelectStatement( this );
	}
}
