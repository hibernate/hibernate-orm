/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.complete;

import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;
import org.hibernate.query.results.internal.ResultsHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.internal.ResultsHelper.impl;

/**
 * CompleteResultBuilder for basic-valued ModelParts
 *
 * @author Steve Ebersole
 */
public class CompleteResultBuilderBasicModelPart
		implements CompleteResultBuilderBasicValued, ModelPartReferenceBasic {
	private final NavigablePath navigablePath;
	private final BasicValuedModelPart modelPart;
	private final String columnAlias;

	public CompleteResultBuilderBasicModelPart(
			NavigablePath navigablePath,
			BasicValuedModelPart modelPart,
			String columnAlias) {
		this.navigablePath = navigablePath;
		this.modelPart = modelPart;
		this.columnAlias = columnAlias;
	}

	@Override
	public Class<?> getJavaType() {
		return modelPart.getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public BasicValuedModelPart getReferencedPart() {
		return modelPart;
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
		final DomainResultCreationStateImpl creationStateImpl = impl( domainResultCreationState );
		final TableReference tableReference = tableReference( creationStateImpl );
		final SqlSelection sqlSelection = sqlSelection( jdbcResultsMetadata, creationStateImpl, tableReference );
		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				columnAlias,
				modelPart.getJdbcMapping(),
				navigablePath,
				false,
				!sqlSelection.isVirtual()
		);
	}

	private SqlSelection sqlSelection(
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationStateImpl creationStateImpl,
			TableReference tableReference) {
		return creationStateImpl.resolveSqlSelection(
				ResultsHelper.resolveSqlExpression(
						creationStateImpl,
						jdbcResultsMetadata,
						tableReference,
						modelPart,
						columnAlias
				),
				modelPart.getJdbcMapping().getJdbcJavaType(),
				null,
				creationStateImpl.getSessionFactory().getTypeConfiguration()
		);
	}

	private TableReference tableReference(DomainResultCreationStateImpl creationStateImpl) {
		return creationStateImpl.getFromClauseAccess()
				.getTableGroup( navigablePath.getParent() )
				.resolveTableReference( navigablePath, modelPart, modelPart.getContainingTableExpression() );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final CompleteResultBuilderBasicModelPart that = (CompleteResultBuilderBasicModelPart) o;
		return navigablePath.equals( that.navigablePath )
				&& modelPart.equals( that.modelPart )
				&& columnAlias.equals( that.columnAlias );
	}

	@Override
	public int hashCode() {
		int result = navigablePath.hashCode();
		result = 31 * result + modelPart.hashCode();
		result = 31 * result + columnAlias.hashCode();
		return result;
	}
}
