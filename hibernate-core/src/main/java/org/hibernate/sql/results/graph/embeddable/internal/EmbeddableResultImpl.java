/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import java.util.ArrayList;
import java.util.BitSet;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.internal.util.NullnessUtil;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.InitializerProducer;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResult;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class EmbeddableResultImpl<T> extends AbstractFetchParent implements EmbeddableResultGraphNode,
		DomainResult<T>,
		EmbeddableResult<T>,
		InitializerProducer<EmbeddableResultImpl<T>> {

	private static final CollectionLoadingAttribute[] NO_COLLECTION_LOADING_ATTRIBUTES = new CollectionLoadingAttribute[0];

	private final String resultVariable;
	private final boolean containsAnyNonScalars;
	private final EmbeddableMappingType fetchContainer;
	private final BasicFetch<?> discriminatorFetch;
	private final @Nullable DomainResult<Boolean> nullIndicatorResult;
	private final CollectionLoadingAttribute[] collectionLoadingAttributes;

	public EmbeddableResultImpl(
			NavigablePath navigablePath,
			EmbeddableValuedModelPart modelPart,
			String resultVariable,
			DomainResultCreationState creationState) {
		/*
			An `{embeddable_result}` sub-path is created for the corresponding initializer to differentiate it from a fetch-initializer if this embedded is also fetched.
			The Jakarta Persistence spec says that any embedded value selected in the result should not be part of the state of any managed entity.
			Using this `{embeddable_result}` sub-path avoids this situation.
		*/
		super( navigablePath.append( "{embeddable_result}" ) );
		this.fetchContainer = modelPart.getEmbeddableTypeDescriptor();
		this.resultVariable = resultVariable;

		final var sqlAstCreationState = creationState.getSqlAstCreationState();
		final var fromClauseAccess = sqlAstCreationState.getFromClauseAccess();

		final var embeddableTableGroup = fromClauseAccess.resolveTableGroup(
				getNavigablePath(),
				np -> {
					final var tableGroup =
							fromClauseAccess.findTableGroup( NullnessUtil.castNonNull( np.getParent() ).getParent() );
					final var tableGroupJoin =
							modelPart.getEmbeddableTypeDescriptor()
									.getEmbeddedValueMapping()
									.createTableGroupJoin(
											np,
											tableGroup,
											resultVariable,
											null,
											SqlAstJoinType.INNER,
											true,
											false,
											sqlAstCreationState
									);
					tableGroup.addTableGroupJoin( tableGroupJoin );
					return tableGroupJoin.getJoinedGroup();
				}
		);

		discriminatorFetch = creationState.visitEmbeddableDiscriminatorFetch( this, false );
		final TableGroup ownerTableGroup =
				fromClauseAccess.findTableGroup( NullnessUtil.castNonNull( navigablePath.getParent() ) );
		collectionLoadingAttributes = collectionLoadingAttributes(
				modelPart,
				ownerTableGroup,
				creationState
		);
		final var aggregateNullIndicatorResult =
				nullIndicatorResult( creationState, embeddableTableGroup, sqlAstCreationState );
		nullIndicatorResult = aggregateNullIndicatorResult == null && collectionLoadingAttributes.length > 0
				? new CollectionKeyNullIndicatorResult(
						collectionLoadingAttributes[0].collectionKeyResult(),
						sqlAstCreationState.getCreationContext()
								.getTypeConfiguration()
								.getBasicTypeForJavaType( Boolean.class )
								.getJavaTypeDescriptor()
				)
				: aggregateNullIndicatorResult;

		resetFetches( withoutCollectionFetches( creationState.visitFetches( this ) ) );

		// after-after-initialize :D
		containsAnyNonScalars = determineIfContainedAnyScalars( getFetches() );
	}

	private CollectionLoadingAttribute[] collectionLoadingAttributes(
			EmbeddableValuedModelPart modelPart,
			TableGroup ownerTableGroup,
			DomainResultCreationState creationState) {
		if ( ownerTableGroup == null ) {
			return NO_COLLECTION_LOADING_ATTRIBUTES;
		}
		else {
			final var collectionLoadingAttributes = new ArrayList<CollectionLoadingAttribute>();
			try {
				modelPart.getEmbeddableTypeDescriptor().forEachAttributeMapping(
						attributeMapping -> {
							if ( attributeMapping instanceof PluralAttributeMapping pluralAttributeMapping ) {
								collectionLoadingAttributes.add(
										new CollectionLoadingAttribute(
												pluralAttributeMapping,
												pluralAttributeMapping.getKeyDescriptor().createTargetDomainResult(
														ownerTableGroup.getNavigablePath()
																.append( pluralAttributeMapping.getPartName() ),
														ownerTableGroup,
														this,
														creationState
												)
										)
								);
							}
						}
				);
			}
			catch (UnsupportedOperationException ignored) {
				return NO_COLLECTION_LOADING_ATTRIBUTES;
			}
			return collectionLoadingAttributes.toArray( CollectionLoadingAttribute[]::new );
		}
	}

	private ImmutableFetchList withoutCollectionFetches(ImmutableFetchList fetches) {
		if ( collectionLoadingAttributes.length == 0 ) {
			return fetches;
		}
		final var builder = new ImmutableFetchList.Builder( fetchContainer );
		for ( var fetch : fetches ) {
			if ( !( fetch.getFetchedMapping() instanceof PluralAttributeMapping ) ) {
				builder.add( fetch );
			}
		}
		return builder.build();
	}

	private DomainResult<Boolean> nullIndicatorResult(
			DomainResultCreationState creationState,
			TableGroup embeddableTableGroup,
			SqlAstCreationState sqlAstCreationState) {
		final var aggregateMapping = fetchContainer.getAggregateMapping();
		if ( aggregateMapping != null ) {
			final var tableReference =
					embeddableTableGroup.resolveTableReference(
							aggregateMapping.getContainingTableExpression() );
			final var aggregateExpression =
					sqlAstCreationState.getSqlExpressionResolver()
							.resolveSqlExpression( tableReference, aggregateMapping );
			final var booleanType =
					sqlAstCreationState.getCreationContext().getTypeConfiguration()
							.getBasicTypeForJavaType( Boolean.class );
			return new NullnessPredicate( aggregateExpression, false, booleanType )
					.createDomainResult( null, creationState );
		}
		else {
			return null;
		}
	}

	private static boolean determineIfContainedAnyScalars(ImmutableFetchList fetches) {
		for ( var fetch : fetches ) {
			if ( fetch.containsAnyNonScalarResults() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public boolean containsAnyNonScalarResults() {
		return containsAnyNonScalars;
	}

	@Override
	public EmbeddableMappingType getFetchContainer() {
		return this.fetchContainer;
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return getReferencedMappingType().getJavaType();
	}

	@Override
	public EmbeddableMappingType getReferencedMappingType() {
		return getFetchContainer();
	}

	@Override
	public EmbeddableValuedModelPart getReferencedMappingContainer() {
		return getFetchContainer().getEmbeddedValueMapping();
	}

	@Override
	public DomainResultAssembler<T> createResultAssembler(
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		final var collectionLoaders = new EmbeddableAssembler.CollectionLoader[collectionLoadingAttributes.length];
		for ( int i = 0; i < collectionLoadingAttributes.length; i++ ) {
			final var collectionLoadingAttribute = collectionLoadingAttributes[i];
			collectionLoaders[i] = new EmbeddableAssembler.CollectionLoader(
					collectionLoadingAttribute.collectionAttribute(),
					collectionLoadingAttribute.collectionKeyResult()
							.createResultAssembler( parent, creationState )
			);
		}
		//noinspection unchecked
		return new EmbeddableAssembler(
				creationState.resolveInitializer( this, parent, this )
						.asEmbeddableInitializer(),
				collectionLoaders
		);
	}

	@Override
	public Initializer<?> createInitializer(
			EmbeddableResultImpl<T> resultGraphNode,
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return resultGraphNode.createInitializer( parent, creationState );
	}

	@Override
	public Initializer<?> createInitializer(InitializerParent<?> parent, AssemblerCreationState creationState) {
		return new EmbeddableInitializerImpl( this, discriminatorFetch, nullIndicatorResult, parent, creationState, true );
	}

	private record CollectionLoadingAttribute(
			PluralAttributeMapping collectionAttribute,
			DomainResult<?> collectionKeyResult) {
	}

	private record CollectionKeyNullIndicatorResult
			(DomainResult<?> collectionKeyResult, JavaType<Boolean> javaType)
			implements DomainResult<Boolean> {

		@Override
		public String getResultVariable() {
			return null;
		}

		@Override
		public JavaType<?> getResultJavaType() {
			return javaType;
		}

		@Override
		public DomainResultAssembler<Boolean> createResultAssembler(
				InitializerParent<?> parent,
				AssemblerCreationState creationState) {
			final var collectionKeyAssembler =
					collectionKeyResult.createResultAssembler( parent, creationState );
			return new DomainResultAssembler<>() {
				@Override
				public Boolean assemble(RowProcessingState rowProcessingState) {
					return collectionKeyAssembler.assemble( rowProcessingState ) == null;
				}

				@Override
				public JavaType<Boolean> getAssembledJavaType() {
					return javaType;
				}

				@Override
				public void resolveState(RowProcessingState rowProcessingState) {
					collectionKeyAssembler.resolveState( rowProcessingState );
				}
			};
		}

		@Override
		public void collectValueIndexesToCache(BitSet valueIndexes) {
			collectionKeyResult.collectValueIndexesToCache( valueIndexes );
		}
	}
}
