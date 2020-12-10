/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.select;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.cte.CteConsumer;
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
public class QuerySpec implements SqlAstNode, PredicateContainer, Expression, CteConsumer, DomainResultProducer {
	private final boolean isRoot;

	private final FromClause fromClause;
	private final SelectClause selectClause = new SelectClause();

	private Predicate whereClauseRestrictions;

	private List<Expression> groupByClauseExpressions = Collections.emptyList();
	private Predicate havingClauseRestrictions;

	private List<SortSpecification> sortSpecifications;
	private Expression limitClauseExpression;
	private Expression offsetClauseExpression;

	public QuerySpec(boolean isRoot) {
		this.isRoot = isRoot;
		this.fromClause = new FromClause();
	}

	public QuerySpec(boolean isRoot, int expectedNumberOfRoots) {
		this.isRoot = isRoot;
		this.fromClause = new FromClause( expectedNumberOfRoots );
	}

	/**
	 * Does this QuerySpec map to the statement's root query (as
	 * opposed to one of its sub-queries)?
	 */
	public boolean isRoot() {
		return isRoot;
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

	public void setGroupByClauseExpressions(List<Expression> groupByClauseExpressions) {
		this.groupByClauseExpressions = groupByClauseExpressions == null ? Collections.emptyList() : groupByClauseExpressions;
	}

	public Predicate getHavingClauseRestrictions() {
		return havingClauseRestrictions;
	}

	public void setHavingClauseRestrictions(Predicate havingClauseRestrictions) {
		this.havingClauseRestrictions = havingClauseRestrictions;
	}

	public List<SortSpecification> getSortSpecifications() {
		return sortSpecifications;
	}

	void visitSortSpecifications(Consumer<SortSpecification> consumer) {
		if ( sortSpecifications != null ) {
			sortSpecifications.forEach( consumer );
		}
	}

	public void addSortSpecification(SortSpecification specification) {
		if ( sortSpecifications == null ) {
			sortSpecifications = new ArrayList<>();
		}
		sortSpecifications.add( specification );
	}

	public Expression getLimitClauseExpression() {
		return limitClauseExpression;
	}

	public void setLimitClauseExpression(Expression limitClauseExpression) {
		this.limitClauseExpression = limitClauseExpression;
	}

	public Expression getOffsetClauseExpression() {
		return offsetClauseExpression;
	}

	public void setOffsetClauseExpression(Expression offsetClauseExpression) {
		this.offsetClauseExpression = offsetClauseExpression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitQuerySpec( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expression

	@Override
	public MappingModelExpressable getExpressionType() {
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
