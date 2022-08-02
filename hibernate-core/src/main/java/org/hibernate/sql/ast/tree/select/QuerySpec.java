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
import java.util.function.Function;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
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
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class QuerySpec extends QueryPart implements SqlAstNode, PredicateContainer, Expression, DomainResultProducer {

	private final FromClause fromClause;
	private final SelectClause selectClause;

	private Predicate whereClauseRestrictions;

	private List<Expression> groupByClauseExpressions = Collections.emptyList();
	private Predicate havingClauseRestrictions;

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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expression

	@Override
	public JdbcMappingContainer getExpressionType() {
		final List<SqlSelection> sqlSelections = selectClause.getSqlSelections();
		switch ( sqlSelections.size() ) {
			case 1:
				return sqlSelections.get( 0 ).getExpressionType();
			default:
				// todo (6.0): At some point we should create an ArrayTupleType and return that
			case 0:
				return null;
		}
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		TypeConfiguration typeConfiguration = creationState.getSqlAstCreationState().getCreationContext().getMappingMetamodel().getTypeConfiguration();
		for ( SqlSelection sqlSelection : selectClause.getSqlSelections() ) {
			sqlSelection.getExpressionType().forEachJdbcType(
					(index, jdbcMapping) -> {
						creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
								this,
								jdbcMapping.getJdbcJavaType(),
								null,
								typeConfiguration
						);
					}
			);
		}
	}

	@Override
	public DomainResult createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		final TypeConfiguration typeConfiguration = creationState.getSqlAstCreationState()
				.getCreationContext()
				.getMappingMetamodel()
				.getTypeConfiguration();
		final SqlExpressionResolver sqlExpressionResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();
		if ( selectClause.getSqlSelections().size() == 1 ) {
			final SqlSelection first = selectClause.getSqlSelections().get( 0 );
			final JdbcMapping jdbcMapping = first.getExpressionType()
					.getJdbcMappings()
					.get( 0 );

			final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
					this,
					jdbcMapping.getJdbcJavaType(),
					null,
					typeConfiguration
			);

			return new BasicResult<>(
					sqlSelection.getValuesArrayPosition(),
					resultVariable,
					jdbcMapping
			);
		}
		else {
			throw new UnsupportedOperationException("Domain result for non-scalar subquery shouldn't be created");
		}
	}
}
