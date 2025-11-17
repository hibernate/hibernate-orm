/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.AssociationKey;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.PluralTableGroup;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.collection.internal.EagerCollectionFetch;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchJoinedImpl;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;

import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping.findMapsIdPropertyName;

/**
 * Base support for EntityCollectionPart implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractEntityCollectionPart implements EntityCollectionPart, FetchOptions, TableGroupProducer {
	private final NavigableRole navigableRole;
	private final Nature nature;
	private final CollectionPersister collectionDescriptor;
	private final EntityMappingType associatedEntityTypeDescriptor;
	private final NotFoundAction notFoundAction;

	protected final Set<String> targetKeyPropertyNames;

	public AbstractEntityCollectionPart(
			Nature nature,
			Collection collectionBootDescriptor,
			CollectionPersister collectionDescriptor,
			EntityMappingType associatedEntityTypeDescriptor,
			NotFoundAction notFoundAction,
			MappingModelCreationProcess creationProcess) {
		this.navigableRole = collectionDescriptor.getNavigableRole().appendContainer( nature.getName() );
		this.nature = nature;
		this.collectionDescriptor = collectionDescriptor;
		this.associatedEntityTypeDescriptor = associatedEntityTypeDescriptor;
		this.notFoundAction = notFoundAction;

		this.targetKeyPropertyNames = resolveTargetKeyPropertyNames(
				nature,
				collectionDescriptor,
				collectionBootDescriptor,
				associatedEntityTypeDescriptor,
				creationProcess
		);
	}

	/**
	 * For Hibernate Reactive
	 */
	protected AbstractEntityCollectionPart(AbstractEntityCollectionPart original) {
		this.navigableRole = original.navigableRole;
		this.nature = original.nature;
		this.collectionDescriptor = original.collectionDescriptor;
		this.associatedEntityTypeDescriptor = original.associatedEntityTypeDescriptor;
		this.notFoundAction = original.notFoundAction;
		this.targetKeyPropertyNames = original.targetKeyPropertyNames;
	}

	@Override
	public String toString() {
		return "EntityCollectionPart(" + navigableRole.getFullPath() + ")@" + System.identityHashCode( this );
	}

	public CollectionPersister getCollectionDescriptor() {
		return collectionDescriptor;
	}

	@Override
	public EntityMappingType getMappedType() {
		return getAssociatedEntityMappingType();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public Nature getNature() {
		return nature;
	}

	@Override
	public PluralAttributeMapping getCollectionAttribute() {
		return collectionDescriptor.getAttributeMapping();
	}

	@Override
	public String getFetchableName() {
		return nature.getName();
	}

	@Override
	public int getFetchableKey() {
		return nature == Nature.INDEX || !collectionDescriptor.hasIndex() ? 0 : 1;
	}

	@Override
	public EntityMappingType getAssociatedEntityMappingType() {
		return associatedEntityTypeDescriptor;
	}

	@Override
	public NotFoundAction getNotFoundAction() {
		return notFoundAction;
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return this;
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
	public boolean incrementFetchDepth() {
		// the collection itself already increments the depth
		return false;
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
	public EntityMappingType findContainingEntityMapping() {
		return collectionDescriptor.getAttributeMapping().findContainingEntityMapping();
	}

	@Override
	public int getNumberOfFetchables() {
		return getAssociatedEntityMappingType().getNumberOfFetchables();
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final TableGroup partTableGroup = resolveTableGroup( navigablePath, creationState );
		return associatedEntityTypeDescriptor.createDomainResult( navigablePath, partTableGroup, resultVariable, creationState );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}

		// should be an instance of the associated entity
		return getAssociatedEntityMappingType().getIdentifierMapping().getIdentifier( value );
	}


	@Override
	public EntityFetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		final var associationKey = resolveFetchAssociationKey();
		final boolean added = creationState.registerVisitedAssociationKey( associationKey );

		final var partTableGroup = resolveTableGroup( fetchablePath, creationState );
		final var fetch = buildEntityFetchJoined(
				fetchParent,
				this,
				partTableGroup,
				fetchablePath,
				creationState
		);

		if ( added ) {
			creationState.removeVisitedAssociationKey( associationKey );
		}

		return fetch;
	}

	/**
	 * For Hibernate Reactive
	 */
	protected EagerCollectionFetch buildEagerCollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			TableGroup collectionTableGroup,
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		return new EagerCollectionFetch(
				fetchedPath,
				fetchedAttribute,
				collectionTableGroup,
				true,
				fetchParent,
				creationState
		);
	}

	/**
	 * For Hibernate Reactive
	 */
	protected EntityFetch buildEntityFetchJoined(
			FetchParent fetchParent,
			AbstractEntityCollectionPart abstractEntityCollectionPart,
			TableGroup partTableGroup,
			NavigablePath fetchablePath,
			DomainResultCreationState creationState) {
		return new EntityFetchJoinedImpl(
				fetchParent,
				abstractEntityCollectionPart,
				partTableGroup,
				fetchablePath,
				creationState
		);
	}

	protected abstract AssociationKey resolveFetchAssociationKey();

	private TableGroup resolveTableGroup(NavigablePath fetchablePath, DomainResultCreationState creationState) {
		final var fromClauseAccess = creationState.getSqlAstCreationState().getFromClauseAccess();
		return fromClauseAccess.resolveTableGroup( fetchablePath, (np) -> {
			final var parentTableGroup = (PluralTableGroup) fromClauseAccess.getTableGroup( np.getParent() );
			return switch ( nature ) {
				case ELEMENT -> parentTableGroup.getElementTableGroup();
				case INDEX -> resolveIndexTableGroup( parentTableGroup, fetchablePath, fromClauseAccess, creationState );
				default -> throw new IllegalStateException( "Could not find table group for: " + np );
			};

		} );
	}

	private TableGroup resolveIndexTableGroup(
			PluralTableGroup collectionTableGroup,
			NavigablePath fetchablePath,
			FromClauseAccess fromClauseAccess,
			DomainResultCreationState creationState) {
		return collectionTableGroup.getIndexTableGroup();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// TableGroupProducer

	@Override
	public String getSqlAliasStem() {
		return getCollectionDescriptor().getAttributeMapping().getSqlAliasStem();
	}

	@Override
	public boolean containsTableReference(String tableExpression) {
		return getAssociatedEntityMappingType().containsTableReference( tableExpression );
	}

	public TableGroup createTableGroupInternal(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			boolean fetched,
			String sourceAlias,
			final SqlAliasBase sqlAliasBase,
			SqlAstCreationState creationState) {
		final var creationContext = creationState.getCreationContext();
		final var primaryTableReference =
				getEntityMappingType()
						.createPrimaryTableReference( sqlAliasBase, creationState );

		final TableGroup tableGroup = new StandardTableGroup(
				canUseInnerJoins,
				navigablePath,
				this,
				fetched,
				sourceAlias,
				primaryTableReference,
				true,
				sqlAliasBase,
				getEntityMappingType().getRootEntityDescriptor()::containsTableReference,
				(tableExpression, group) -> getEntityMappingType().createTableReferenceJoin(
						tableExpression,
						sqlAliasBase,
						primaryTableReference,
						creationState
				),
				creationContext.getSessionFactory()
		);
		// Make sure the association key's table is resolved in the table group
		tableGroup.getTableReference( null, resolveFetchAssociationKey().table(), true );
		return tableGroup;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Initialization

	private static Set<String> resolveTargetKeyPropertyNames(
			Nature nature,
			CollectionPersister collectionDescriptor,
			Collection collectionBootDescriptor,
			EntityMappingType elementTypeDescriptor,
			MappingModelCreationProcess creationProcess) {
		final Value bootModelValue =
				nature == Nature.INDEX
						? ( (IndexedCollection) collectionBootDescriptor ).getIndex()
						: collectionBootDescriptor.getElement();
		final var entityBinding =
				creationProcess.getCreationContext().getMetadata()
						.getEntityBinding( elementTypeDescriptor.getEntityName() );

		final String referencedPropertyName;
		if ( bootModelValue instanceof OneToMany ) {
			final String mappedByProperty = collectionDescriptor.getMappedByProperty();
			referencedPropertyName =
					isEmpty( mappedByProperty )
							? null
							: mappedByProperty;
		}
		else if ( bootModelValue instanceof ToOne toOne ) {
			referencedPropertyName = toOne.getReferencedPropertyName();
		}
		else {
			throw new AssertionFailure( "Expected a OneToMany or ToOne" );
		}

		if ( referencedPropertyName == null ) {
			final Set<String> targetKeyPropertyNames = new HashSet<>( 2 );
			targetKeyPropertyNames.add( EntityIdentifierMapping.ID_ROLE_NAME );
			final Type propertyType =
					entityBinding.getIdentifierMapper() == null
							? entityBinding.getIdentifier().getType()
							: entityBinding.getIdentifierMapper().getType();
			if ( entityBinding.getIdentifierProperty() == null ) {
				if ( propertyType instanceof ComponentType compositeType
						&& compositeType.isEmbedded()
						&& compositeType.getPropertyNames().length == 1 ) {
					ToOneAttributeMapping.addPrefixedPropertyPaths(
							targetKeyPropertyNames,
							compositeType.getPropertyNames()[0],
							compositeType.getSubtypes()[0],
							creationProcess.getCreationContext().getSessionFactory()
					);
					ToOneAttributeMapping.addPrefixedPropertyNames(
							targetKeyPropertyNames,
							EntityIdentifierMapping.ID_ROLE_NAME,
							propertyType,
							creationProcess.getCreationContext().getSessionFactory()
					);
				}
				else {
					ToOneAttributeMapping.addPrefixedPropertyPaths(
							targetKeyPropertyNames,
							null,
							propertyType,
							creationProcess.getCreationContext().getSessionFactory()
					);
				}
			}
			else {
				ToOneAttributeMapping.addPrefixedPropertyPaths(
						targetKeyPropertyNames,
						entityBinding.getIdentifierProperty().getName(),
						propertyType,
						creationProcess.getCreationContext().getSessionFactory()
				);
			}
			return targetKeyPropertyNames;
		}
		else if ( bootModelValue instanceof OneToMany ) {
			final Set<String> targetKeyPropertyNames = new HashSet<>( 2 );
			int dotIndex = -1;
			while ( ( dotIndex = referencedPropertyName.indexOf( '.', dotIndex + 1 ) ) != -1 ) {
				targetKeyPropertyNames.add( referencedPropertyName.substring( 0, dotIndex ) );
			}
			// todo (PropertyMapping) : the problem here is timing.  this needs to be delayed.
			ToOneAttributeMapping.addPrefixedPropertyPaths(
					targetKeyPropertyNames,
					referencedPropertyName,
					elementTypeDescriptor.getEntityPersister().getPropertyType( referencedPropertyName ),
					creationProcess.getCreationContext().getSessionFactory()
			);
			return targetKeyPropertyNames;
		}
		else {
			final Type propertyType = entityBinding.getRecursiveProperty( referencedPropertyName ).getType();
			if ( propertyType instanceof ComponentType compositeType
					&& compositeType.isEmbedded()
					&& compositeType.getPropertyNames().length == 1 ) {
				final Set<String> targetKeyPropertyNames = new HashSet<>( 2 );
				ToOneAttributeMapping.addPrefixedPropertyPaths(
						targetKeyPropertyNames,
						compositeType.getPropertyNames()[0],
						compositeType.getSubtypes()[0],
						creationProcess.getCreationContext().getSessionFactory()
				);
				ToOneAttributeMapping.addPrefixedPropertyNames(
						targetKeyPropertyNames,
						EntityIdentifierMapping.ID_ROLE_NAME,
						propertyType,
						creationProcess.getCreationContext().getSessionFactory()
				);
				return targetKeyPropertyNames;
			}
			else {
				final Set<String> targetKeyPropertyNames = new HashSet<>( 2 );
				targetKeyPropertyNames.add( EntityIdentifierMapping.ID_ROLE_NAME );
				targetKeyPropertyNames.add( referencedPropertyName );
				final String mapsIdAttributeName =
						findMapsIdPropertyName( elementTypeDescriptor, referencedPropertyName );
				if ( mapsIdAttributeName != null ) {
					ToOneAttributeMapping.addPrefixedPropertyPaths(
							targetKeyPropertyNames,
							mapsIdAttributeName,
							elementTypeDescriptor.getEntityPersister().getIdentifierType(),
							creationProcess.getCreationContext().getSessionFactory()
					);
				}
				else {
					ToOneAttributeMapping.addPrefixedPropertyPaths(
							targetKeyPropertyNames,
							null,
							propertyType,
							creationProcess.getCreationContext().getSessionFactory()
					);
				}
				return targetKeyPropertyNames;
			}
		}
	}
}
