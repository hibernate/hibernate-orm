/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.update.Assignable;

/**
 * @author Steve Ebersole
 */
public class EmbeddableValuedPathInterpretation<T> extends AbstractSqmPathInterpretation<T> implements Assignable, SqlTupleContainer {

	/**
	 * Static factory
	 */
	public static <T> EmbeddableValuedPathInterpretation<T> from(
			SqmEmbeddedValuedSimplePath<T> sqmPath,
			SqmToSqlAstConverter converter,
			SemanticQueryWalker sqmWalker) {
		TableGroup tableGroup = converter.getFromClauseAccess()
				.findTableGroup( sqmPath.getLhs().getNavigablePath() );

		final EmbeddableValuedModelPart mapping = (EmbeddableValuedModelPart) tableGroup.getModelPart().findSubPart(
				sqmPath.getReferencedPathSource().getPathName(),
				null
		);

		return new EmbeddableValuedPathInterpretation<>(
				mapping.toSqlExpression(
						tableGroup,
						converter.getCurrentClauseStack().getCurrent(),
						converter,
						converter
				),
				sqmPath,
				mapping,
				tableGroup
		);
	}

	private final SqlTuple sqlExpression;

	public EmbeddableValuedPathInterpretation(
			SqlTuple sqlExpression,
			SqmEmbeddedValuedSimplePath<T> sqmPath,
			EmbeddableValuedModelPart mapping,
			TableGroup tableGroup) {
		super( sqmPath, mapping, tableGroup );
		this.sqlExpression = sqlExpression;
	}

	public SqlTuple getSqlExpression() {
		return sqlExpression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlExpression.accept( sqlTreeWalker );
	}

	@Override
	public String toString() {
		return "EmbeddableValuedPathInterpretation(" + getNavigablePath().getFullPath() + ')';
	}

	@Override
	public void visitColumnReferences(Consumer<ColumnReference> columnReferenceConsumer) {
		for ( Expression expression : sqlExpression.getExpressions() ) {
			if ( !( expression instanceof ColumnReference ) ) {
				throw new IllegalArgumentException( "Expecting ColumnReference, found : " + expression );
			}
			columnReferenceConsumer.accept( (ColumnReference) expression );
		}
	}

	@Override
	public List<ColumnReference> getColumnReferences() {
		final List<ColumnReference> results = new ArrayList<>();
		visitColumnReferences( results::add );
		return results;
	}

	@Override
	public SqlTuple getSqlTuple() {
		return sqlExpression;
	}
}
