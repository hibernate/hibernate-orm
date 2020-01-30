/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.collection.internal.EntityCollectionPartTableGroup;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchJoinedImpl;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class EntityCollectionPart
		implements CollectionPart, EntityAssociationMapping, EntityValuedFetchable, Association {
	private final NavigableRole navigableRole;
	private final CollectionPersister collectionDescriptor;
	private final Nature nature;
	private final EntityMappingType entityMappingType;

	private ModelPart fkTargetModelPart;
	private String[] identifyingColumns;

	@SuppressWarnings("WeakerAccess")
	public EntityCollectionPart(
			CollectionPersister collectionDescriptor,
			Nature nature,
			Value bootModelValue,
			EntityMappingType entityMappingType,
			MappingModelCreationProcess creationProcess) {
		this.navigableRole = collectionDescriptor.getNavigableRole().appendContainer( nature.getName() );
		this.collectionDescriptor = collectionDescriptor;

		this.nature = nature;
		this.entityMappingType = entityMappingType;
	}

	@SuppressWarnings("WeakerAccess")
	public void finishInitialization(
			CollectionPersister collectionDescriptor,
			Collection bootValueMapping,
			String fkTargetModelPartName,
			MappingModelCreationProcess creationProcess) {
		if ( fkTargetModelPartName == null ) {
			fkTargetModelPart = entityMappingType.getIdentifierMapping();
		}
		else {
			fkTargetModelPart = entityMappingType.findSubPart( fkTargetModelPartName, null );
		}

		final List<String> identifyingColumnsList = new ArrayList<>();
		collectionDescriptor.getAttributeMapping().getKeyDescriptor().visitReferringColumns(
				(containingTableExpression, columnExpression, jdbcMapping) -> {
					identifyingColumnsList.add( containingTableExpression + "." + columnExpression );
				}
		);
		this.identifyingColumns = identifyingColumnsList.toArray( new String[0] );
	}


	@Override
	public Nature getNature() {
		return nature;
	}

	@Override
	public MappingType getPartMappingType() {
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
	public NavigableRole getNavigableRole() {
		return navigableRole;
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
	public Fetch resolveCircularFetch(
			NavigablePath fetchablePath,
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		return null;
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
//		assert fetchParent.getReferencedMappingContainer() instanceof PluralAttributeMapping;

		// find or create the TableGroup associated with this `fetchablePath`
		creationState.getSqlAstCreationState().getFromClauseAccess().resolveTableGroup(
				fetchablePath,
				np -> {
					// We need to create one.  The Result will be able to find it later by path

					// first, find the collection's TableGroup
					final TableGroup collectionTableGroup = creationState.getSqlAstCreationState()
							.getFromClauseAccess()
							.getTableGroup( fetchParent.getNavigablePath() );

					assert collectionTableGroup != null;
					NavigablePath navigablePath = new NavigablePath( entityMappingType.getRootPathName() );
					TableGroup rhsTableGroup = creationState.getSqlAstCreationState()
							.getFromClauseAccess()
							.resolveTableGroup( navigablePath,
												npa-> {
													final QuerySpec rootQuerySpec = new QuerySpec( true );
													return entityMappingType.createRootTableGroup(
															npa,
															null,
															true,
															LockMode.NONE,
															new SqlAliasBaseManager(),
															creationState.getSqlAstCreationState().getSqlExpressionResolver(),
															() -> rootQuerySpec::applyPredicate,
															creationState.getSqlAstCreationState().getCreationContext().getSessionFactory()
													);
												});
					TableGroupJoin tableGroupJoin = new TableGroupJoin(
							navigablePath,
							SqlAstJoinType.LEFT,
							rhsTableGroup
					);
					collectionTableGroup.addTableGroupJoin( tableGroupJoin );
					// create a "wrapper" around the collection TableGroup adding in the entity's table references
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
	public EntityMappingType findContainingEntityMapping() {
		return collectionDescriptor.getAttributeMapping().findContainingEntityMapping();
	}

	@Override
	public int getNumberOfFetchables() {
		return entityMappingType.getNumberOfFetchables();
	}

	public String getMappedBy() {
		return collectionDescriptor.getMappedByProperty();
	}

	@Override
	public String toString() {
		return "EntityCollectionPart {" + navigableRole + "}";
	}

	@Override
	public ForeignKeyDescriptor getForeignKeyDescriptor() {
		// todo (6.0) : this will not strictly work - we'd want a new ForeignKeyDescriptor that points the other direction
		return collectionDescriptor.getAttributeMapping().getKeyDescriptor();
	}

	@Override
	public String[] getIdentifyingColumnExpressions() {
		return identifyingColumns;
	}
}
