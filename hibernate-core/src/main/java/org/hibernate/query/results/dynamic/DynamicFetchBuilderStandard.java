/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.dynamic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.NativeQuery;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class DynamicFetchBuilderStandard
		implements DynamicFetchBuilder, NativeQuery.ReturnProperty {

	private final String fetchableName;
	private final List<String> columnNames;

	public DynamicFetchBuilderStandard(String fetchableName) {
		this.fetchableName = fetchableName;
		this.columnNames = new ArrayList<>();
	}

	private DynamicFetchBuilderStandard(String fetchableName, List<String> columnNames) {
		this.fetchableName = fetchableName;
		this.columnNames = columnNames;
	}

	@Override
	public DynamicFetchBuilderStandard cacheKeyInstance() {
		return new DynamicFetchBuilderStandard(
				fetchableName,
				List.copyOf( columnNames )
		);
	}

	public DynamicFetchBuilderStandard cacheKeyInstance(DynamicFetchBuilderContainer container) {
		return new DynamicFetchBuilderStandard(
				fetchableName,
				List.copyOf( columnNames )
		);
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationStateImpl = ResultsHelper.impl( domainResultCreationState );

		final TableGroup ownerTableGroup = creationStateImpl.getFromClauseAccess().getTableGroup( parent.getNavigablePath() );

		final Fetchable attributeMapping = (Fetchable) parent.getReferencedMappingContainer().findSubPart( fetchableName, null );
		final SqlExpressionResolver sqlExpressionResolver = domainResultCreationState.getSqlAstCreationState().getSqlExpressionResolver();

		final BasicValuedModelPart basicPart = attributeMapping.asBasicValuedModelPart();
		if ( basicPart != null ) {
			attributeMapping.forEachSelectable(
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
					attributeMapping,
					fetchPath,
					FetchTiming.IMMEDIATE,
					true,
					null,
					creationStateImpl
			);
		}
		else if ( attributeMapping instanceof EmbeddableValuedFetchable ) {
			attributeMapping.forEachSelectable(
					getSelectableConsumer(
							fetchPath,
							jdbcResultsMetadata,
							domainResultCreationState,
							creationStateImpl,
							ownerTableGroup,
							sqlExpressionResolver,
							(EmbeddableValuedFetchable) attributeMapping
					)
			);
			return parent.generateFetchableFetch(
					attributeMapping,
					fetchPath,
					FetchTiming.IMMEDIATE,
					false,
					null,
					creationStateImpl
			);
		}
		else if ( attributeMapping instanceof ToOneAttributeMapping ) {
			final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) attributeMapping;
			toOneAttributeMapping.getForeignKeyDescriptor().getPart( toOneAttributeMapping.getSideNature() )
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
					attributeMapping,
					fetchPath,
					attributeMapping.getMappedFetchOptions().getTiming(),
					false,
					null,
					creationStateImpl
			);
		}
		else {
			assert attributeMapping instanceof PluralAttributeMapping;
			final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) attributeMapping;
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
					attributeMapping,
					fetchPath,
					attributeMapping.getMappedFetchOptions().getTiming(),
					false,
					null,
					creationStateImpl
			);
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
			final TableReference tableReference = ownerTableGroup.resolveTableReference(
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
					domainResultCreationState.getSqlAstCreationState()
							.getCreationContext()
							.getSessionFactory()
							.getTypeConfiguration()
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
		int result = fetchableName.hashCode();
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

		final DynamicFetchBuilderStandard that = (DynamicFetchBuilderStandard) o;
		return fetchableName.equals( that.fetchableName )
				&& columnNames.equals( that.columnNames );
	}
}
