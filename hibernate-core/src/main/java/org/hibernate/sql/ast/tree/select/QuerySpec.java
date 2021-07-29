/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.select;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.query.sqm.tree.expression.SqmAliasedNodeRef;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.PredicateContainer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class QuerySpec extends QueryPart implements SqlAstNode, PredicateContainer, Expression, DomainResultProducer {

	private final FromClause fromClause;
	private final SelectClause selectClause = new SelectClause();

	private Predicate whereClauseRestrictions;

	private boolean hasPositionalGroupItem;
	private List<Expression> groupByClauseExpressions = Collections.emptyList();
	private Predicate havingClauseRestrictions;

	public QuerySpec(boolean isRoot) {
		super( isRoot );
		this.fromClause = new FromClause();
	}

	public QuerySpec(boolean isRoot, int expectedNumberOfRoots) {
		super( isRoot );
		this.fromClause = new FromClause( expectedNumberOfRoots );
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
	public void forEachQuerySpec(Consumer<QuerySpec> querySpecConsumer) {
		querySpecConsumer.accept( this );
	}

	public FromClause getFromClause() {
		return fromClause;
	}

	public SelectClause getSelectClause() {
		return selectClause;
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

	public boolean hasPositionalGroupItem() {
		return hasPositionalGroupItem;
	}

	public void setGroupByClauseExpressions(final List<Expression> lgroupByClauseExpressions) {
		this.groupByClauseExpressions = lgroupByClauseExpressions == null ? Collections.emptyList() : lgroupByClauseExpressions;
		if ( isRoot() ) {
			for ( int i = 0; i < groupByClauseExpressions.size(); i++ ) {
				final Expression groupItem = groupByClauseExpressions.get( i );
				if ( groupItem instanceof SqmAliasedNodeRef ) {
					hasPositionalGroupItem = true;
				}
			}
		}
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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expression

	@Override
	public JdbcMappingContainer getExpressionType() {
		if ( selectClause.getSqlSelections().size() == 1 ) {
			SqlSelection first = selectClause.getSqlSelections().get( 0 );
			return first.getExpressionType();
		}
		else {
			return null;
		}
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		TypeConfiguration typeConfiguration = creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration();
		for ( SqlSelection sqlSelection : selectClause.getSqlSelections() ) {
			sqlSelection.getExpressionType().forEachJdbcType(
					(index, jdbcMapping) -> {
						creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
								this,
								jdbcMapping.getJavaTypeDescriptor(),
								typeConfiguration
						);
					}
			);
		}
	}

	@Override
	public DomainResult createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		TypeConfiguration typeConfiguration = creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration();
		final SqlExpressionResolver sqlExpressionResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();
		if ( selectClause.getSqlSelections().size() == 1 ) {
			SqlSelection first = selectClause.getSqlSelections().get( 0 );
			JavaTypeDescriptor descriptor = first.getExpressionType()
					.getJdbcMappings()
					.get( 0 )
					.getJavaTypeDescriptor();

			final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
					this,
					descriptor,
					typeConfiguration
			);

			return new BasicResult<>(
					sqlSelection.getValuesArrayPosition(),
					resultVariable,
					descriptor
			);
		}
		else {
			throw new UnsupportedOperationException("Domain result for non-scalar subquery shouldn't be created!");
		}
	}
}
