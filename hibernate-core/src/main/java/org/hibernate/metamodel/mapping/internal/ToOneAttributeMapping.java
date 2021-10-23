/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;
import org.hibernate.metamodel.mapping.AssociationKey;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.EntityIdentifierNavigablePath;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAliasStemHelper;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.LazyTableGroup;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;
import org.hibernate.sql.results.graph.entity.internal.EntityDelayedFetchImpl;
import org.hibernate.sql.results.graph.entity.internal.EntityDelayedResultImpl;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchJoinedImpl;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchSelectImpl;
import org.hibernate.sql.results.graph.entity.internal.EntityResultImpl;
import org.hibernate.sql.results.graph.entity.internal.EntityResultJoinedSubclassImpl;
import org.hibernate.sql.results.internal.domain.CircularBiDirectionalFetchImpl;
import org.hibernate.sql.results.internal.domain.CircularFetchImpl;
import org.hibernate.tuple.IdentifierProperty;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class ToOneAttributeMapping
		extends AbstractSingularAttributeMapping
		implements EntityValuedFetchable, EntityAssociationMapping, TableGroupJoinProducer {

	public enum Cardinality {
		ONE_TO_ONE,
		MANY_TO_ONE,
		LOGICAL_ONE_TO_ONE
	}

	private final NavigableRole navigableRole;

	private final String sqlAliasStem;
	private final boolean isNullable;
	private final boolean isConstrained;
	private final boolean isIgnoreNotFound;
	private final boolean unwrapProxy;
	private final boolean isOptional;
	private final EntityMappingType entityMappingType;

	private final String referencedPropertyName;
	private final String targetKeyPropertyName;
	private final Set<String> targetKeyPropertyNames;

	private final Cardinality cardinality;
	private final String bidirectionalAttributeName;
	private final TableGroupProducer declaringTableGroupProducer;

	private ForeignKeyDescriptor foreignKeyDescriptor;
	private ForeignKeyDescriptor.Nature sideNature;
	private String identifyingColumnsTableExpression;
	private boolean canUseParentTableGroup;


	public ToOneAttributeMapping(
			String name,
			NavigableRole navigableRole,
			int stateArrayPosition,
			ToOne bootValue,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			FetchOptions mappedFetchOptions,
			EntityMappingType entityMappingType,
			ManagedMappingType declaringType,
			EntityPersister declaringEntityPersister,
			PropertyAccess propertyAccess) {
		this(
				name,
				navigableRole,
				stateArrayPosition,
				bootValue,
				attributeMetadataAccess,
				mappedFetchOptions.getTiming(),
				mappedFetchOptions.getStyle(),
				entityMappingType,
				declaringType,
				declaringEntityPersister,
				propertyAccess
		);
	}

	public ToOneAttributeMapping(
			String name,
			NavigableRole navigableRole,
			int stateArrayPosition,
			ToOne bootValue,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			FetchTiming mappedFetchTiming,
			FetchStyle mappedFetchStyle,
			EntityMappingType entityMappingType,
			ManagedMappingType declaringType,
			EntityPersister declaringEntityPersister,
			PropertyAccess propertyAccess) {
		super(
				name,
				stateArrayPosition,
				attributeMetadataAccess,
				mappedFetchTiming,
				mappedFetchStyle,
				declaringType,
				propertyAccess,
				// can never be a generated value
				NoValueGeneration.INSTANCE
		);
		this.sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( name );
		this.isNullable = bootValue.isNullable();
		this.referencedPropertyName = bootValue.getReferencedPropertyName();
		this.unwrapProxy = bootValue.isUnwrapProxy();
		this.entityMappingType = entityMappingType;

		if ( bootValue instanceof ManyToOne ) {
			final ManyToOne manyToOne = (ManyToOne) bootValue;
			this.isIgnoreNotFound = ( (ManyToOne) bootValue ).isIgnoreNotFound();
			if ( manyToOne.isLogicalOneToOne() ) {
				cardinality = Cardinality.LOGICAL_ONE_TO_ONE;
			}
			else {
				cardinality = Cardinality.MANY_TO_ONE;
			}
			if ( referencedPropertyName == null ) {
				String bidirectionalAttributeName = null;
				final PersistentClass entityBinding = manyToOne.getMetadata().getEntityBinding( manyToOne.getReferencedEntityName() );
				final Iterator<Property> propertyClosureIterator = entityBinding.getPropertyClosureIterator();
				while (propertyClosureIterator.hasNext()) {
					final Property property = propertyClosureIterator.next();
					if ( property.getValue() instanceof Collection && name.equals( ( (Collection) property.getValue() ).getMappedByProperty() ) ) {
						bidirectionalAttributeName = property.getName();
						break;
					}
				}
				this.bidirectionalAttributeName = bidirectionalAttributeName;
			}
			else {
				this.bidirectionalAttributeName = referencedPropertyName;
			}
			isOptional = ( (ManyToOne) bootValue ).isIgnoreNotFound();
		}
		else {
			assert bootValue instanceof OneToOne;
			cardinality = Cardinality.ONE_TO_ONE;

			/*
		 		The otherSidePropertyName value is used to determine bidirectionality based on the navigablePath string

		 		e.g.

				class Card{
					@OneToMany( mappedBy = "card")
					Set<CardField> fields;
				}

				class CardField{
					@ManyToOne(optional = false)
					Card card;

					@ManyToOne(optional = false)
					Card card1;
				}

				NavigablePath(CardField.card.fields)  fields is consideredBidirectional
				NavigablePath(CardField.card1.fields) fields is NOT bidirectional

				e.g. Embeddable case

				class Card{
					@OneToMany( mappedBy = "primaryKey.card")
					Set<CardField> fields;
				}

				class CardField{
					@EmbeddedId
					PrimaryKey primaryKey;
				}

				@Embeddable
				class PrimaryKey implements Serializable {
					@ManyToOne(optional = false)
					Card card;
				}

				in such case the mappedBy is "primaryKey.card"
				the navigable path is NavigablePath(Card.fields.{element}.{id}.card) and it does not contain the "primaryKey" part,
				so in order to recognize the bidirectionality the "primaryKey." is removed from the otherSidePropertyName value.
		 	*/
			// todo (6.0): find a better solution for the embeddable part name not in the NavigablePath
			final OneToOne oneToOne = (OneToOne) bootValue;
			String bidirectionalAttributeName = StringHelper.subStringNullIfEmpty(
					oneToOne.getMappedByProperty(),
					'.'
			);

			if ( bidirectionalAttributeName == null ) {
				this.bidirectionalAttributeName = StringHelper.subStringNullIfEmpty(
						referencedPropertyName,
						'.'
				);
			}
			else {
				this.bidirectionalAttributeName = bidirectionalAttributeName;
			}
			isIgnoreNotFound = isNullable();
			isOptional = ! bootValue.isConstrained();
		}
		isConstrained = bootValue.isConstrained();

		this.navigableRole = navigableRole;
		final CollectionPart.Nature nature = CollectionPart.Nature.fromNameExact(
				getNavigableRole().getParent().getLocalName()
		);
		if ( nature == null ) {
			// This is a simple to-one association
			this.declaringTableGroupProducer = declaringEntityPersister;
		}
		else {
			// This is a collection part i.e. to-many association
			final String collectionRoleName = getNavigableRole().getParent().getParent().getLocalName();
			this.declaringTableGroupProducer = ( (PluralAttributeMapping) declaringEntityPersister.findAttributeMapping(
					collectionRoleName.substring( collectionRoleName.lastIndexOf( '.' ) + 1 )
			) );
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
					this.targetKeyPropertyName = compositeType.getPropertyNames()[0];
					addPrefixedPropertyNames( targetKeyPropertyNames, targetKeyPropertyName, compositeType.getSubtypes()[0] );
				}
				else {
					this.targetKeyPropertyName = EntityIdentifierMapping.ROLE_LOCAL_NAME;
					addPrefixedPropertyNames( targetKeyPropertyNames, null, propertyType );
				}
			}
			else {
				this.targetKeyPropertyName = identifierProperty.getName();
				addPrefixedPropertyNames( targetKeyPropertyNames, targetKeyPropertyName, propertyType );
			}
			this.targetKeyPropertyNames = targetKeyPropertyNames;
		}
		else if ( bootValue.isReferenceToPrimaryKey() ) {
			this.targetKeyPropertyName = referencedPropertyName;
			this.targetKeyPropertyNames = Collections.singleton( targetKeyPropertyName );
		}
		else {
			final EntityMetamodel entityMetamodel = entityMappingType.getEntityPersister().getEntityMetamodel();
			final int propertyIndex = entityMetamodel.getPropertyIndex( referencedPropertyName );
			final Type propertyType = entityMetamodel.getPropertyTypes()[propertyIndex];
			final CompositeType compositeType;
			if ( propertyType.isComponentType() && ( compositeType = (CompositeType) propertyType ).isEmbedded()
					&& compositeType.getPropertyNames().length == 1 ) {
				this.targetKeyPropertyName = compositeType.getPropertyNames()[0];
				this.targetKeyPropertyNames = Collections.singleton( targetKeyPropertyName );
			}
			else {
				this.targetKeyPropertyName = referencedPropertyName;
				this.targetKeyPropertyNames = Collections.singleton( targetKeyPropertyName );
			}
		}
	}

	private ToOneAttributeMapping(ToOneAttributeMapping original) {
		super(
				original.getAttributeName(),
				original.getStateArrayPosition(),
				original.getAttributeMetadataAccess(),
				original,
				original.getDeclaringType(),
				original.getPropertyAccess(),
				original.getValueGeneration()
		);
		this.navigableRole = original.navigableRole;
		this.sqlAliasStem = original.sqlAliasStem;
		this.isNullable = original.isNullable;
		this.isOptional = original.isOptional;
		this.isIgnoreNotFound = original.isIgnoreNotFound;
		this.unwrapProxy = original.unwrapProxy;
		this.entityMappingType = original.entityMappingType;
		this.referencedPropertyName = original.referencedPropertyName;
		this.targetKeyPropertyName = original.targetKeyPropertyName;
		this.targetKeyPropertyNames = original.targetKeyPropertyNames;
		this.cardinality = original.cardinality;
		this.bidirectionalAttributeName = original.bidirectionalAttributeName;
		this.declaringTableGroupProducer = original.declaringTableGroupProducer;
		this.isConstrained = original.isConstrained;
	}

	private static void addPrefixedPropertyNames(
			Set<String> targetKeyPropertyNames,
			String prefix,
			Type type) {
		if ( prefix != null ) {
			targetKeyPropertyNames.add( prefix );
		}
		if ( type.isComponentType() ) {
			final ComponentType componentType = (ComponentType) type;
			final String[] propertyNames = componentType.getPropertyNames();
			final Type[] componentTypeSubtypes = componentType.getSubtypes();
			for ( int i = 0, propertyNamesLength = propertyNames.length; i < propertyNamesLength; i++ ) {
				final String newPrefix;
				if ( prefix == null ) {
					newPrefix = propertyNames[i];
				}
				else {
					newPrefix = prefix + "." + propertyNames[i];
				}
				addPrefixedPropertyNames( targetKeyPropertyNames, newPrefix, componentTypeSubtypes[i] );
			}
		}
	}

	public ToOneAttributeMapping copy() {
		return new ToOneAttributeMapping( this );
	}

	@Override
	public void setForeignKeyDescriptor(ForeignKeyDescriptor foreignKeyDescriptor) {
		assert identifyingColumnsTableExpression != null;
		this.foreignKeyDescriptor = foreignKeyDescriptor;
		this.sideNature = foreignKeyDescriptor.getAssociationKey().getTable().equals( identifyingColumnsTableExpression )
				? ForeignKeyDescriptor.Nature.KEY
				: ForeignKeyDescriptor.Nature.TARGET;

		// We can only use the parent table group if the FK is located there and ignoreNotFound is false
		// If this is not the case, the FK is not constrained or on a join/secondary table, so we need a join
		this.canUseParentTableGroup = !isIgnoreNotFound && sideNature == ForeignKeyDescriptor.Nature.KEY
				&& declaringTableGroupProducer.containsTableReference( identifyingColumnsTableExpression );
	}

	public void setIdentifyingColumnsTableExpression(String tableExpression) {
		identifyingColumnsTableExpression = tableExpression;
	}

	@Override
	public ForeignKeyDescriptor getForeignKeyDescriptor() {
		return this.foreignKeyDescriptor;
	}

	@Override
	public ForeignKeyDescriptor.Nature getSideNature() {
		return sideNature;
	}

	public boolean canJoinForeignKey(EntityIdentifierMapping identifierMapping) {
		return sideNature == ForeignKeyDescriptor.Nature.KEY && identifierMapping == getForeignKeyDescriptor().getTargetPart() && !isNullable;
	}

	public String getReferencedPropertyName() {
		return referencedPropertyName;
	}

	public String getTargetKeyPropertyName() {
		return targetKeyPropertyName;
	}

	public boolean isTargetKeyPropertyPath(String path) {
		return targetKeyPropertyNames.contains( path );
	}

	public Cardinality getCardinality() {
		return cardinality;
	}

	@Override
	public EntityMappingType getMappedType() {
		return getEntityMappingType();
	}

	@Override
	public EntityMappingType getEntityMappingType() {
		return entityMappingType;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public ModelPart findSubPart(String name) {
		return findSubPart( name, null );
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType targetType) {
		// Prefer resolving the key part of the foreign key rather than the target part if possible
		// This way, we don't have to register table groups the target entity type
		if ( canUseParentTableGroup && name.equals( targetKeyPropertyName ) ) {
			return foreignKeyDescriptor.getKeyPart();
		}
		return EntityValuedFetchable.super.findSubPart( name, targetType );
	}

	@Override
	public Fetch resolveCircularFetch(
			NavigablePath fetchablePath,
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			DomainResultCreationState creationState) {
		final AssociationKey associationKey = foreignKeyDescriptor.getAssociationKey();

		if ( creationState.isAssociationKeyVisited( associationKey )
				|| bidirectionalAttributeName != null && !creationState.isRegisteringVisitedAssociationKeys() ) {
			NavigablePath parentNavigablePath = fetchablePath.getParent();
			assert parentNavigablePath.equals( fetchParent.getNavigablePath() );
			// The parent navigable path is {fk} if we are creating the domain result for the foreign key for a circular fetch
			// In the following example, we create a circular fetch for the composite `Card.field.{id}.card.field`
			// While creating the domain result for the foreign key of `Card#field`, we run into this condition
			// We know that `Card#field` will be delayed because `EmbeddableForeignKeyResultImpl` enforces that
			// so we can safely return null to avoid a stack overflow
			/*
				@Entity
				public class Card {
					@Id
					private String id;
					@ManyToOne
					private CardField field;
				}
				@Entity
				public class CardField {
					@EmbeddedId
					private PrimaryKey primaryKey;
				}
				@Embeddable
				public class PrimaryKey {
					@ManyToOne(optional = false)
					private Card card;
					@ManyToOne(optional = false)
					private Key key;
				}
			 */
			if ( parentNavigablePath.getLocalName().equals( ForeignKeyDescriptor.PART_NAME ) ) {
				// todo (6.0): maybe it's better to have a flag in creation state that marks if we are building a circular fetch domain result already to skip this?
				return null;
			}

			ModelPart parentModelPart = creationState.resolveModelPart( parentNavigablePath );
			if ( parentModelPart instanceof EmbeddedIdentifierMappingImpl ) {
				while ( parentNavigablePath instanceof EntityIdentifierNavigablePath ) {
					parentNavigablePath = parentNavigablePath.getParent();
					assert parentNavigablePath != null;
					parentModelPart = creationState.resolveModelPart( parentNavigablePath );
				}
			}
			while ( parentModelPart instanceof EmbeddableValuedFetchable ) {
				parentNavigablePath = parentNavigablePath.getParent();
				assert parentNavigablePath != null;
				parentModelPart = creationState.resolveModelPart( parentNavigablePath );
			}

			if ( isBidirectionalAttributeName( parentNavigablePath, parentModelPart, fetchablePath, creationState ) ) {
				return createCircularBiDirectionalFetch(
						fetchablePath,
						fetchParent,
						parentNavigablePath,
						LockMode.READ
				);
			}

			/*
						class Child {
							@OneToOne
							private Mother mother;
						}

						class Mother {
							@OneToOne
							private Child stepMother;
						}

				We have a circularity but it is not bidirectional
			 */
			if ( sideNature == ForeignKeyDescriptor.Nature.KEY ) {
				final TableGroup parentTableGroup = creationState
						.getSqlAstCreationState()
						.getFromClauseAccess()
						.getTableGroup( fetchParent.getNavigablePath() );
				final DomainResult<?> foreignKeyDomainResult;
				assert !creationState.isResolvingCircularFetch();
				try {
					creationState.setResolvingCircularFetch( true );
					foreignKeyDomainResult = foreignKeyDescriptor.createKeyDomainResult(
							fetchablePath,
							parentTableGroup,
							creationState
					);
				}
				finally {
					creationState.setResolvingCircularFetch( false );
				}
				return new CircularFetchImpl(
						this,
						getEntityMappingType(),
						fetchTiming,
						fetchablePath,
						fetchParent,
						this,
						isSelectByUniqueKey( sideNature ),
						fetchablePath,
						foreignKeyDomainResult
				);
			}
		}
		return null;
	}

	protected boolean isBidirectionalAttributeName(
			NavigablePath parentNavigablePath,
			ModelPart parentModelPart,
			NavigablePath fetchablePath,
			DomainResultCreationState creationState) {
		if ( bidirectionalAttributeName == null ) {
			/*
				check if mappedBy is on the other side of the association
			 */

			/*
				class Child {
					@OneToOne(mappedBy = "biologicalChild")
					private Mother mother;
				}

				class Mother {
					@OneToOne
					private Child biologicalChild;
				}

				fetchablePath = "Child.mother.biologicalChild"
				otherSideAssociationModelPart = ToOneAttributeMapping("Child.mother")
				otherSideMappedBy = "biologicalChild"

			 */
			if ( parentModelPart instanceof ToOneAttributeMapping ) {
				final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) parentModelPart;
				if ( toOneAttributeMapping.bidirectionalAttributeName != null ) {
					return toOneAttributeMapping.isBidirectionalAttributeName(
							fetchablePath,
							this,
							parentNavigablePath,
							creationState
					);
				}
			}
			else if ( parentModelPart instanceof PluralAttributeMapping ) {
				return ( (PluralAttributeMapping) parentModelPart ).isBidirectionalAttributeName( fetchablePath, this );
			}
			else if ( parentModelPart instanceof EntityCollectionPart ) {
				NavigablePath parentOfParent = parentNavigablePath.getParent();
				if ( parentOfParent instanceof EntityIdentifierNavigablePath ) {
					parentOfParent = parentOfParent.getParent();
				}
				return ( (PluralAttributeMapping) creationState.resolveModelPart( parentOfParent ) )
						.isBidirectionalAttributeName( fetchablePath, this );
			}
			return false;
		}
		if ( cardinality == Cardinality.MANY_TO_ONE ) {
			/*
				class Child {
					@OneToOne(mappedBy = "biologicalChild")
					private Mother mother;
				}

				class Mother {
					@OneToOne
					private Child biologicalChild;
				}

				fetchablePath= Mother.biologicalChild.mother
				this.mappedBy = "biologicalChild"
				parent.getFullPath() = "Mother.biologicalChild"
			 */
			final String fullPath = parentNavigablePath.getFullPath();
			if ( fullPath.endsWith( bidirectionalAttributeName + "." + CollectionPart.Nature.ELEMENT.getName() ) ) {
				final NavigablePath parentPath = parentNavigablePath.getParent().getParent();
				// This can be null for a collection loader
				if ( parentPath != null ) {
					// If the parent is null, this is a simple collection fetch of a root, in which case the types must match
					if ( parentPath.getParent() == null ) {
						final String entityName = entityMappingType.getPartName();
						return parentPath.getFullPath().startsWith( entityName ) && (
								parentPath.getFullPath().length() == entityName.length()
										// Ignore a possible alias
										|| parentPath.getFullPath().charAt( entityName.length() ) == '('
						);
					}
					// If we have a parent, we ensure that the parent is the same as the attribute name
					else {
						return parentPath.getUnaliasedLocalName().equals( navigableRole.getLocalName() );
					}
				}
			}
			return false;
		}
		return parentNavigablePath.getUnaliasedLocalName().equals( bidirectionalAttributeName );
	}

	public String getBidirectionalAttributeName(){
		return bidirectionalAttributeName;
	}

	private Fetch createCircularBiDirectionalFetch(
			NavigablePath fetchablePath,
			FetchParent fetchParent,
			NavigablePath parentNavigablePath,
			LockMode lockMode) {
		NavigablePath referencedNavigablePath;
		if ( parentNavigablePath.getParent() == null ) {
			referencedNavigablePath = parentNavigablePath;
		}
		else {
			referencedNavigablePath = parentNavigablePath.getParent();
		}
		return new CircularBiDirectionalFetchImpl(
				FetchTiming.IMMEDIATE,
				fetchablePath,
				fetchParent,
				this,
				lockMode,
				referencedNavigablePath
		);
	}

	@Override
	public EntityFetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {

		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final FromClauseAccess fromClauseAccess = sqlAstCreationState.getFromClauseAccess();

		final TableGroup parentTableGroup = fromClauseAccess.getTableGroup(
				fetchParent.getNavigablePath()
		);

		final NavigablePath parentNavigablePath = fetchablePath.getParent();
		assert parentNavigablePath.equals( fetchParent.getNavigablePath() );

		if ( fetchTiming == FetchTiming.IMMEDIATE && selected ) {
			final TableGroup tableGroup;
			if ( fetchParent instanceof EntityResultJoinedSubclassImpl &&
					( (EntityPersister) fetchParent.getReferencedModePart() ).findDeclaredAttributeMapping( getPartName() ) == null ) {
				final TableGroupJoin tableGroupJoin = createTableGroupJoin(
						fetchablePath,
						parentTableGroup,
						resultVariable,
						getJoinType( fetchablePath, parentTableGroup ),
						true,
						creationState.getSqlAstCreationState()
				);
				parentTableGroup.addTableGroupJoin( tableGroupJoin );
				tableGroup = tableGroupJoin.getJoinedGroup();
				fromClauseAccess.registerTableGroup( fetchablePath, tableGroup );
			}
			else {
				tableGroup = fromClauseAccess.resolveTableGroup(
						fetchablePath,
						np -> {
							final TableGroupJoin tableGroupJoin = createTableGroupJoin(
									fetchablePath,
									parentTableGroup,
									resultVariable,
									getDefaultSqlAstJoinType( parentTableGroup ),
									true,
									creationState.getSqlAstCreationState()
							);
							parentTableGroup.addTableGroupJoin( tableGroupJoin );
							return tableGroupJoin.getJoinedGroup();
						}
				);
			}

			creationState.registerVisitedAssociationKey( foreignKeyDescriptor.getAssociationKey() );
			return new EntityFetchJoinedImpl(
					fetchParent,
					this,
					tableGroup,
					true,
					fetchablePath,
					creationState
			);
		}

		/*
			1. No JoinTable
				Model:
					EntityA{
						@ManyToOne
						EntityB b
					}

					EntityB{
						@ManyToOne
						EntityA a
					}

				Relational:
					ENTITY_A( id )
					ENTITY_B( id, entity_a_id)

				1.1 EntityA -> EntityB : as keyResult we need ENTITY_B.id
				1.2 EntityB -> EntityA : as keyResult we need ENTITY_B.entity_a_id (FK referring column)

			2. JoinTable

		 */

		final ForeignKeyDescriptor.Nature resolvingKeySideOfForeignKey = creationState.getCurrentlyResolvingForeignKeyPart();
		final ForeignKeyDescriptor.Nature side;
		if ( resolvingKeySideOfForeignKey == ForeignKeyDescriptor.Nature.KEY && this.sideNature == ForeignKeyDescriptor.Nature.TARGET ) {
			// If we are currently resolving the key part of a foreign key we do not want to add joins.
			// So if the lhs of this association is the target of the FK, we have to use the KEY part to avoid a join
			side = ForeignKeyDescriptor.Nature.KEY;
		}
		else {
			side = this.sideNature;
		}
		final DomainResult<?> keyResult = foreignKeyDescriptor.createDomainResult(
				fetchablePath,
				parentTableGroup,
				side,
				creationState
		);
		final boolean selectByUniqueKey = isSelectByUniqueKey( side );

		if ( fetchTiming == FetchTiming.IMMEDIATE ) {
			return new EntityFetchSelectImpl(
					fetchParent,
					this,
					fetchablePath,
					keyResult,
					selectByUniqueKey,
					creationState
			);
		}

		return new EntityDelayedFetchImpl(
				fetchParent,
				this,
				fetchablePath,
				keyResult
		);
	}

	private boolean isSelectByUniqueKey(ForeignKeyDescriptor.Nature side) {
		if ( side == ForeignKeyDescriptor.Nature.KEY ) {
			// case 1.2
			return !getKeyTargetMatchPart().getNavigableRole()
					.equals( entityMappingType.getIdentifierMapping().getNavigableRole() );
		}
		else {
			// case 1.1
			return bidirectionalAttributeName != null;
		}
	}

	@Override
	public <T> DomainResult<T> createDelayedDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		// We only need a join if the key is on the referring side i.e. this is an inverse to-one
		// and if the FK refers to a non-PK, in which case we must load the whole entity
		if ( sideNature == ForeignKeyDescriptor.Nature.TARGET || referencedPropertyName != null ) {
			final TableGroupJoin tableGroupJoin = createTableGroupJoin(
					navigablePath,
					tableGroup,
					null,
					getDefaultSqlAstJoinType( tableGroup ),
					true,
					creationState.getSqlAstCreationState()
			);
			tableGroup.addTableGroupJoin( tableGroupJoin );

			creationState.getSqlAstCreationState().getFromClauseAccess().registerTableGroup(
					navigablePath,
					tableGroupJoin.getJoinedGroup()
			);
		}
		if ( referencedPropertyName == null ) {
			return new EntityDelayedResultImpl(
					navigablePath.append( EntityIdentifierMapping.ROLE_LOCAL_NAME ),
					this,
					tableGroup,
					creationState
			);
		}
		else {
			// We don't support proxies based on a non-PK yet, so we must fetch the whole entity
			return new EntityResultImpl(
					navigablePath,
					this,
					tableGroup, null,
					creationState
			);
		}
	}

	@Override
	public SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup) {
		if ( isNullable ) {
			return SqlAstJoinType.LEFT;
		}
		else if ( parentTableGroup.getModelPart() instanceof CollectionPart ) {
			return SqlAstJoinType.LEFT;
		}
		else {
			if ( parentTableGroup.canUseInnerJoins() ) {
				return SqlAstJoinType.INNER;
			}
			return SqlAstJoinType.LEFT;
		}
	}

	private TableGroup createTableGroupJoin(
			NavigablePath fetchablePath,
			boolean fetched,
			SqlAstJoinType sqlAstJoinType,
			String sourceAlias,
			DomainResultCreationState creationState,
			TableGroup parentTableGroup) {
		final TableGroupJoin tableGroupJoin = createTableGroupJoin(
				fetchablePath,
				parentTableGroup,
				sourceAlias,
				sqlAstJoinType,
				fetched,
				creationState.getSqlAstCreationState()
		);

		return tableGroupJoin.getJoinedGroup();
	}

	@Override
	public int getNumberOfFetchables() {
		return getEntityMappingType().getNumberOfFetchables();
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

		final LazyTableGroup lazyTableGroup = createRootTableGroupJoin(
				navigablePath,
				lhs,
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

		final TableReference lhsTableReference = lhs.resolveTableReference( navigablePath, identifyingColumnsTableExpression );

		lazyTableGroup.setTableGroupInitializerCallback(
				tableGroup -> join.applyPredicate(
						foreignKeyDescriptor.generateJoinPredicate(
								sideNature == ForeignKeyDescriptor.Nature.TARGET ? lhsTableReference : tableGroup.getPrimaryTableReference(),
								sideNature == ForeignKeyDescriptor.Nature.TARGET ? tableGroup.getPrimaryTableReference() : lhsTableReference,
								sqlAstJoinType,
								sqlExpressionResolver,
								creationContext
						)
				)
		);

		if ( sqlAstJoinType == SqlAstJoinType.INNER && isNullable ) {
			// Force initialization of the underlying table group join to retain cardinality
			lazyTableGroup.getPrimaryTableReference();
		}

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
		final SqlAliasBase sqlAliasBase = aliasBaseGenerator.createSqlAliasBase( sqlAliasStem );
		final boolean canUseInnerJoin = sqlAstJoinType == SqlAstJoinType.INNER || lhs.canUseInnerJoins() && !isNullable;
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
					if ( !canUseParentTableGroup ) {
						return false;
					}
					NavigablePath path = np.getParent();
					// Fast path
					if ( path != null && navigablePath.equals( path ) ) {
						return targetKeyPropertyNames.contains( np.getUnaliasedLocalName() )
								&& identifyingColumnsTableExpression.equals( tableExpression );
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
							&& identifyingColumnsTableExpression.equals( tableExpression );
				},
				this,
				explicitSourceAlias,
				sqlAliasBase,
				creationContext.getSessionFactory(),
				lhs
		);

		if ( predicateConsumer != null ) {
			final TableReference lhsTableReference = lhs.resolveTableReference(
					navigablePath,
					identifyingColumnsTableExpression
			);

			lazyTableGroup.setTableGroupInitializerCallback(
					tableGroup -> predicateConsumer.accept(
							foreignKeyDescriptor.generateJoinPredicate(
									sideNature == ForeignKeyDescriptor.Nature.TARGET ? lhsTableReference : tableGroup.getPrimaryTableReference(),
									sideNature == ForeignKeyDescriptor.Nature.TARGET ? tableGroup.getPrimaryTableReference() : lhsTableReference,
									sqlAstJoinType,
									sqlExpressionResolver,
									creationContext
							)
					)
			);

			if ( sqlAstJoinType == SqlAstJoinType.INNER && isNullable ) {
				// Force initialization of the underlying table group join to retain cardinality
				lazyTableGroup.getPrimaryTableReference();
			}
		}

		return lazyTableGroup;
	}

	private SqlAstJoinType getJoinType(NavigablePath navigablePath, TableGroup tableGroup) {
		for ( TableGroupJoin tableGroupJoin : tableGroup.getTableGroupJoins() ) {
			if ( tableGroupJoin.getNavigablePath().equals( navigablePath ) ) {
				return tableGroupJoin.getJoinType();
			}
		}
		return getDefaultSqlAstJoinType( tableGroup );
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
		return sqlAliasStem;
	}

	public boolean isNullable() {
		return isNullable;
	}

	@Override
	public boolean isOptional(){
		return isOptional;
	}

	public boolean isConstrained(){
		return isConstrained;
	}

	public boolean isIgnoreNotFound(){
		return isIgnoreNotFound;
	}

	@Override
	public boolean isUnwrapProxy() {
		return unwrapProxy;
	}

	@Override
	public EntityMappingType getAssociatedEntityMappingType() {
		return getEntityMappingType();
	}

	@Override
	public ModelPart getKeyTargetMatchPart() {
		return foreignKeyDescriptor;
	}

	@Override
	public String toString() {
		return "ToOneAttributeMapping(" + navigableRole + ")@" + System.identityHashCode( this );
	}

	@Override
	public void breakDownJdbcValues(
			Object domainValue,
			JdbcValueConsumer valueConsumer,
			SharedSessionContractImplementor session) {
		foreignKeyDescriptor.breakDownJdbcValues( domainValue, valueConsumer, session );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		if ( sideNature == ForeignKeyDescriptor.Nature.KEY ) {
			return foreignKeyDescriptor.visitKeySelectables( offset, consumer );
		}
		else {
			return 0;
		}
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		if ( sideNature == ForeignKeyDescriptor.Nature.KEY ) {
			foreignKeyDescriptor.getKeyPart().applySqlSelections( navigablePath, tableGroup, creationState );
		}
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		if ( sideNature == ForeignKeyDescriptor.Nature.KEY ) {
			foreignKeyDescriptor.getKeyPart().applySqlSelections(
					navigablePath,
					tableGroup,
					creationState,
					selectionConsumer
			);
		}
	}

	@Override
	public int getJdbcTypeCount() {
		return foreignKeyDescriptor.getJdbcTypeCount();
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return foreignKeyDescriptor.forEachJdbcType( offset, action );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return foreignKeyDescriptor.disassemble( value, session );
	}

	@Override
	public int forEachDisassembledJdbcValue(Object value, Clause clause, int offset, JdbcValuesConsumer valuesConsumer, SharedSessionContractImplementor session) {
		return foreignKeyDescriptor.forEachDisassembledJdbcValue( value, clause, offset, valuesConsumer, session );
	}

	@Override
	public int forEachJdbcValue(Object value, Clause clause, int offset, JdbcValuesConsumer consumer, SharedSessionContractImplementor session) {
		return foreignKeyDescriptor.forEachDisassembledJdbcValue(
				foreignKeyDescriptor.disassemble( value, session ),
				clause,
				offset,
				consumer,
				session
		);
	}
}
