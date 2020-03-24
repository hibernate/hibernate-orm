/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.select;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.internal.FilterHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.cte.CteConsumer;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.predicate.FilterPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.PredicateContainer;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
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
	private List<FilterPredicate> filterPredicates;
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

	public void addFilterPredicate(FilterPredicate filterPredicate) {
		if ( filterPredicates == null ) {
			filterPredicates = new ArrayList<>();
		}
		filterPredicates.add( filterPredicate );
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
		SqlSelection first = selectClause.getSqlSelections().get(0);
		return first.getExpressionType();
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		SqlSelection first = selectClause.getSqlSelections().get(0);
		TypeConfiguration typeConfiguration = creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration();
		JavaTypeDescriptor descriptor = first.getExpressionType().getJdbcMappings( typeConfiguration ).get(0).getJavaTypeDescriptor();
		creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
				this,
				descriptor,
				typeConfiguration
		);
	}

	@Override
	public DomainResult createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		SqlSelection first = selectClause.getSqlSelections().get(0);
		TypeConfiguration typeConfiguration = creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration();
		JavaTypeDescriptor descriptor = first.getExpressionType().getJdbcMappings( typeConfiguration ).get(0).getJavaTypeDescriptor();

		final SqlExpressionResolver sqlExpressionResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();
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

	public void bindFilterPredicateParameters(JdbcParameterBindings jdbcParameterBindings) {
		if ( filterPredicates != null && !filterPredicates.isEmpty() ) {
			for ( FilterPredicate filterPredicate : filterPredicates ) {
				for ( int i = 0; i < filterPredicate.getJdbcParameters().size(); i++ ) {
					final JdbcParameter parameter = filterPredicate.getJdbcParameters().get( i );
					final FilterHelper.TypedValue parameterTypedValue = filterPredicate.getJdbcParameterTypedValues().get( i );
					if ( !(parameterTypedValue.getType() instanceof JdbcMapping ) ) {
						throw new HibernateException( String.format( "Filter parameter type [%s] did not implement JdbcMapping", parameterTypedValue.getType() ) );
					}
					jdbcParameterBindings.addBinding(
							parameter,
							new JdbcParameterBinding() {
								@Override
								public JdbcMapping getBindType() {
									return (JdbcMapping) parameterTypedValue.getType();
								}

								@Override
								public Object getBindValue() {
									return parameterTypedValue.getValue();
								}
							}
					);
				}
			}
		}
	}
}
