/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity;

import java.util.BitSet;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityRowIdMapping;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * AbstractFetchParent sub-class for entity-valued graph nodes
 *
 * @author Steve Ebersole
 */
public abstract class AbstractEntityResultGraphNode extends AbstractFetchParent implements EntityResultGraphNode {
	private Fetch identifierFetch;
	private BasicFetch<?> discriminatorFetch;
	private DomainResult<Object> rowIdResult;
	private final EntityValuedModelPart fetchContainer;

	public AbstractEntityResultGraphNode(EntityValuedModelPart referencedModelPart, NavigablePath navigablePath) {
		super( navigablePath );
		this.fetchContainer = referencedModelPart;
	}

	@Override
	public void afterInitialize(FetchParent fetchParent, DomainResultCreationState creationState) {
		final NavigablePath navigablePath = getNavigablePath();
		final TableGroup entityTableGroup = creationState.getSqlAstCreationState().getFromClauseAccess()
				.getTableGroup( navigablePath );
		final EntityResultGraphNode entityResultGraphNode = (EntityResultGraphNode) fetchParent;
		final Fetch idFetch = creationState.visitIdentifierFetch( entityResultGraphNode );
		if ( navigablePath.getParent() == null && !creationState.forceIdentifierSelection() &&
				( idFetch.asFetchParent() == null || !idFetch.asFetchParent().containsCollectionFetches() ) ) {
			identifierFetch = null;
		}
		else {
			identifierFetch = idFetch;
		}

		discriminatorFetch = creationState.visitDiscriminatorFetch( entityResultGraphNode );

		final EntityRowIdMapping rowIdMapping = getEntityValuedModelPart().getEntityMappingType().getRowIdMapping();
		if ( rowIdMapping == null ) {
			rowIdResult = null;
		}
		else {
			rowIdResult = rowIdMapping.createDomainResult(
					navigablePath.append( rowIdMapping.getRowIdName() ),
					entityTableGroup,
					AbstractEntityPersister.ROWID_ALIAS,
					creationState
			);
		}
		super.afterInitialize( fetchParent, creationState );
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

	public Fetch getIdentifierFetch() {
		return identifierFetch;
	}

	public BasicFetch<?> getDiscriminatorFetch() {
		return discriminatorFetch;
	}

	public DomainResult<Object> getRowIdResult() {
		return rowIdResult;
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		final EntityPersister entityPersister = fetchContainer.getEntityMappingType().getEntityPersister();
		identifierFetch.collectValueIndexesToCache( valueIndexes );
		if ( !entityPersister.useShallowQueryCacheLayout() ) {
			if ( discriminatorFetch != null ) {
				discriminatorFetch.collectValueIndexesToCache( valueIndexes );
			}
			if ( rowIdResult != null ) {
				rowIdResult.collectValueIndexesToCache( valueIndexes );
			}
			super.collectValueIndexesToCache( valueIndexes );
		}
		else if ( entityPersister.storeDiscriminatorInShallowQueryCacheLayout() && discriminatorFetch != null ) {
			discriminatorFetch.collectValueIndexesToCache( valueIndexes );
		}
	}

}
