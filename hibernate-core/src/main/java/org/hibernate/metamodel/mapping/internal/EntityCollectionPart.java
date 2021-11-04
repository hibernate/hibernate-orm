/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.OneToMany;
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
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.LazyTableGroup;
import org.hibernate.sql.ast.tree.from.OneToManyTableGroup;
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
import org.hibernate.tuple.IdentifierProperty;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
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
	private final Set<String> targetKeyPropertyNames;

	private ModelPart fkTargetModelPart;
	private ForeignKeyDescriptor fkDescriptor;

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
		final String referencedPropertyName;
		if ( bootModelValue instanceof OneToMany ) {
			referencedPropertyName = null;
		}
		else {
			referencedPropertyName = ( (ToOne) bootModelValue ).getReferencedPropertyName();
		}
		if ( referencedPropertyName == null ) {
			final Set<String> targetKeyPropertyNames = new HashSet<>( 2 );
			targetKeyPropertyNames.add( EntityIdentifierMapping.ROLE_LOCAL_NAME );
			final IdentifierProperty identifierProperty = getEntityMappingType()
					.getEntityPersister()
					.getEntityMetamodel()
					.getIdentifierProperty();
			final Type propertyType = identifierProperty.getType();
			if ( identifierProperty.getName() == null ) {
				final CompositeType compositeType;
				if ( propertyType.isComponentType() && ( compositeType = (CompositeType) propertyType ).isEmbedded()
						&& compositeType.getPropertyNames().length == 1 ) {
					ToOneAttributeMapping.addPrefixedPropertyNames(
							targetKeyPropertyNames,
							compositeType.getPropertyNames()[0],
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
				}
			}
			else {
				ToOneAttributeMapping.addPrefixedPropertyNames(
						targetKeyPropertyNames,
						identifierProperty.getName(),
						propertyType,
						creationProcess.getCreationContext().getSessionFactory()
				);
			}
			this.targetKeyPropertyNames = targetKeyPropertyNames;
		}
		else if ( bootModelValue instanceof OneToMany ) {
			this.targetKeyPropertyNames = Collections.singleton( referencedPropertyName );
		}
		else {
			final EntityMetamodel entityMetamodel = entityMappingType.getEntityPersister().getEntityMetamodel();
			final int propertyIndex = entityMetamodel.getPropertyIndex( referencedPropertyName );
			final Type propertyType = entityMetamodel.getPropertyTypes()[propertyIndex];
			final CompositeType compositeType;
			if ( propertyType.isComponentType() && ( compositeType = (CompositeType) propertyType ).isEmbedded()
					&& compositeType.getPropertyNames().length == 1 ) {
				this.targetKeyPropertyNames = Collections.singleton( compositeType.getPropertyNames()[0] );
			}
			else {
				final String mapsIdAttributeName;
				if ( ( mapsIdAttributeName = ToOneAttributeMapping.mapsId( entityMappingType, referencedPropertyName ) ) != null ) {
					final Set<String> targetKeyPropertyNames = new HashSet<>( 2 );
					targetKeyPropertyNames.add( referencedPropertyName );
					ToOneAttributeMapping.addPrefixedPropertyNames(
							targetKeyPropertyNames,
							mapsIdAttributeName,
							entityMappingType.getEntityPersister().getIdentifierType(),
							creationProcess.getCreationContext().getSessionFactory()
					);
					this.targetKeyPropertyNames = targetKeyPropertyNames;
				}
				else {
					this.targetKeyPropertyNames = Collections.singleton( referencedPropertyName );
				}
			}
		}
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
		final ModelPart fkTargetPart = entityType.isReferenceToPrimaryKey()
				? associatedEntityDescriptor.getIdentifierMapping()
				: associatedEntityDescriptor.findSubPart( entityType.getRHSUniqueKeyPropertyName() );

		if ( fkTargetPart instanceof BasicValuedModelPart ) {
			final BasicValuedModelPart basicFkTargetPart = (BasicValuedModelPart) fkTargetPart;
			final Joinable collectionDescriptorAsJoinable = (Joinable) collectionDescriptor;
			final SelectableMapping keySelectableMapping = SelectableMappingImpl.from(
					collectionDescriptorAsJoinable.getTableName(),
					fkBootDescriptorSource.getColumnIterator().next(),
					basicFkTargetPart.getJdbcMapping(),
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

		final TableGroup partTableGroup = fromClauseAccess.resolveTableGroup(
				fetchablePath,
				np -> {
					final TableGroup parentTableGroup = fromClauseAccess.getTableGroup( np.getParent() );
					if ( collectionDescriptor.isOneToMany() && nature == Nature.ELEMENT ) {
						return ( (OneToManyTableGroup) parentTableGroup ).getElementTableGroup();
					}
					for ( TableGroupJoin nestedTableGroupJoin : parentTableGroup.getNestedTableGroupJoins() ) {
						if ( nestedTableGroupJoin.getNavigablePath().equals( np ) ) {
							return nestedTableGroupJoin.getJoinedGroup();
						}
					}

					throw new IllegalStateException( "Could not find table group for: " + np );
				}
		);

		return new EntityFetchJoinedImpl( fetchParent, this, partTableGroup, selected, fetchablePath, creationState );
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
					final TableGroup parentTableGroup = fromClauseAccess.getTableGroup( np.getParent() );
					if ( collectionDescriptor.isOneToMany() && nature == Nature.ELEMENT ) {
						return ( (OneToManyTableGroup) parentTableGroup ).getElementTableGroup();
					}
					final TableGroupJoin tableGroupJoin = createTableGroupJoin(
							navigablePath,
							parentTableGroup,
							resultVariable,
							SqlAstJoinType.INNER,
							true,
							creationState.getSqlAstCreationState()
					);
					parentTableGroup.addTableGroupJoin( tableGroupJoin );
					return tableGroupJoin.getJoinedGroup();
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
		return fkDescriptor;
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
			TableGroup collectionTableGroup,
			String explicitSourceAlias,
			SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		if ( collectionDescriptor.isOneToMany() && nature == Nature.ELEMENT ) {
			// If this is a one-to-many, the element part is already available, so we return a TableGroupJoin "hull"
			return new TableGroupJoin(
					navigablePath,
					sqlAstJoinType,
					( (OneToManyTableGroup) collectionTableGroup ).getElementTableGroup(),
					null
			);
		}

		final LazyTableGroup lazyTableGroup = createRootTableGroupJoin(
				navigablePath,
				collectionTableGroup,
				explicitSourceAlias,
				sqlAstJoinType,
				fetched,
				null,
				aliasBaseGenerator,
				sqlExpressionResolver,
				creationContext
		);
		final TableGroupJoin join = new TableGroupJoin(
				navigablePath,
				sqlAstJoinType,
				lazyTableGroup,
				null
		);

		final TableReference keySideTableReference = collectionTableGroup.getPrimaryTableReference();

		lazyTableGroup.setTableGroupInitializerCallback(
				tableGroup -> join.applyPredicate(
						fkDescriptor.generateJoinPredicate(
								tableGroup.getPrimaryTableReference(),
								keySideTableReference,
								sqlAstJoinType,
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
			SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			Consumer<Predicate> predicateConsumer,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final SqlAliasBase sqlAliasBase = aliasBaseGenerator.createSqlAliasBase( getSqlAliasStem() );
		final boolean canUseInnerJoin = sqlAstJoinType == SqlAstJoinType.INNER || lhs.canUseInnerJoins();
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
					NavigablePath path = np.getParent();
					// Fast path
					if ( path != null && navigablePath.equals( path ) ) {
						return targetKeyPropertyNames.contains( np.getUnaliasedLocalName() )
								&& fkDescriptor.getKeyTable().equals( tableExpression );
					}
					final StringBuilder sb = new StringBuilder( np.getFullPath().length() );
					sb.append( np.getUnaliasedLocalName() );
					while ( path != null && !navigablePath.equals( path ) ) {
						sb.insert( 0, '.' );
						sb.insert( 0, path.getUnaliasedLocalName() );
						path = path.getParent();
					}
					return path != null && navigablePath.equals( path )
							&& targetKeyPropertyNames.contains( sb.toString() )
							&& fkDescriptor.getKeyTable().equals( tableExpression );
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
									sqlAstJoinType,
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
				false,
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
