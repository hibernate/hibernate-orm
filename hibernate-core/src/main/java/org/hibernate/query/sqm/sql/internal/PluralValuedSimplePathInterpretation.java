package org.hibernate.query.sqm.sql.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.update.Assignment;

/**
 * @author Andrea Boriero
 */
public class PluralValuedSimplePathInterpretation<T>
		implements AssignableSqmPathInterpretation<T>, DomainResultProducer<T> {

	public static SqmPathInterpretation<?> from(
			SqmPluralValuedSimplePath sqmPath,
			SqmToSqlAstConverter converter,
			SemanticQueryWalker sqmWalker) {
		final TableGroup tableGroup = converter.getFromClauseAccess().findTableGroup( sqmPath.getLhs()
																							  .getNavigablePath() );

		final PluralAttributeMapping mapping = (PluralAttributeMapping) tableGroup.getModelPart().findSubPart(
				sqmPath.getReferencedPathSource().getPathName(),
				null
		);


		return new PluralValuedSimplePathInterpretation<>(
				null,
				sqmPath,
				mapping,
				tableGroup
		);
	}

	private final Expression sqlExpression;

	private final SqmPluralValuedSimplePath<T> sqmPath;
	private final PluralAttributeMapping mapping;
	private final TableGroup tableGroup;

	private PluralValuedSimplePathInterpretation(
			Expression sqlExpression,
			SqmPluralValuedSimplePath sqmPath,
			PluralAttributeMapping mapping,
			TableGroup tableGroup) {
		this.sqlExpression = sqlExpression;

		assert sqmPath != null;
		this.sqmPath = sqmPath;

		assert mapping != null;
		this.mapping = mapping;

		assert tableGroup != null;
		this.tableGroup = tableGroup;
	}

	@Override
	public void applySqlAssignments(
			Expression newValueExpression,
			AssignmentContext assignmentProcessingState,
			Consumer<Assignment> assignmentConsumer,
			SqlAstCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return sqmPath.getNavigablePath();
	}

	@Override
	public ModelPart getExpressionType() {
		return mapping;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlExpression.accept( sqlTreeWalker );
	}

	@Override
	public List<ColumnReference> getColumnReferences() {
		final List<ColumnReference> results = new ArrayList<>();
		visitColumnReferences( results::add );
		return results;
	}

	@Override
	public void visitColumnReferences(Consumer<ColumnReference> columnReferenceConsumer) {
		if ( sqlExpression instanceof ColumnReference ) {
			columnReferenceConsumer.accept( (ColumnReference) sqlExpression );
		}
		else {
			throw new NotYetImplementedFor6Exception( getClass() );
		}
	}
}
