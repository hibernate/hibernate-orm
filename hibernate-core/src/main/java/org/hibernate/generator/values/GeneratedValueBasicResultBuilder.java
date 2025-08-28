/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator.values;

import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;
import org.hibernate.query.results.internal.ResultsHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.generator.values.internal.GeneratedValuesHelper.getActualGeneratedModelPart;
import static org.hibernate.query.results.internal.ResultsHelper.impl;
import static org.hibernate.query.results.internal.ResultsHelper.jdbcPositionToValuesArrayPosition;

/**
 * Simple implementation of {@link ResultBuilder} for retrieving generated basic values.
 *
 * @author Marco Belladelli
 * @see GeneratedValuesMutationDelegate
 */
public class GeneratedValueBasicResultBuilder implements ResultBuilder {
	private final NavigablePath navigablePath;
	private final BasicValuedModelPart modelPart;
	private final Integer valuesArrayPosition;
	private final TableGroup tableGroup;

	public GeneratedValueBasicResultBuilder(
			NavigablePath navigablePath,
			BasicValuedModelPart modelPart,
			TableGroup tableGroup,
			Integer valuesArrayPosition) {
		this.navigablePath = navigablePath;
		this.modelPart = modelPart;
		this.valuesArrayPosition = valuesArrayPosition;
		this.tableGroup = tableGroup;
	}

	@Override
	public Class<?> getJavaType() {
		return modelPart.getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	public ResultBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public BasicResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState) {
		return new BasicResult<>(
				sqlSelection( jdbcResultsMetadata, domainResultCreationState )
						.getValuesArrayPosition(),
				null,
				modelPart.getJdbcMapping(),
				navigablePath,
				false,
				false
		);
	}

	private SqlSelection sqlSelection(
			JdbcValuesMetadata jdbcResultsMetadata, DomainResultCreationState domainResultCreationState) {
		final var creationStateImpl = impl( domainResultCreationState );
		return sqlSelection( jdbcResultsMetadata, creationStateImpl, tableReference( creationStateImpl ) );
	}

	private TableReference tableReference(DomainResultCreationStateImpl creationStateImpl) {
		return creationStateImpl.getFromClauseAccess()
				.resolveTableGroup( navigablePath.getParent(), path -> this.tableGroup )
				.resolveTableReference( navigablePath, modelPart, "t" );
	}

	private SqlSelection sqlSelection(
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationStateImpl creationStateImpl,
			TableReference tableReference) {
		return creationStateImpl.resolveSqlSelection(
				ResultsHelper.resolveSqlExpression(
						creationStateImpl,
						tableReference,
						modelPart,
						valuesArrayPosition != null
								? valuesArrayPosition
								: columnIndex( jdbcResultsMetadata, modelPart )
				),
				modelPart.getJdbcMapping().getJdbcJavaType(),
				null,
				creationStateImpl.getSessionFactory().getTypeConfiguration()
		);
	}

	public BasicValuedModelPart getModelPart() {
		return modelPart;
	}

	private static int columnIndex(JdbcValuesMetadata jdbcResultsMetadata, BasicValuedModelPart modelPart) {
		if ( jdbcResultsMetadata.getColumnCount() == 1 ) {
			assert modelPart.isEntityIdentifierMapping()
				|| getColumnPosition( jdbcResultsMetadata, modelPart ) == 1;
			return 0;
		}
		else {
			return jdbcPositionToValuesArrayPosition( getColumnPosition( jdbcResultsMetadata, modelPart ) );
		}
	}

	private static int getColumnPosition(JdbcValuesMetadata valuesMetadata, BasicValuedModelPart modelPart) {
		return valuesMetadata.resolveColumnPosition( getActualGeneratedModelPart( modelPart ).getSelectionExpression() );
	}
}
