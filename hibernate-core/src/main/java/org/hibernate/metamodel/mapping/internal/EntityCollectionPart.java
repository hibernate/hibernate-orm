/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.VirtualModelPart;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.LazyTableGroup;
import org.hibernate.sql.ast.tree.from.OneToManyTableGroup;
import org.hibernate.sql.ast.tree.from.PluralTableGroup;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchJoinedImpl;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;

import static java.util.Objects.requireNonNullElse;

/**
 * @author Steve Ebersole
 */
public class EntityCollectionPart
		implements CollectionPart, EntityAssociationMapping, EntityValuedFetchable, FetchOptions {
	private final NavigableRole navigableRole;
	private final CollectionPersister collectionDescriptor;
	private final Nature nature;
	private final EntityMappingType entityMappingType;
	private final Set<String> targetKeyPropertyNames;

	private ModelPart fkTargetModelPart;
	private ForeignKeyDescriptor fkDescriptor;

	public EntityCollectionPart(
			CollectionPersister collectionDescriptor,
			Nature nature,
			Value bootModelValue,
			@SuppressWarnings("unused") NotFoundAction notFoundAction,
			EntityMappingType entityMappingType,
			MappingModelCreationProcess creationProcess) {
		this.navigableRole = collectionDescriptor.getNavigableRole().appendContainer( nature.getName() );
		this.collectionDescriptor = collectionDescriptor;
		this.nature = nature;
		this.entityMappingType = entityMappingType;

		final PersistentClass entityBinding = creationProcess.getCreationContext()
				.getMetadata()
				.getEntityBinding( entityMappingType.getEntityName() );
		final String referencedPropertyName;

		if ( bootModelValue instanceof OneToMany ) {
			final String mappedByProperty = collectionDescriptor.getMappedByProperty();
			referencedPropertyName = StringHelper.isEmpty( mappedByProperty )
					? null
					: mappedByProperty;
		}
		else {
			final ToOne toOne = (ToOne) bootModelValue;
			referencedPropertyName = toOne.getReferencedPropertyName();
		}

		if ( referencedPropertyName == null ) {
			final Set<String> targetKeyPropertyNames = new HashSet<>( 2 );
			targetKeyPropertyNames.add( EntityIdentifierMapping.ROLE_LOCAL_NAME );
			final Type propertyType;
			if ( entityBinding.getIdentifierMapper() == null ) {
				propertyType = entityBinding.getIdentifier().getType();
			}
			else {
				propertyType = entityBinding.getIdentifierMapper().getType();
			}
			if ( entityBinding.getIdentifierProperty() == null ) {
				final CompositeType compositeType;
				if ( propertyType.isComponentType() && ( compositeType = (CompositeType) propertyType ).isEmbedded()
						&& compositeType.getPropertyNames().length == 1 ) {
					ToOneAttributeMapping.addPrefixedPropertyNames(
							targetKeyPropertyNames,
							compositeType.getPropertyNames()[0],
							compositeType.getSubtypes()[0],
							creationProcess.getCreationContext().getSessionFactory()
					);
					ToOneAttributeMapping.addPrefixedPropertyNames(
							targetKeyPropertyNames,
							ForeignKeyDescriptor.PART_NAME,
							compositeType.getSubtypes()[0],
							creationProcess.getCreationContext().getSessionFactory()
					);
				}
				else {
					ToOneAttributeMapping.addPrefixedPropertyNames(
							targetKeyPropertyNames,
							null,
							propertyType,
							creationProcess.getCreationContext().getSessionFactory()
					);
					ToOneAttributeMapping.addPrefixedPropertyNames(
							targetKeyPropertyNames,
							ForeignKeyDescriptor.PART_NAME,
							propertyType,
							creationProcess.getCreationContext().getSessionFactory()
					);
				}
			}
			else {
				ToOneAttributeMapping.addPrefixedPropertyNames(
						targetKeyPropertyNames,
						entityBinding.getIdentifierProperty().getName(),
						propertyType,
						creationProcess.getCreationContext().getSessionFactory()
				);
				ToOneAttributeMapping.addPrefixedPropertyNames(
						targetKeyPropertyNames,
						ForeignKeyDescriptor.PART_NAME,
						propertyType,
						creationProcess.getCreationContext().getSessionFactory()
				);
			}
			this.targetKeyPropertyNames = targetKeyPropertyNames;
		}
		else if ( bootModelValue instanceof OneToMany ) {
			final Set<String> targetKeyPropertyNames = new HashSet<>( 2 );
			int dotIndex = -1;
			while ( ( dotIndex = referencedPropertyName.indexOf( '.', dotIndex + 1 ) ) != -1 ) {
				targetKeyPropertyNames.add( referencedPropertyName.substring( 0, dotIndex ) );
			}
			final Type propertyType = ( (PropertyMapping) entityMappingType.getEntityPersister() )
					.toType( referencedPropertyName );
			ToOneAttributeMapping.addPrefixedPropertyNames(
					targetKeyPropertyNames,
					referencedPropertyName,
					propertyType,
					creationProcess.getCreationContext().getSessionFactory()
			);
			ToOneAttributeMapping.addPrefixedPropertyNames(
					targetKeyPropertyNames,
					ForeignKeyDescriptor.PART_NAME,
					propertyType,
					creationProcess.getCreationContext().getSessionFactory()
			);
			this.targetKeyPropertyNames = targetKeyPropertyNames;
		}
		else {
			final Type propertyType = entityBinding.getRecursiveProperty( referencedPropertyName ).getType();
			final CompositeType compositeType;
			if ( propertyType.isComponentType() && ( compositeType = (CompositeType) propertyType ).isEmbedded()
					&& compositeType.getPropertyNames().length == 1 ) {
				final Set<String> targetKeyPropertyNames = new HashSet<>( 2 );
				ToOneAttributeMapping.addPrefixedPropertyNames(
						targetKeyPropertyNames,
						compositeType.getPropertyNames()[0],
						compositeType.getSubtypes()[0],
						creationProcess.getCreationContext().getSessionFactory()
				);
				ToOneAttributeMapping.addPrefixedPropertyNames(
						targetKeyPropertyNames,
						ForeignKeyDescriptor.PART_NAME,
						compositeType.getSubtypes()[0],
						creationProcess.getCreationContext().getSessionFactory()
				);
				this.targetKeyPropertyNames = targetKeyPropertyNames;
			}
			else {
				final String mapsIdAttributeName;
				if ( ( mapsIdAttributeName = ToOneAttributeMapping.findMapsIdPropertyName( entityMappingType, referencedPropertyName ) ) != null ) {
					final Set<String> targetKeyPropertyNames = new HashSet<>( 2 );
					targetKeyPropertyNames.add( referencedPropertyName );
					ToOneAttributeMapping.addPrefixedPropertyNames(
							targetKeyPropertyNames,
							mapsIdAttributeName,
							entityMappingType.getEntityPersister().getIdentifierType(),
							creationProcess.getCreationContext().getSessionFactory()
					);
					ToOneAttributeMapping.addPrefixedPropertyNames(
							targetKeyPropertyNames,
							ForeignKeyDescriptor.PART_NAME,
							entityMappingType.getEntityPersister().getIdentifierType(),
							creationProcess.getCreationContext().getSessionFactory()
					);
					this.targetKeyPropertyNames = targetKeyPropertyNames;
				}
				else {
					this.targetKeyPropertyNames = Set.of( referencedPropertyName, ForeignKeyDescriptor.PART_NAME );
				}
			}
		}
	}

	public void finishInitialization(
			CollectionPersister collectionDescriptor,
			Collection bootValueMapping,
			String fkTargetModelPartName,
			MappingModelCreationProcess creationProcess) {
		if ( fkTargetModelPartName == null ) {
			if ( nature == Nature.INDEX ) {
				final String mapKeyPropertyName = ( (Map) bootValueMapping ).getMapKeyPropertyName();
				if ( mapKeyPropertyName == null ) {
					fkTargetModelPart = entityMappingType.getIdentifierMapping();
				}
				else {
					final EntityPersister elementPersister = ( (EntityType) collectionDescriptor.getElementType() )
							.getAssociatedEntityPersister( creationProcess.getCreationContext().getSessionFactory() );
					fkTargetModelPart = elementPersister.findByPath( mapKeyPropertyName );
					if ( fkTargetModelPart == null ) {
						// This is expected to happen when processing a
						// PostInitCallbackEntry because the callbacks
						// are not ordered. The exception is caught in
						// MappingModelCreationProcess.executePostInitCallbacks()
						// and the callback is re-queued.
						throw new IllegalStateException( "Couldn't find model part for path [" + mapKeyPropertyName + "] on entity: " + elementPersister.getEntityName() );
					}
				}
			}
			else {
				final String mappedByProperty = bootValueMapping.getMappedByProperty();
				if ( collectionDescriptor.isOneToMany() && mappedByProperty != null && !mappedByProperty.isEmpty() ) {
					fkTargetModelPart = entityMappingType.findByPath( mappedByProperty );
					if ( fkTargetModelPart == null ) {
						// This is expected to happen when processing a
						// PostInitCallbackEntry because the callbacks
						// are not ordered. The exception is caught in
						// MappingModelCreationProcess.executePostInitCallbacks()
						// and the callback is re-queued.
						throw new IllegalStateException( "Couldn't find model part for path [" + mappedByProperty + "] on entity: " + entityMappingType.getEntityName() );
					}
				}
				else {
					fkTargetModelPart = entityMappingType.getIdentifierMapping();
				}
			}
		}
		else {
			fkTargetModelPart = entityMappingType.findSubPart( fkTargetModelPartName, null );
		}
		if ( nature == Nature.ELEMENT ) {
			fkDescriptor = createForeignKeyDescriptor(
					bootValueMapping.getElement(),
					(EntityType) collectionDescriptor.getElementType(),
					creationProcess,
					collectionDescriptor.getFactory().getJdbcServices().getDialect()
			);
		}
		else {
			fkDescriptor = createForeignKeyDescriptor(
					( (IndexedCollection) bootValueMapping).getIndex(),
					(EntityType) collectionDescriptor.getIndexType(),
					creationProcess,
					collectionDescriptor.getFactory().getJdbcServices().getDialect()
			);
		}
	}

	private ForeignKeyDescriptor createForeignKeyDescriptor(
			Value fkBootDescriptorSource,
			EntityType entityType,
			MappingModelCreationProcess creationProcess,
			Dialect dialect) {
		final EntityPersister associatedEntityDescriptor =  creationProcess.getEntityPersister( entityType.getAssociatedEntityName() );
		// If this is mapped by a to-one attribute, we can use the FK of that attribute
		if ( fkTargetModelPart instanceof ToOneAttributeMapping ) {
			final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) fkTargetModelPart;
			if ( toOneAttributeMapping.getForeignKeyDescriptor() == null ) {
				throw new IllegalStateException( "Not yet ready: " + toOneAttributeMapping );
			}
			return toOneAttributeMapping.getForeignKeyDescriptor();
		}
		final ModelPart fkTargetPart = fkTargetModelPart;

		final String fkKeyTableName;
		if ( nature == Nature.INDEX ) {
			final String indexPropertyName = collectionDescriptor.getAttributeMapping()
					.getIndexMetadata()
					.getIndexPropertyName();
			if ( indexPropertyName == null ) {
				fkKeyTableName = ( (Joinable) collectionDescriptor ).getTableName();
			}
			else {
				fkKeyTableName = fkBootDescriptorSource.getTable().getQuotedName( dialect );
			}
		}
		else {
			fkKeyTableName = ( (Joinable) collectionDescriptor ).getTableName();
		}
		if ( fkTargetPart instanceof BasicValuedModelPart ) {
			final BasicValuedModelPart basicFkTargetPart = (BasicValuedModelPart) fkTargetPart;
			final SelectableMapping keySelectableMapping = SelectableMappingImpl.from(
					fkKeyTableName,
					fkBootDescriptorSource.getSelectables().get(0),
					basicFkTargetPart.getJdbcMapping(),
					creationProcess.getCreationContext().getTypeConfiguration(),
					dialect,
					creationProcess.getSqmFunctionRegistry()
			);
			final boolean hasConstraint;
			if ( fkBootDescriptorSource instanceof SimpleValue ) {
				hasConstraint = ( (SimpleValue) fkBootDescriptorSource ).isConstrained();
			}
			else {
				// We assume there is a constraint if the key is not nullable
				hasConstraint = !fkBootDescriptorSource.isNullable();
			}
			return new SimpleForeignKeyDescriptor(
					associatedEntityDescriptor,
					basicFkTargetPart,
					null,
					keySelectableMapping,
					basicFkTargetPart,
					entityType.isReferenceToPrimaryKey(),
					hasConstraint
			);
		}
		else if ( fkTargetPart instanceof EmbeddableValuedModelPart ) {
			return MappingModelCreationHelper.buildEmbeddableForeignKeyDescriptor(
					(EmbeddableValuedModelPart) fkTargetPart,
					fkBootDescriptorSource,
					findContainingEntityMapping(),
					collectionDescriptor.getAttributeMapping(),
					false,
					dialect,
					creationProcess
			);
		}
		else {
			throw new NotYetImplementedFor6Exception(
					"Support for composite foreign keys not yet implemented : " + collectionDescriptor
							.getRole()
			);
		}
	}

	@Override
	public SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup) {
		return SqlAstJoinType.INNER;
	}

	@Override
	public boolean isSimpleJoinPredicate(Predicate predicate) {
		return fkDescriptor.isSimpleJoinPredicate( predicate );
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
		return collectionDescriptor.isOneToMany()
				? entityMappingType.getIdentifierMapping()
				: fkTargetModelPart;
	}

	@Override
	public JavaType<?> getJavaType() {
		return getEntityMappingType().getJavaType();
	}

	@Override
	public JavaType<?> getExpressibleJavaType() {
		return getJavaType();
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
	public boolean incrementFetchDepth() {
		// the collection itself already increments the depth
		return false;
	}

	@Override
	public ModelPart findSubPart(String name) {
		return findSubPart( name, null );
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType targetType) {
		// Prefer resolving the key part of the foreign key rather than the target part if possible
		// to allow deferring the initialization of the target table group, omitting it if possible.
		// This is not possible for one-to-many associations because we need to create the target table group eagerly,
		// to preserve the cardinality. Also, the OneToManyTableGroup has no reference to the parent table group
		if ( !collectionDescriptor.isOneToMany() && targetKeyPropertyNames.contains( name ) ) {
			if ( fkTargetModelPart instanceof ToOneAttributeMapping ) {
				return fkTargetModelPart;
			}
			final ModelPart keyPart = fkDescriptor.getKeyPart();
			if ( keyPart instanceof EmbeddableValuedModelPart && keyPart instanceof VirtualModelPart ) {
				return ( (ModelPartContainer) keyPart ).findSubPart( name, targetType );
			}
			return keyPart;
		}
		return EntityValuedFetchable.super.findSubPart( name, targetType );
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
		final TableGroup partTableGroup = resolveTableGroup( navigablePath, creationState );
		return entityMappingType.createDomainResult( navigablePath, partTableGroup, resultVariable, creationState );
	}

	@Override
	public EntityFetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		final ForeignKeyDescriptor foreignKeyDescriptor = getForeignKeyDescriptor();
		final boolean added = creationState.registerVisitedAssociationKey( foreignKeyDescriptor.getAssociationKey() );

		final TableGroup partTableGroup = resolveTableGroup( fetchablePath, creationState );
		final EntityFetchJoinedImpl fetch = new EntityFetchJoinedImpl(
				fetchParent,
				this,
				partTableGroup,
				fetchablePath,
				creationState
		);

		if ( added ) {
			creationState.removeVisitedAssociationKey( foreignKeyDescriptor.getAssociationKey() );
		}

		return fetch;
	}

	private TableGroup resolveTableGroup(NavigablePath fetchablePath, DomainResultCreationState creationState) {
		final FromClauseAccess fromClauseAccess = creationState.getSqlAstCreationState().getFromClauseAccess();
		return fromClauseAccess.resolveTableGroup(
				fetchablePath,
				np -> {
					final PluralTableGroup parentTableGroup = (PluralTableGroup) fromClauseAccess.getTableGroup( np.getParent() );
					switch ( nature ) {
						case ELEMENT:
							return parentTableGroup.getElementTableGroup();
						case INDEX:
							return parentTableGroup.getIndexTableGroup();
					}

					throw new IllegalStateException( "Could not find table group for: " + np );
				}
		);
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
		return fkDescriptor;
	}

	@Override
	public ForeignKeyDescriptor.Nature getSideNature() {
		return collectionDescriptor.isOneToMany()
				? ForeignKeyDescriptor.Nature.TARGET
				: ForeignKeyDescriptor.Nature.KEY;
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
			TableGroup collectionTableGroup,
			String explicitSourceAlias,
			SqlAstJoinType requestedJoinType,
			boolean fetched,
			boolean addsPredicate,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext) {
		final SqlAstJoinType joinType = requireNonNullElse( requestedJoinType, SqlAstJoinType.INNER );

		if ( collectionDescriptor.isOneToMany() && nature == Nature.ELEMENT ) {
			// If this is a one-to-many, the element part is already available, so we return a TableGroupJoin "hull"
			return new TableGroupJoin(
					navigablePath,
					joinType,
					( (OneToManyTableGroup) collectionTableGroup ).getElementTableGroup(),
					null
			);
		}

		final LazyTableGroup lazyTableGroup = createRootTableGroupJoin(
				navigablePath,
				collectionTableGroup,
				explicitSourceAlias,
				requestedJoinType,
				fetched,
				null,
				aliasBaseGenerator,
				sqlExpressionResolver,
				fromClauseAccess,
				creationContext
		);
		final TableGroupJoin join = new TableGroupJoin(
				navigablePath,
				joinType,
				lazyTableGroup,
				null
		);

		lazyTableGroup.setTableGroupInitializerCallback(
				tableGroup -> join.applyPredicate(
						fkDescriptor.generateJoinPredicate(
								tableGroup.getPrimaryTableReference(),
								collectionTableGroup.resolveTableReference( fkDescriptor.getKeyTable() ),
								sqlExpressionResolver,
								creationContext
						)
				)
		);

		return join;
	}

	@Override
	public LazyTableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType requestedJoinType,
			boolean fetched,
			Consumer<Predicate> predicateConsumer,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext) {
		final SqlAstJoinType joinType = requireNonNullElse( requestedJoinType, SqlAstJoinType.INNER );
		final SqlAliasBase sqlAliasBase = aliasBaseGenerator.createSqlAliasBase( getSqlAliasStem() );
		final boolean canUseInnerJoin = joinType == SqlAstJoinType.INNER || lhs.canUseInnerJoins();

		final LazyTableGroup lazyTableGroup = new LazyTableGroup(
				canUseInnerJoin,
				navigablePath,
				fetched,
				() -> createTableGroupInternal(
						canUseInnerJoin,
						navigablePath,
						fetched,
						null,
						sqlAliasBase,
						sqlExpressionResolver,
						creationContext
				),
				(np, tableExpression) -> {
					if ( ! fkDescriptor.getKeyTable().equals( tableExpression ) ) {
						return false;
					}

					if ( navigablePath.equals( np.getParent() ) ) {
						return targetKeyPropertyNames.contains( np.getLocalName() );
					}

					final String relativePath = np.relativize( navigablePath );
					if ( relativePath == null ) {
						return false;
					}

					return targetKeyPropertyNames.contains( relativePath );
				},
				this,
				explicitSourceAlias,
				sqlAliasBase,
				creationContext.getSessionFactory(),
				lhs
		);

		if ( predicateConsumer != null ) {
			final TableReference keySideTableReference = lhs.resolveTableReference(
					navigablePath,
					fkDescriptor.getKeyTable()
			);

			lazyTableGroup.setTableGroupInitializerCallback(
					tableGroup -> predicateConsumer.accept(
							fkDescriptor.generateJoinPredicate(
									tableGroup.getPrimaryTableReference(),
									keySideTableReference,
									sqlExpressionResolver,
									creationContext
							)
					)
			);
		}

		return lazyTableGroup;
	}

	public TableGroup createTableGroupInternal(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			boolean fetched,
			String sourceAlias,
			final SqlAliasBase sqlAliasBase,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final TableReference primaryTableReference = getEntityMappingType().createPrimaryTableReference(
				sqlAliasBase,
				sqlExpressionResolver,
				creationContext
		);

		return new StandardTableGroup(
				canUseInnerJoins,
				navigablePath,
				this,
				fetched,
				sourceAlias,
				primaryTableReference,
				true,
				sqlAliasBase,
				(tableExpression) -> getEntityMappingType().containsTableReference( tableExpression ),
				(tableExpression, tg) -> getEntityMappingType().createTableReferenceJoin(
						tableExpression,
						sqlAliasBase,
						primaryTableReference,
						sqlExpressionResolver,
						creationContext
				),
				creationContext.getSessionFactory()
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
