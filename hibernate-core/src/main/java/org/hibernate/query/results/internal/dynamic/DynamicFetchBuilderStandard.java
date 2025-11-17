/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.dynamic;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;
import org.hibernate.query.results.internal.ResultsHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class DynamicFetchBuilderStandard
		implements DynamicFetchBuilder, NativeQuery.ReturnProperty {

	private final Fetchable fetchable;
	private final List<String> columnNames;

	public DynamicFetchBuilderStandard(Fetchable fetchable) {
		this( fetchable, new ArrayList<>() );
	}

	private DynamicFetchBuilderStandard(Fetchable fetchable, List<String> columnNames) {
		this.fetchable = fetchable;
		this.columnNames = columnNames;
	}

	@Override
	public DynamicFetchBuilderStandard cacheKeyInstance() {
		return new DynamicFetchBuilderStandard(
				fetchable,
				List.copyOf( columnNames )
		);
	}

	public DynamicFetchBuilderStandard cacheKeyInstance(DynamicFetchBuilderContainer container) {
		return new DynamicFetchBuilderStandard(
				fetchable,
				List.copyOf( columnNames )
		);
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState domainResultCreationState) {
		final var creationStateImpl = ResultsHelper.impl( domainResultCreationState );
		final var ownerTableGroup =
				creationStateImpl.getFromClauseAccess().getTableGroup( parent.getNavigablePath() );

		final var sqlExpressionResolver =
				domainResultCreationState.getSqlAstCreationState().getSqlExpressionResolver();

		final var basicPart = fetchable.asBasicValuedModelPart();
		if ( basicPart != null ) {
			fetchable.forEachSelectable(
					getSelectableConsumer(
							fetchPath,
							jdbcResultsMetadata,
							domainResultCreationState,
							creationStateImpl,
							ownerTableGroup,
							sqlExpressionResolver,
							basicPart
					)
			);
			return parent.generateFetchableFetch(
					fetchable,
					fetchPath,
					FetchTiming.IMMEDIATE,
					true,
					null,
					creationStateImpl
			);
		}
		else if ( fetchable instanceof EmbeddableValuedFetchable embeddableValuedFetchable ) {
			fetchable.forEachSelectable(
					getSelectableConsumer(
							fetchPath,
							jdbcResultsMetadata,
							domainResultCreationState,
							creationStateImpl,
							ownerTableGroup,
							sqlExpressionResolver,
							embeddableValuedFetchable
					)
			);
			return parent.generateFetchableFetch(
					fetchable,
					fetchPath,
					FetchTiming.IMMEDIATE,
					false,
					null,
					creationStateImpl
			);
		}
		else if ( fetchable instanceof ToOneAttributeMapping toOneAttributeMapping ) {
			toOneAttributeMapping.getForeignKeyDescriptor()
					.getPart( toOneAttributeMapping.getSideNature() )
					.forEachSelectable(
							getSelectableConsumer(
									fetchPath,
									jdbcResultsMetadata,
									domainResultCreationState,
									creationStateImpl,
									ownerTableGroup,
									sqlExpressionResolver,
									toOneAttributeMapping.getForeignKeyDescriptor()
							)
					);
			return parent.generateFetchableFetch(
					fetchable,
					fetchPath,
					fetchable.getMappedFetchOptions().getTiming(),
					false,
					null,
					creationStateImpl
			);
		}
		else if ( fetchable instanceof PluralAttributeMapping pluralAttributeMapping ) {
			pluralAttributeMapping.getKeyDescriptor().visitTargetSelectables(
					getSelectableConsumer(
							fetchPath,
							jdbcResultsMetadata,
							domainResultCreationState,
							creationStateImpl,
							ownerTableGroup,
							sqlExpressionResolver,
							pluralAttributeMapping.getKeyDescriptor()
					)
			);
			return parent.generateFetchableFetch(
					fetchable,
					fetchPath,
					fetchable.getMappedFetchOptions().getTiming(),
					false,
					null,
					creationStateImpl
			);
		}
		else {
			throw new AssertionFailure( "Unexpected attribute mapping" );
		}
	}

	private SelectableConsumer getSelectableConsumer(
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState domainResultCreationState,
			DomainResultCreationStateImpl creationStateImpl,
			TableGroup ownerTableGroup,
			SqlExpressionResolver sqlExpressionResolver,
			ValuedModelPart valuedModelPart) {
		return (selectionIndex, selectableMapping) -> {
			final var tableReference = ownerTableGroup.resolveTableReference(
					fetchPath,
					valuedModelPart,
					selectableMapping.getContainingTableExpression()
			);
			final String columnAlias = columnNames.get( selectionIndex );
			sqlExpressionResolver.resolveSqlSelection(
					ResultsHelper.resolveSqlExpression(
							creationStateImpl,
							jdbcResultsMetadata,
							tableReference,
							selectableMapping,
							columnAlias
					),
					selectableMapping.getJdbcMapping().getJdbcJavaType(),
					null,
					domainResultCreationState.getSqlAstCreationState().getCreationContext().getTypeConfiguration()
			);
		};
	}

	@Override
	public NativeQuery.ReturnProperty addColumnAlias(String columnAlias) {
		columnNames.add( columnAlias );
		return this;
	}

	@Override
	public List<String> getColumnAliases() {
		return columnNames;
	}

	@Override
	public int hashCode() {
		int result = fetchable.hashCode();
		result = 31 * result + columnNames.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final var that = (DynamicFetchBuilderStandard) o;
		return fetchable.equals( that.fetchable )
			&& columnNames.equals( that.columnNames );
	}
}
