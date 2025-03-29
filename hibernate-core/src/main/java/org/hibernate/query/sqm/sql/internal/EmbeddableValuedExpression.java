/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.update.Assignable;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableExpressionResultImpl;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A computed expression that produces an embeddable valued model part.
 * It may only be composed of basic attribute mappings.
 */
public class EmbeddableValuedExpression<T> implements Expression, DomainResultProducer<T>, Assignable, SqlTupleContainer {

	private final NavigablePath navigablePath;
	private final EmbeddableValuedModelPart mapping;
	private final SqlTuple sqlExpression;

	public EmbeddableValuedExpression(
			NavigablePath baseNavigablePath,
			EmbeddableValuedModelPart mapping,
			SqlTuple sqlExpression) {
		assert mapping != null;
		assert sqlExpression != null;
		assert mapping.getEmbeddableTypeDescriptor().getNumberOfAttributeMappings() == sqlExpression.getExpressions().size();
		this.navigablePath = baseNavigablePath.append( mapping.getPartName(), SqmCreationHelper.acquireUniqueAlias());
		this.mapping = mapping;
		this.sqlExpression = sqlExpression;
	}

	@Override
	public ModelPart getExpressionType() {
		return mapping;
	}

	@Override
	public DomainResult<T> createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		return new EmbeddableExpressionResultImpl<>(
				navigablePath,
				mapping,
				sqlExpression,
				resultVariable,
				creationState
		);
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		final EmbeddableMappingType mappingType = mapping.getEmbeddableTypeDescriptor();
		final int numberOfAttributeMappings = mappingType.getNumberOfAttributeMappings();
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final TypeConfiguration typeConfiguration = sqlAstCreationState.getCreationContext().getTypeConfiguration();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
			final AttributeMapping attributeMapping = mappingType.getAttributeMapping( i );
			assert attributeMapping instanceof BasicAttributeMapping;
			sqlExpressionResolver.resolveSqlSelection(
					sqlExpression.getExpressions().get( i ),
					attributeMapping.getJavaType(),
					null,
					typeConfiguration
			);
		}
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

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlExpression.accept( sqlTreeWalker );
	}
}
