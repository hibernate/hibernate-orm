package org.hibernate.sql.results.graph.entity;

import jakarta.annotation.Nullable;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.BitSet;

import static org.hibernate.metamodel.mapping.internal.AuditMappingImpl.findParentRevColumn;

/**
 * AbstractFetchParent sub-class for entity-valued graph nodes
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public abstract class AbstractEntityResultGraphNode extends AbstractFetchParent implements EntityResultGraphNode {
	private @Nullable Fetch identifierFetch;
	private BasicFetch<?> discriminatorFetch;
	private DomainResult<Object> rowIdResult;
	private @Nullable DomainResult<?> auditChangesetIdResult;
	private final EntityValuedModelPart fetchContainer;

	@org.hibernate.SPI(org.hibernate.SPI.Role.IMPLEMENT)
	public AbstractEntityResultGraphNode(EntityValuedModelPart referencedModelPart, NavigablePath navigablePath) {
		super( navigablePath );
		this.fetchContainer = referencedModelPart;
	}

	@Override
	public void afterInitialize(FetchParent fetchParent, DomainResultCreationState creationState) {
		final var navigablePath = getNavigablePath();
		final var entityTableGroup =
				creationState.getSqlAstCreationState().getFromClauseAccess()
						.getTableGroup( navigablePath );

		final var entityResultGraphNode = (EntityResultGraphNode) fetchParent;
		identifierFetch = identifierFetch( creationState, entityResultGraphNode, navigablePath );
		discriminatorFetch = creationState.visitDiscriminatorFetch( entityResultGraphNode );

		rowIdResult = rowIdResult( creationState, navigablePath, entityTableGroup );
		auditChangesetIdResult = auditChangesetIdResult( creationState, entityTableGroup,
				fetchParent instanceof Fetch && fetchContainer instanceof ToOneAttributeMapping );

		super.afterInitialize( fetchParent, creationState );
	}

	private DomainResult<Object> rowIdResult(
			DomainResultCreationState creationState,
			NavigablePath navigablePath,
			TableGroup entityTableGroup) {
		final var rowIdMapping =
				getEntityValuedModelPart().getEntityMappingType()
						.getRowIdMapping();
		if ( rowIdMapping == null ) {
			return null;
		}
		else {
			return rowIdMapping.createDomainResult(
					navigablePath.append( rowIdMapping.getRowIdName() ),
					entityTableGroup,
					AbstractEntityPersister.ROWID_ALIAS,
					creationState
			);
		}
	}

	private Fetch identifierFetch(
			DomainResultCreationState creationState,
			EntityResultGraphNode entityResultGraphNode,
			NavigablePath navigablePath) {
		final var idFetch = creationState.visitIdentifierFetch( entityResultGraphNode );
		if ( navigablePath.getParent() == null
				&& !creationState.forceIdentifierSelection()
				&& ( idFetch.asFetchParent() == null || !idFetch.asFetchParent().containsCollectionFetches() ) ) {
			return null;
		}
		else {
			return idFetch;
		}
	}

	private @Nullable DomainResult<?> auditChangesetIdResult(
			DomainResultCreationState creationState,
			TableGroup entityTableGroup,
			boolean toOneFetch) {
		final var entityMappingType = getEntityValuedModelPart().getEntityMappingType();
		final var auditMapping = entityMappingType.getAuditMapping();
		if ( auditMapping != null ) {
			final var influencers = creationState.getSqlAstCreationState().getLoadQueryInfluencers();
			if ( influencers.isAllRevisions() && auditMapping.useAuxiliaryTable( influencers ) ) {
				final String originalTable = entityMappingType.getMappedTableDetails().getTableName();
				final var changesetIdMapping = auditMapping.getChangesetIdMapping( originalTable );
				final var sqlAstCreationState = creationState.getSqlAstCreationState();
				final var expressionResolver = sqlAstCreationState.getSqlExpressionResolver();
				// A correlated fetch represents the target as of its parent's revision.
				final var parentRevColumn = toOneFetch
						? findParentRevColumn( entityTableGroup.getNavigablePath(), sqlAstCreationState )
						: null;
				final var expression = parentRevColumn != null ? parentRevColumn
						: expressionResolver.resolveSqlExpression(
								entityTableGroup.resolveTableReference( auditMapping.resolveTableName( originalTable ) ),
								changesetIdMapping );
				final var sqlSelection = expressionResolver.resolveSqlSelection(
						expression,
						changesetIdMapping.getJdbcMapping().getJdbcJavaType(),
						null,
						sqlAstCreationState.getCreationContext().getTypeConfiguration()
				);
				return new BasicResult<>(
						sqlSelection.getValuesArrayPosition(),
						"audit_txn_id",
						changesetIdMapping.getJdbcMapping()
				);
			}
		}
		return null;
	}

	@Override
	public EntityMappingType getReferencedMappingContainer() {
		return getEntityValuedModelPart().getEntityMappingType();
	}

	@Override
	public EntityValuedModelPart getEntityValuedModelPart() {
		return this.fetchContainer;
	}

	@Override
	public FetchableContainer getFetchContainer() {
		return this.fetchContainer;
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return getEntityValuedModelPart().getEntityMappingType().getMappedJavaType();
	}

	public @Nullable Fetch getIdentifierFetch() {
		return identifierFetch;
	}

	public BasicFetch<?> getDiscriminatorFetch() {
		return discriminatorFetch;
	}

	public DomainResult<Object> getRowIdResult() {
		return rowIdResult;
	}

	public @Nullable DomainResult<?> getAuditChangesetIdResult() {
		return auditChangesetIdResult;
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		final var entityPersister = fetchContainer.getEntityMappingType().getEntityPersister();
		if ( identifierFetch != null ) {
			identifierFetch.collectValueIndexesToCache( valueIndexes );
		}
		if ( auditChangesetIdResult != null ) {
			auditChangesetIdResult.collectValueIndexesToCache( valueIndexes );
		}
		if ( !entityPersister.useShallowQueryCacheLayout() ) {
			if ( discriminatorFetch != null ) {
				discriminatorFetch.collectValueIndexesToCache( valueIndexes );
			}
			if ( rowIdResult != null ) {
				rowIdResult.collectValueIndexesToCache( valueIndexes );
			}
			super.collectValueIndexesToCache( valueIndexes );
		}
		else if ( entityPersister.storeDiscriminatorInShallowQueryCacheLayout()
					&& discriminatorFetch != null ) {
			discriminatorFetch.collectValueIndexesToCache( valueIndexes );
		}
	}

}
