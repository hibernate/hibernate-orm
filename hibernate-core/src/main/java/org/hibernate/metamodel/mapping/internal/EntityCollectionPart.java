/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.collection.internal.EntityCollectionPartTableGroup;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchJoinedImpl;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class EntityCollectionPart implements CollectionPart, EntityAssociationMapping, EntityValuedFetchable {
	private final Nature nature;
	private final EntityMappingType entityMappingType;

	private ModelPart fkTargetModelPart;

	@SuppressWarnings("WeakerAccess")
	public EntityCollectionPart(
			Nature nature,
			EntityMappingType entityMappingType,
			String fkTargetModelPartName,
			MappingModelCreationProcess creationProcess) {
		this.nature = nature;
		this.entityMappingType = entityMappingType;

		creationProcess.registerInitializationCallback(
				() -> {
					fkTargetModelPart = fkTargetModelPartName == null
							? entityMappingType.getIdentifierMapping()
							: entityMappingType.findSubPart( fkTargetModelPartName, entityMappingType );
					assert fkTargetModelPart != null;
					return true;
				}
		);
	}

	@Override
	public Nature getNature() {
		return nature;
	}

	@Override
	public EntityMappingType getPartTypeDescriptor() {
		return getEntityMappingType();
	}

	@Override
	public EntityMappingType getEntityMappingType() {
		return entityMappingType;
	}

	@Override
	public EntityMappingType getAssociatedEntityMappingType() {
		return getEntityMappingType();
	}

	@Override
	public ModelPart getKeyTargetMatchPart() {
		return fkTargetModelPart;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getEntityMappingType().getJavaTypeDescriptor();
	}

	@Override
	public String getFetchableName() {
		return nature.getName();
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return FetchStrategy.IMMEDIATE_JOIN;
	}

	@Override
	public EntityFetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		creationState.getSqlAstCreationState().getFromClauseAccess().resolveTableGroup(
				fetchablePath,
				np -> {
					final TableGroup collectionTableGroup = creationState.getSqlAstCreationState()
							.getFromClauseAccess()
							.getTableGroup( fetchablePath.getParent() );
					return new EntityCollectionPartTableGroup( fetchablePath, collectionTableGroup, this );
				}
		);
		return new EntityFetchJoinedImpl( fetchParent, this, lockMode, selected, fetchablePath, creationState );
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public int getNumberOfFetchables() {
		return entityMappingType.getNumberOfFetchables();
	}
}
