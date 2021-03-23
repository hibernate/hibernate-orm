/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.update.Assignable;

/**
 * @author Steve Ebersole
 */
public class BasicValuedPathInterpretation<T> extends AbstractSqmPathInterpretation<T> implements Assignable,  DomainResultProducer<T> {
	/**
	 * Static factory
	 */
	public static <T> BasicValuedPathInterpretation<T> from(
			SqmBasicValuedSimplePath<T> sqmPath,
			SqlAstCreationState sqlAstCreationState,
			SemanticQueryWalker sqmWalker) {
		TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup( sqmPath.getLhs().getNavigablePath() );

		final BasicValuedModelPart mapping = (BasicValuedModelPart) tableGroup.getModelPart().findSubPart(
				sqmPath.getReferencedPathSource().getPathName(),
				null
		);

		if ( mapping == null ) {
			throw new SemanticException( "`" + sqmPath.getNavigablePath().getFullPath() + "` did not reference a known model part" );
		}

		final TableReference tableReference = tableGroup.resolveTableReference( mapping.getContainingTableExpression() );

		final Expression expression = sqlAstCreationState.getSqlExpressionResolver().resolveSqlExpression(
				SqlExpressionResolver.createColumnReferenceKey(
						tableReference,
						mapping.getSelectionExpression()
				),
				sacs -> new ColumnReference(
						tableReference.getIdentificationVariable(),
						mapping,
						sqlAstCreationState.getCreationContext().getSessionFactory()
				)
		);

		final ColumnReference columnReference;
		if ( expression instanceof ColumnReference ) {
			columnReference = ( (ColumnReference) expression );
		}
		else if ( expression instanceof SqlSelectionExpression ) {
			final Expression selectedExpression = ( (SqlSelectionExpression) expression ).getSelection().getExpression();
			assert selectedExpression instanceof ColumnReference;
			columnReference = (ColumnReference) selectedExpression;
		}
		else {
			throw new UnsupportedOperationException( "Unsupported basic-valued path expression : " + expression );
		}

		return new BasicValuedPathInterpretation<>( columnReference, sqmPath, mapping, tableGroup );
	}

	private final ColumnReference columnReference;

	private BasicValuedPathInterpretation(
			ColumnReference columnReference,
			SqmBasicValuedSimplePath<T> sqmPath,
			BasicValuedModelPart mapping,
			TableGroup tableGroup) {
		super(sqmPath,mapping,tableGroup);
		assert columnReference != null;
		this.columnReference = columnReference;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainResultProducer

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		columnReference.accept( sqlTreeWalker );
	}

	@Override
	public String toString() {
		return "BasicValuedPathInterpretation(" + getNavigablePath().getFullPath() + ')';
	}

	@Override
	public void visitColumnReferences(Consumer<ColumnReference> columnReferenceConsumer) {
		columnReferenceConsumer.accept( columnReference );
	}

	@Override
	public List<ColumnReference> getColumnReferences() {
		return Collections.singletonList( columnReference );
	}
}
