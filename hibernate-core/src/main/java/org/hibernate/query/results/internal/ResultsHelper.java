/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal;

import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;
import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createDiscriminatorColumnReferenceKey;

/**
 * @author Steve Ebersole
 */
public class ResultsHelper {
	public static int jdbcPositionToValuesArrayPosition(int jdbcPosition) {
		return jdbcPosition - 1;
	}

	public static int valuesArrayPositionToJdbcPosition(int valuesArrayPosition) {
		return valuesArrayPosition + 1;
	}

	public static DomainResultCreationStateImpl impl(DomainResultCreationState creationState) {
		return unwrap( creationState );
	}

	private static DomainResultCreationStateImpl unwrap(DomainResultCreationState creationState) {
		if ( creationState instanceof DomainResultCreationStateImpl domainResultCreationState ) {
			return domainResultCreationState;
		}
		else {
			throw new IllegalArgumentException(
					"Passed DomainResultCreationState not an instance of org.hibernate.query.results.internal.DomainResultCreationStateImpl"
			);
		}
	}

	public static Expression resolveSqlExpression(
			DomainResultCreationStateImpl resolver,
			JdbcValuesMetadata jdbcValuesMetadata,
			TableReference tableReference,
			SelectableMapping selectableMapping,
			String columnAlias) {
		return resolver.resolveSqlExpression(
				createColumnReferenceKey(
						tableReference,
						selectableMapping
				),
				processingState -> {
					final int jdbcPosition = jdbcValuesMetadata.resolveColumnPosition( columnAlias );
					final int valuesArrayPosition = jdbcPositionToValuesArrayPosition( jdbcPosition );
					return new ResultSetMappingSqlSelection( valuesArrayPosition, selectableMapping.getJdbcMapping() );
				}
		);
	}

	public static Expression resolveSqlExpression(
			DomainResultCreationStateImpl resolver,
			JdbcValuesMetadata jdbcValuesMetadata,
			TableReference tableReference,
			EntityDiscriminatorMapping discriminatorMapping,
			String columnAlias) {
		return resolver.resolveSqlExpression(
				createDiscriminatorColumnReferenceKey(
						tableReference,
						discriminatorMapping
				),
				processingState -> {
					final int jdbcPosition = jdbcValuesMetadata.resolveColumnPosition( columnAlias );
					final int valuesArrayPosition = jdbcPositionToValuesArrayPosition( jdbcPosition );
					return new ResultSetMappingSqlSelection( valuesArrayPosition, discriminatorMapping.getJdbcMapping() );
				}
		);
	}

	public static Expression resolveSqlExpression(
			DomainResultCreationStateImpl resolver,
			TableReference tableReference,
			SelectableMapping selectableMapping,
			int valuesArrayPosition) {
		return resolver.resolveSqlExpression(
				createColumnReferenceKey(
						tableReference,
						selectableMapping.getSelectablePath(),
						selectableMapping.getJdbcMapping()
				),
				processingState -> new ResultSetMappingSqlSelection(
						valuesArrayPosition,
						selectableMapping.getJdbcMapping()
				)
		);
	}

	private ResultsHelper() {
	}

	public static String attributeName(ModelPart identifierMapping) {
		if ( identifierMapping.isEntityIdentifierMapping() ) {
			return identifierMapping instanceof SingleAttributeIdentifierMapping singleAttributeIdentifierMapping
					? singleAttributeIdentifierMapping.getAttributeName()
					: null;
		}
		else {
			return identifierMapping.getPartName();
		}

	}
}
