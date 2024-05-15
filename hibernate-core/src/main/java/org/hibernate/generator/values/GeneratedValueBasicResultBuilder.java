/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.generator.values;

import java.util.function.BiFunction;

import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.generator.values.internal.GeneratedValuesHelper.getActualGeneratedModelPart;
import static org.hibernate.query.results.ResultsHelper.impl;
import static org.hibernate.query.results.ResultsHelper.jdbcPositionToValuesArrayPosition;

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
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationStateImpl = impl( domainResultCreationState );

		final TableGroup tableGroup = creationStateImpl.getFromClauseAccess().resolveTableGroup(
				navigablePath.getParent(),
				path -> this.tableGroup
		);
		final TableReference tableReference = tableGroup.resolveTableReference(
				navigablePath,
				modelPart,
				"t"
		);

		final int position = valuesArrayPosition != null ?
				valuesArrayPosition :
				columnIndex( jdbcResultsMetadata, modelPart );
		final SqlSelection sqlSelection = creationStateImpl.resolveSqlSelection(
				ResultsHelper.resolveSqlExpression(
						creationStateImpl,
						tableReference,
						modelPart,
						position
				),
				modelPart.getJdbcMapping().getJdbcJavaType(),
				null,
				creationStateImpl.getSessionFactory().getTypeConfiguration()
		);

		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				null,
				modelPart.getJdbcMapping(),
				navigablePath,
				false,
				false
		);
	}

	public BasicValuedModelPart getModelPart() {
		return modelPart;
	}

	private static int columnIndex(JdbcValuesMetadata jdbcResultsMetadata, BasicValuedModelPart modelPart) {
		if ( jdbcResultsMetadata.getColumnCount() == 1 ) {
			assert modelPart.isEntityIdentifierMapping() || jdbcResultsMetadata.resolveColumnPosition(
					getActualGeneratedModelPart( modelPart ).getSelectionExpression()
			) == 1;
			return 0;
		}
		else {
			return jdbcPositionToValuesArrayPosition( jdbcResultsMetadata.resolveColumnPosition(
					getActualGeneratedModelPart( modelPart ).getSelectionExpression()
			) );
		}
	}
}
