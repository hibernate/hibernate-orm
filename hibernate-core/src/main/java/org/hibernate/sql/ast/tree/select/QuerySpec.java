/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.select;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.PredicateContainer;

/**
 * @author Steve Ebersole
 */
public class QuerySpec extends QueryPart implements SqlAstNode, PredicateContainer {

	private final FromClause fromClause;
	private final SelectClause selectClause;

	private Predicate whereClauseRestrictions;

	private List<Expression> groupByClauseExpressions = Collections.emptyList();
	private Predicate havingClauseRestrictions;

	private Set<NavigablePath> rootPathsForLocking;

	public QuerySpec(boolean isRoot) {
		super( isRoot );
		this.fromClause = new FromClause();
		this.selectClause = new SelectClause();
	}

	public QuerySpec(boolean isRoot, int expectedNumberOfRoots) {
		super( isRoot );
		this.fromClause = new FromClause( expectedNumberOfRoots );
		this.selectClause = new SelectClause();
	}

	private QuerySpec(QuerySpec original, boolean root) {
		super( root, original );
		this.fromClause = original.fromClause;
		this.selectClause = original.selectClause;
		this.whereClauseRestrictions = original.whereClauseRestrictions;
		this.groupByClauseExpressions = original.groupByClauseExpressions;
		this.havingClauseRestrictions = original.havingClauseRestrictions;
	}

	public QuerySpec asSubQuery() {
		return isRoot() ? new QuerySpec( this, false ) : this;
	}

	public QuerySpec asRootQuery() {
		return isRoot() ? this : new QuerySpec( this, true );
	}

	@Override
	public QuerySpec getFirstQuerySpec() {
		return this;
	}

	@Override
	public QuerySpec getLastQuerySpec() {
		return this;
	}

	@Override
	public void visitQuerySpecs(Consumer<QuerySpec> querySpecConsumer) {
		querySpecConsumer.accept( this );
	}

	@Override
	public <T> T queryQuerySpecs(Function<QuerySpec, T> querySpecConsumer) {
		return querySpecConsumer.apply( this );
	}

	public FromClause getFromClause() {
		return fromClause;
	}

	public SelectClause getSelectClause() {
		return selectClause;
	}

	/// Set of [NavigablePath] references to be considered roots
	/// for locking purposes.
	public Set<NavigablePath> getRootPathsForLocking() {
		return rootPathsForLocking;
	}

	/// Applies a [NavigablePath] to be considered a root for the
	/// purpose of potential locking.
	public void applyRootPathForLocking(NavigablePath path) {
		if ( rootPathsForLocking == null ) {
			rootPathsForLocking = new HashSet<>();
		}
		rootPathsForLocking.add( path );
	}

	public Predicate getWhereClauseRestrictions() {
		return whereClauseRestrictions;
	}

	@Override
	public void applyPredicate(Predicate predicate) {
		this.whereClauseRestrictions = SqlAstTreeHelper.combinePredicates( this.whereClauseRestrictions, predicate );
	}

	public List<Expression> getGroupByClauseExpressions() {
		return groupByClauseExpressions;
	}

	public void setGroupByClauseExpressions(List<Expression> groupByClauseExpressions) {
		this.groupByClauseExpressions = groupByClauseExpressions == null ? Collections.emptyList() : groupByClauseExpressions;
	}

	public Predicate getHavingClauseRestrictions() {
		return havingClauseRestrictions;
	}

	public void setHavingClauseRestrictions(Predicate havingClauseRestrictions) {
		this.havingClauseRestrictions = havingClauseRestrictions;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitQuerySpec( this );
	}

}
