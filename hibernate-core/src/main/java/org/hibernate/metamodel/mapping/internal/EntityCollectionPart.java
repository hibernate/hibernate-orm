/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.collection.internal.EntityCollectionPartTableGroup;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchJoinedImpl;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class EntityCollectionPart
		implements CollectionPart, EntityAssociationMapping, EntityValuedFetchable, FetchOptions {
	private final NavigableRole navigableRole;
	private final CollectionPersister collectionDescriptor;
	private final Nature nature;
	private final EntityMappingType entityMappingType;

	private ModelPart fkTargetModelPart;

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
	}


	@Override
	public SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup) {
		return SqlAstJoinType.INNER;
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
	public JavaType<?> getJavaTypeDescriptor() {
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
	public FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	public EntityFetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		// find or create the TableGroup associated with this `fetchablePath`
		final FromClauseAccess fromClauseAccess = creationState.getSqlAstCreationState().getFromClauseAccess();
		creationState.registerVisitedAssociationKey( getForeignKeyDescriptor().getAssociationKey() );

		TableGroup tableGroup = fromClauseAccess.resolveTableGroup(
				fetchablePath,
				np -> {
					// We need to create one.  The Result will be able to find it later by path

					// first, find the collection's TableGroup
					final TableGroup collectionTableGroup = fromClauseAccess.getTableGroup( fetchParent.getNavigablePath() );

					assert collectionTableGroup != null;

					// create a "wrapper" around the collection TableGroup adding in the entity's table references
					return new EntityCollectionPartTableGroup( fetchablePath, collectionTableGroup, this );
				}
		);

		return new EntityFetchJoinedImpl( fetchParent, this, tableGroup, selected, fetchablePath, creationState );
	}

	@Override
	public boolean isOptional() {
		return false;
	}

	@Override
	public boolean isUnwrapProxy() {
		return false;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final FromClauseAccess fromClauseAccess = creationState.getSqlAstCreationState().getFromClauseAccess();
		final TableGroup partTableGroup = fromClauseAccess.resolveTableGroup(
				navigablePath,
				np -> {
					// We need to create one.  The Result will be able to find it later by path

					// first, find the collection's TableGroup
					final TableGroup collectionTableGroup = fromClauseAccess.getTableGroup( np.getParent() );

					assert collectionTableGroup != null;

					// create a "wrapper" around the collection TableGroup adding in the entity's table references
					return new EntityCollectionPartTableGroup( np, collectionTableGroup, this );
				}
		);

		return entityMappingType.createDomainResult( navigablePath, partTableGroup, resultVariable, creationState );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		return entityMappingType.forEachSelectable( offset, consumer );
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		fkTargetModelPart.breakDownJdbcValues( domainValue, valueConsumer, session );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		entityMappingType.applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		entityMappingType.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
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
		return "EntityCollectionPart(" + navigableRole + ")@" + System.identityHashCode( this );
	}

	@Override
	public ForeignKeyDescriptor getForeignKeyDescriptor() {
		// todo (6.0) : this will not strictly work - we'd want a new ForeignKeyDescriptor that points the other direction
		return collectionDescriptor.getAttributeMapping().getKeyDescriptor();
	}

	@Override
	public ForeignKeyDescriptor.Nature getSideNature() {
		return ForeignKeyDescriptor.Nature.TARGET;
	}

	@Override
	public FetchStyle getStyle() {
		return FetchStyle.JOIN;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		return collectionDescriptor.getAttributeMapping().createTableGroupJoin(
				navigablePath,
				lhs,
				explicitSourceAlias,
				sqlAstJoinType,
				fetched,
				aliasBaseGenerator,
				sqlExpressionResolver,
				creationContext
		);
	}

	@Override
	public String getSqlAliasStem() {
		return collectionDescriptor.getAttributeMapping().getSqlAliasStem();
	}

	@Override
	public boolean containsTableReference(String tableExpression) {
		return collectionDescriptor.getAttributeMapping().containsTableReference( tableExpression );
	}
}
