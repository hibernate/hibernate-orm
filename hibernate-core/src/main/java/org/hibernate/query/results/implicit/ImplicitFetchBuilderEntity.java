/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.implicit;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.results.ResultSetMappingSqlSelection;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.results.Builders;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.ResultsHelper.impl;
import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Steve Ebersole
 */
public class ImplicitFetchBuilderEntity implements ImplicitFetchBuilder {
	private final NavigablePath fetchPath;
	private final ToOneAttributeMapping fetchable;
	private final Map<NavigablePath, FetchBuilder> fetchBuilders;

	public ImplicitFetchBuilderEntity(
			NavigablePath fetchPath,
			ToOneAttributeMapping fetchable,
			DomainResultCreationState creationState) {
		this.fetchPath = fetchPath;
		this.fetchable = fetchable;
		final DomainResultCreationStateImpl creationStateImpl = impl( creationState );
		final NavigablePath relativePath = creationStateImpl.getCurrentRelativePath();
		final Function<String, FetchBuilder> fetchBuilderResolver = creationStateImpl.getCurrentExplicitFetchMementoResolver();
		final ForeignKeyDescriptor foreignKeyDescriptor = fetchable.getForeignKeyDescriptor();
		final String associationKeyPropertyName;
		if ( fetchable.getReferencedPropertyName() == null ) {
			associationKeyPropertyName = fetchable.getEntityMappingType().getIdentifierMapping().getPartName();
		}
		else {
			associationKeyPropertyName = fetchable.getReferencedPropertyName();
		}
		final NavigablePath associationKeyFetchPath = relativePath.append( associationKeyPropertyName );
		final FetchBuilder explicitAssociationKeyFetchBuilder = fetchBuilderResolver
				.apply( associationKeyFetchPath.getFullPath() );
		final Map<NavigablePath, FetchBuilder> fetchBuilders;
		if ( explicitAssociationKeyFetchBuilder == null ) {
			final MappingType partMappingType = foreignKeyDescriptor.getPartMappingType();
			if ( partMappingType instanceof EmbeddableMappingType ) {
				final EmbeddableMappingType embeddableValuedModelPart = (EmbeddableMappingType) partMappingType;
				fetchBuilders = new LinkedHashMap<>( embeddableValuedModelPart.getNumberOfFetchables() );
				embeddableValuedModelPart.visitFetchables(
						subFetchable -> {
							final NavigablePath subFetchPath = associationKeyFetchPath.append( subFetchable.getFetchableName() );
							final FetchBuilder explicitFetchBuilder = fetchBuilderResolver
									.apply( subFetchPath.getFullPath() );
							if ( explicitFetchBuilder == null ) {
								fetchBuilders.put(
										subFetchPath,
										Builders.implicitFetchBuilder( fetchPath, subFetchable, creationStateImpl )
								);
							}
							else {
								fetchBuilders.put( subFetchPath, explicitFetchBuilder );
							}
						},
						null
				);
			}
			else {
				fetchBuilders = Collections.emptyMap();
			}
		}
		else {
			fetchBuilders = Collections.singletonMap( associationKeyFetchPath, explicitAssociationKeyFetchBuilder );
		}
		this.fetchBuilders = fetchBuilders;
	}

	private ImplicitFetchBuilderEntity(ImplicitFetchBuilderEntity original) {
		this.fetchPath = original.fetchPath;
		this.fetchable = original.fetchable;
		final Map<NavigablePath, FetchBuilder> fetchBuilders;
		if ( original.fetchBuilders.isEmpty() ) {
			fetchBuilders = Collections.emptyMap();
		}
		else {
			fetchBuilders = new HashMap<>( original.fetchBuilders.size() );
			for ( Map.Entry<NavigablePath, FetchBuilder> entry : original.fetchBuilders.entrySet() ) {
				fetchBuilders.put( entry.getKey(), entry.getValue().cacheKeyInstance() );
			}
		}
		this.fetchBuilders = fetchBuilders;
	}

	@Override
	public FetchBuilder cacheKeyInstance() {
		return new ImplicitFetchBuilderEntity( this );
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState creationState) {
		if ( fetchBuilders.isEmpty() ) {
			// If no fetch builders exist, we must ensure we resolve selections for the FK ourselves
			// otherwise we might run into duplicate alias issues on certain queries,
			// because selections that are created in DomainResultCreationStateImpl don't consider the table name
			final DomainResultCreationStateImpl creationStateImpl = ResultsHelper.impl( creationState );
			final TableGroup ownerTableGroup = creationStateImpl.getFromClauseAccess().getTableGroup( parent.getNavigablePath() );
			final SqlExpressionResolver sqlExpressionResolver = creationStateImpl.getSqlAstCreationState().getSqlExpressionResolver();
			fetchable.getForeignKeyDescriptor().getSide( fetchable.getSideNature() ).getModelPart().forEachSelectable(
					(selectionIndex, selectableMapping) -> {
						final TableReference tableReference = ownerTableGroup.resolveTableReference(
								fetchPath,
								selectableMapping.getContainingTableExpression()
						);
						sqlExpressionResolver.resolveSqlSelection(
								sqlExpressionResolver.resolveSqlExpression(
										createColumnReferenceKey( tableReference, selectableMapping.getSelectionExpression() ),
										state -> {
											final int resultSetPosition = jdbcResultsMetadata.resolveColumnPosition(
													selectableMapping.getSelectionExpression(),
													selectableMapping.getContainingTableExpression()
											);
											final int valuesArrayPosition = resultSetPosition - 1;
											return new ResultSetMappingSqlSelection( valuesArrayPosition, selectableMapping.getJdbcMapping() );
										}
								),
								selectableMapping.getJdbcMapping().getMappedJavaType(),
								creationState.getSqlAstCreationState()
										.getCreationContext()
										.getSessionFactory()
										.getTypeConfiguration()
						);
					}
			);
		}
		final Fetch fetch = parent.generateFetchableFetch(
				fetchable,
				fetchPath,
				FetchTiming.DELAYED,
				false,
				null,
				creationState
		);
//		final FetchParent fetchParent = (FetchParent) fetch;
//		fetchBuilders.forEach(
//				(subFetchPath, fetchBuilder) -> fetchBuilder.buildFetch(
//						fetchParent,
//						subFetchPath,
//						jdbcResultsMetadata,
//						legacyFetchResolver,
//						creationState
//				)
//		);

		return fetch;
	}

	@Override
	public void visitFetchBuilders(BiConsumer<String, FetchBuilder> consumer) {
		fetchBuilders.forEach( (k, v) -> consumer.accept( k.getUnaliasedLocalName(), v ) );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final ImplicitFetchBuilderEntity that = (ImplicitFetchBuilderEntity) o;
		return fetchPath.equals( that.fetchPath )
				&& fetchable.equals( that.fetchable )
				&& fetchBuilders.equals( that.fetchBuilders );
	}

	@Override
	public int hashCode() {
		int result = fetchPath.hashCode();
		result = 31 * result + fetchable.hashCode();
		result = 31 * result + fetchBuilders.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "ImplicitFetchBuilderEntity(" + fetchPath + ")";
	}
}
