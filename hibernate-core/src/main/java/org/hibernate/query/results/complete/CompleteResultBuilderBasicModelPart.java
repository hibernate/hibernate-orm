/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.complete;

import java.util.function.BiFunction;

import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.ResultsHelper.impl;

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
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationStateImpl = impl( domainResultCreationState );

		final TableGroup tableGroup = creationStateImpl.getFromClauseAccess().getTableGroup( navigablePath.getParent() );
		final TableReference tableReference = tableGroup.resolveTableReference( navigablePath, modelPart, modelPart.getContainingTableExpression() );

		final SqlSelection sqlSelection = creationStateImpl.resolveSqlSelection(
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

		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				columnAlias,
				modelPart.getJdbcMapping(),
				navigablePath,
				false,
				!sqlSelection.isVirtual()
		);
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		CompleteResultBuilderBasicModelPart that = (CompleteResultBuilderBasicModelPart) o;

		if ( !navigablePath.equals( that.navigablePath ) ) {
			return false;
		}
		if ( !modelPart.equals( that.modelPart ) ) {
			return false;
		}
		return columnAlias.equals( that.columnAlias );
	}

	@Override
	public int hashCode() {
		int result = navigablePath.hashCode();
		result = 31 * result + modelPart.hashCode();
		result = 31 * result + columnAlias.hashCode();
		return result;
	}
}
