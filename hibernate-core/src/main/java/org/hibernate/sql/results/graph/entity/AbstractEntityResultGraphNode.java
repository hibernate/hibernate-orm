package org.hibernate.sql.results.graph.entity;

import jakarta.annotation.Nullable;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.internal.CurrentState;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.expression.Expression;
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

import static org.hibernate.metamodel.mapping.internal.CurrentState.hasHistory;
import static org.hibernate.metamodel.mapping.internal.CurrentState.ownerRevision;

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
	private boolean currentState;
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
		final var sqlAstCreationState = creationState.getSqlAstCreationState();
		currentState = hasHistory( getEntityValuedModelPart().getEntityMappingType() )
				&& CurrentState.readsCurrentState( entityTableGroup, sqlAstCreationState );
		auditChangesetIdResult = currentState ? null : auditChangesetIdResult( creationState, entityTableGroup );

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
			TableGroup entityTableGroup) {
		final var sqlAstCreationState = creationState.getSqlAstCreationState();
		if ( !sqlAstCreationState.getLoadQueryInfluencers().isAllRevisions() ) {
			return null;
		}
		final var entityMappingType = getEntityValuedModelPart().getEntityMappingType();
		final var navigablePath = entityTableGroup.getNavigablePath();
		final var joinedPart = entityTableGroup.getModelPart();
		final boolean association =
				joinedPart instanceof ToOneAttributeMapping || joinedPart instanceof EntityCollectionPart;
		final var expressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final var auditMapping = entityMappingType.getAuditMapping();
		final var temporalMapping = entityMappingType.getTemporalMapping();
		final Expression changesetId;
		if ( auditMapping != null ) {
			// The target of a to-one, or an element, is the target as of its owner's revision,
			// however it is joined, and anything else is the snapshot its own row holds
			final var ownerRevision = association ? ownerRevision( navigablePath, sqlAstCreationState ) : null;
			final String table = entityMappingType.getMappedTableDetails().getTableName();
			changesetId = ownerRevision != null ? ownerRevision
					: expressionResolver.resolveSqlExpression(
							entityTableGroup.resolveTableReference( auditMapping.resolveTableName( table ) ),
							auditMapping.getChangesetIdMapping( table ) );
		}
		else if ( association && temporalMapping != null ) {
			// A temporal target or element of an audit snapshot is read as of the snapshot's revision
			changesetId = ownerRevision( temporalMapping, navigablePath, sqlAstCreationState );
		}
		else {
			changesetId = null;
		}
		if ( changesetId == null ) {
			return null;
		}
		final var jdbcMapping = changesetId.getExpressionType().getSingleJdbcMapping();
		final var sqlSelection = expressionResolver.resolveSqlSelection(
				changesetId,
				jdbcMapping.getJdbcJavaType(),
				null,
				sqlAstCreationState.getCreationContext().getTypeConfiguration()
		);
		return new BasicResult<>( sqlSelection.getValuesArrayPosition(), "audit_txn_id", jdbcMapping );
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

	@Override
	public boolean readsCurrentState() {
		return currentState;
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
