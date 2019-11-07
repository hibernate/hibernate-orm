/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.expression;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.predicate.CasePredicate;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.domain.basic.BasicResult;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class CaseSearchedExpression implements Expression, DomainResultProducer {
	private final BasicType type;

	private List<WhenFragment> whenFragments = new ArrayList<>();
	private Expression otherwise;
	private String columnExpression;

	public CaseSearchedExpression(MappingModelExpressable type) {
		this.type = (BasicType) type;
	}

	public void setColumnExpression(String columnExpression) {
		this.columnExpression = columnExpression;
	}

	public String getColumnExpression(){
		return columnExpression;
	}

	public List<WhenFragment> getWhenFragments() {
		return whenFragments;
	}

	public Expression getOtherwise() {
		return otherwise;
	}

	public void when(CasePredicate predicate, Expression result) {
		whenFragments.add( new WhenFragment( predicate, result ) );
	}

	public void otherwise(Expression otherwiseExpression) {
		this.otherwise = otherwiseExpression;
		// todo : inject implied type?
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {

		final SqlSelection sqlSelection = creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
				this,
				type.getExpressableJavaTypeDescriptor(),
				creationState.getSqlAstCreationState()
						.getCreationContext()
						.getSessionFactory()
						.getTypeConfiguration()
		);

		//noinspection unchecked
		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				type.getExpressableJavaTypeDescriptor()
		);
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			JavaTypeDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return new SqlSelectionImpl(
				jdbcPosition,
				valuesArrayPosition,
				this,
				type.getJdbcMapping()
		);
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitCaseSearchedExpression( this );
	}

	@Override
	public MappingModelExpressable getExpressionType() {
		return type;
	}

	public static class WhenFragment {
		private final CasePredicate predicate;
		private final Expression result;

		public WhenFragment(CasePredicate predicate, Expression result) {
			this.predicate = predicate;
			this.result = result;
		}

		public CasePredicate getPredicate() {
			return predicate;
		}

		public Expression getResult() {
			return result;
		}
	}
}
