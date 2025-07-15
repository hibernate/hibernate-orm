/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.AssociationKey;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.VirtualModelPart;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityNameUse;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.spi.TreatedNavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasStemHelper;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.CorrelatedTableGroup;
import org.hibernate.sql.ast.tree.from.LazyTableGroup;
import org.hibernate.sql.ast.tree.from.MappedByTableGroup;
import org.hibernate.sql.ast.tree.from.PluralTableGroup;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;
import org.hibernate.sql.results.graph.entity.internal.EntityDelayedFetchImpl;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchJoinedImpl;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchSelectImpl;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.internal.domain.CircularBiDirectionalFetchImpl;
import org.hibernate.sql.results.internal.domain.CircularFetchImpl;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hibernate.internal.util.StringHelper.split;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.getTableIdentifierExpression;

/**
 * @author Steve Ebersole
 */
public class ToOneAttributeMapping
		extends AbstractSingularAttributeMapping
		implements EntityValuedFetchable, EntityAssociationMapping, TableGroupJoinProducer,
				LazyTableGroup.ParentTableGroupUseChecker {

	public enum Cardinality {
		ONE_TO_ONE,
		MANY_TO_ONE,
		LOGICAL_ONE_TO_ONE
	}

	private final NavigableRole navigableRole;

	private final String sqlAliasStem;
	// The nullability of the actual FK column
	private final boolean isNullable;
	private final boolean isLazy;
	/*
	The nullability of the table on which the FK column is located
	Note that this can be null although the FK column is not nullable e.g. in the case of a join table

	@Entity
	public class Entity1 {
		@OneToOne
		@JoinTable(name = "key_table")
		Entity2 association;
	}

	Here the join to "key_table" is nullable, but the FK column is not null.
	Choosing an inner join for the association would be wrong though, because of the nullability of the key table,
	hence this flag is also controlling the default join type.
	 */
	private final boolean isKeyTableNullable;
	private final boolean isInternalLoadNullable;
	private final NotFoundAction notFoundAction;
	private final boolean unwrapProxy;
	private final boolean isOptional;
	private final EntityMappingType entityMappingType;

	private final String referencedPropertyName;
	private final String targetKeyPropertyName;
	private final Set<String> targetKeyPropertyNames;

	private final Cardinality cardinality;
	private final boolean hasJoinTable;
	/*
	Capture the other side's name of a possibly bidirectional association to allow resolving circular fetches.
	It may be null if the referenced property is a non-entity.
	 */
	private final SelectablePath bidirectionalAttributePath;
	private final TableGroupProducer declaringTableGroupProducer;

	private ForeignKeyDescriptor foreignKeyDescriptor;
	private ForeignKeyDescriptor.Nature sideNature;
	private String identifyingColumnsTableExpression;
	private boolean canUseParentTableGroup;

	/**
	 * For Hibernate Reactive
	 */
	protected ToOneAttributeMapping(ToOneAttributeMapping original) {
		super( original );
		navigableRole = original.navigableRole;
		isInternalLoadNullable = original.isInternalLoadNullable;
		notFoundAction = original.notFoundAction;
		unwrapProxy = original.unwrapProxy;
		isOptional = original.isOptional;
		entityMappingType = original.entityMappingType;
		referencedPropertyName = original.referencedPropertyName;
		targetKeyPropertyName = original.targetKeyPropertyName;
		cardinality = original.cardinality;
		hasJoinTable = original.hasJoinTable;
		bidirectionalAttributePath = original.bidirectionalAttributePath;
		declaringTableGroupProducer = original.declaringTableGroupProducer;
		isKeyTableNullable = original.isKeyTableNullable;
		sqlAliasStem = original.sqlAliasStem;
		targetKeyPropertyNames = original.targetKeyPropertyNames;
		isNullable = original.isNullable;
		isLazy = original.isLazy;
		foreignKeyDescriptor = original.foreignKeyDescriptor;
		sideNature = original.sideNature;
		identifyingColumnsTableExpression = original.identifyingColumnsTableExpression;
		canUseParentTableGroup = original.canUseParentTableGroup;

	}

	public ToOneAttributeMapping(
			String name,
			NavigableRole navigableRole,
			int stateArrayPosition,
			int fetchableIndex,
			ToOne bootValue,
			AttributeMetadata attributeMetadata,
			FetchOptions mappedFetchOptions,
			EntityMappingType entityMappingType,
			ManagedMappingType declaringType,
			EntityPersister declaringEntityPersister,
			PropertyAccess propertyAccess) {
		this(
				name,
				navigableRole,
				stateArrayPosition,
				fetchableIndex,
				bootValue,
				attributeMetadata,
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
			int fetchableIndex,
			ToOne bootValue,
			AttributeMetadata attributeMetadata,
			FetchTiming mappedFetchTiming,
			FetchStyle mappedFetchStyle,
			EntityMappingType entityMappingType,
			ManagedMappingType declaringType,
			EntityPersister declaringEntityPersister,
			PropertyAccess propertyAccess) {
		super(
				name,
				stateArrayPosition,
				fetchableIndex,
				attributeMetadata,
				adjustFetchTiming( mappedFetchTiming, bootValue ),
				mappedFetchStyle,
				declaringType,
				propertyAccess
		);
		this.entityMappingType = entityMappingType;
		this.navigableRole = navigableRole;
		sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( name );
		isNullable = bootValue.isNullable();
		isLazy = navigableRole.getParent().getParent() == null
				&& declaringEntityPersister.getBytecodeEnhancementMetadata().getLazyAttributesMetadata()
						.isLazyAttribute( name );
		referencedPropertyName = bootValue.getReferencedPropertyName();
		unwrapProxy = bootValue.isUnwrapProxy();

		declaringTableGroupProducer = resolveDeclaringTableGroupProducer( declaringEntityPersister, navigableRole );
		final var factory = declaringEntityPersister.getFactory();
		if ( bootValue instanceof ManyToOne manyToOne ) {
			notFoundAction = manyToOne.getNotFoundAction();
			cardinality = manyToOne.isLogicalOneToOne()
					? Cardinality.LOGICAL_ONE_TO_ONE
					: Cardinality.MANY_TO_ONE;
			final PersistentClass entityBinding =
					manyToOne.getMetadata().getEntityBinding( manyToOne.getReferencedEntityName() );
			if ( referencedPropertyName == null ) {
				hasJoinTable = manyToOne.hasJoinTable();
				bidirectionalAttributePath =
						bidirectionalAttributePath( declaringType, manyToOne, name, entityBinding );
			}
			else {
				// Only set the bidirectional attribute name if the referenced property can actually be circular i.e. an entity type
				final Property property = entityBinding.getProperty( referencedPropertyName );
				hasJoinTable =
						cardinality == Cardinality.LOGICAL_ONE_TO_ONE
								&& property != null
								&& property.getValue() instanceof ManyToOne manyToOneValue
								&& manyToOneValue.isLogicalOneToOne();
				bidirectionalAttributePath =
						property != null && property.getValue().getType() instanceof EntityType
								? SelectablePath.parse( referencedPropertyName )
								: null;
			}
			if ( bootValue.isNullable() ) {
				isKeyTableNullable = true;
			}
			else {
				final String targetTableName =
						getTableIdentifierExpression( manyToOne.getTable(), factory );
				if ( CollectionPart.Nature.fromNameExact( navigableRole.getParent().getLocalName() ) != null ) {
					// * the to-one's parent is directly a collection element or index
					// * therefore, its parent-parent should be the collection itself
					final String rootPath = declaringEntityPersister.getNavigableRole().getFullPath();
					final String unqualifiedPath =
							navigableRole.getParent().getParent().getFullPath()
									.substring( rootPath.length() + 1 );
					final PluralAttributeMapping pluralAttribute =
							(PluralAttributeMapping) declaringEntityPersister.findByPath( unqualifiedPath );
					assert pluralAttribute != null;
					isKeyTableNullable =
							!pluralAttribute.getCollectionDescriptor().getTableName()
									.equals( targetTableName );
				}
				else {
					final int tableIndex = ArrayHelper.indexOf(
							declaringEntityPersister.getTableNames(),
							targetTableName
					);
					isKeyTableNullable = declaringEntityPersister.isNullableTable( tableIndex );
				}
			}
			isOptional = manyToOne.isIgnoreNotFound();
			isInternalLoadNullable = isNullable && bootValue.isForeignKeyEnabled() || hasNotFoundAction();
		}
		else if ( bootValue instanceof OneToOne oneToOne ) {
			cardinality = Cardinality.ONE_TO_ONE;
			hasJoinTable = false;

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
			bidirectionalAttributePath =
					oneToOne.getMappedByProperty() == null
							? SelectablePath.parse( referencedPropertyName )
							: SelectablePath.parse( oneToOne.getMappedByProperty() );
			notFoundAction = null;
			isKeyTableNullable = isNullable();
			isOptional = !bootValue.isConstrained();
			isInternalLoadNullable = isNullable();
		}
		else {
			throw new AssertionFailure( "Unrecognized kind of ToOne" );
		}

		if ( entityMappingType.getSoftDeleteMapping() != null ) {
			// cannot be lazy
			if ( getTiming() == FetchTiming.DELAYED ) {
				throw new UnsupportedMappingException( String.format(
						Locale.ROOT,
						"To-one attribute (%s.%s) cannot be mapped as LAZY as its associated entity is defined with @SoftDelete",
						declaringType.getPartName(),
						getAttributeName()
				) );
			}
		}

		if ( referencedPropertyName == null ) {
			final Set<String> targetKeyPropertyNames = new HashSet<>( 2 );
			targetKeyPropertyNames.add( EntityIdentifierMapping.ID_ROLE_NAME );
			final PersistentClass entityBinding = bootValue.getBuildingContext().getMetadataCollector()
					.getEntityBinding( entityMappingType.getEntityName() );
			final Type propertyType =
					entityBinding.getIdentifierMapper() == null
							? entityBinding.getIdentifier().getType()
							: entityBinding.getIdentifierMapper().getType();
			if ( entityBinding.getIdentifierProperty() == null ) {
				if ( propertyType instanceof ComponentType compositeType && compositeType.isEmbedded()
						&& compositeType.getPropertyNames().length == 1 ) {
					this.targetKeyPropertyName = compositeType.getPropertyNames()[0];
					addPrefixedPropertyPaths(
							targetKeyPropertyNames,
							targetKeyPropertyName,
							compositeType.getSubtypes()[0],
							factory
					);
					addPrefixedPropertyNames(
							targetKeyPropertyNames,
							EntityIdentifierMapping.ID_ROLE_NAME,
							propertyType,
							factory
					);
				}
				else {
					targetKeyPropertyName = EntityIdentifierMapping.ID_ROLE_NAME;
					addPrefixedPropertyPaths(
							targetKeyPropertyNames,
							null,
							propertyType,
							factory
					);
				}
			}
			else {
				targetKeyPropertyName = entityBinding.getIdentifierProperty().getName();
				addPrefixedPropertyPaths(
						targetKeyPropertyNames,
						targetKeyPropertyName,
						propertyType,
						factory
				);
			}
			this.targetKeyPropertyNames = targetKeyPropertyNames;
		}
		else {
			final PersistentClass entityBinding = bootValue.getBuildingContext().getMetadataCollector()
					.getEntityBinding( entityMappingType.getEntityName() );
			final Type propertyType = entityBinding.getRecursiveProperty( referencedPropertyName ).getType();
			if ( bootValue.isReferenceToPrimaryKey() ) {
				targetKeyPropertyName = referencedPropertyName;
				final Set<String> targetKeyPropertyNames = new HashSet<>( 3 );
				addPrefixedPropertyNames(
						targetKeyPropertyNames,
						targetKeyPropertyName,
						propertyType,
						factory
				);
				addPrefixedPropertyNames(
						targetKeyPropertyNames,
						null,
						bootValue.getType(),
						factory
				);
				this.targetKeyPropertyNames = targetKeyPropertyNames;
			}
			else {
				if ( propertyType instanceof ComponentType compositeType && compositeType.isEmbedded()
						&& compositeType.getPropertyNames().length == 1 ) {
					final Set<String> targetKeyPropertyNames = new HashSet<>( 2 );
					targetKeyPropertyName = compositeType.getPropertyNames()[0];
					addPrefixedPropertyPaths(
							targetKeyPropertyNames,
							targetKeyPropertyName,
							compositeType.getSubtypes()[0],
							factory
					);
					addPrefixedPropertyNames(
							targetKeyPropertyNames,
							EntityIdentifierMapping.ID_ROLE_NAME,
							propertyType,
							factory
					);
					this.targetKeyPropertyNames = targetKeyPropertyNames;
				}
				else {
					final Set<String> targetKeyPropertyNames = new HashSet<>( 2 );
					targetKeyPropertyName = referencedPropertyName;
					final String mapsIdAttributeName = findMapsIdPropertyName( entityMappingType, referencedPropertyName );
					// If there is a "virtual property" for a non-PK join mapping, we try to see if the columns match the
					// primary key columns and if so, we add the primary key property name as target key property
					if ( mapsIdAttributeName != null ) {
						addPrefixedPropertyPaths(
								targetKeyPropertyNames,
								mapsIdAttributeName,
								entityMappingType.getEntityPersister().getIdentifierType(),
								factory
						);
					}
					addPrefixedPropertyNames(
							targetKeyPropertyNames,
							targetKeyPropertyName,
							propertyType,
							factory
					);
					addPrefixedPropertyNames(
							targetKeyPropertyNames,
							ForeignKeyDescriptor.PART_NAME,
							propertyType,
							factory
					);
					this.targetKeyPropertyNames = targetKeyPropertyNames;
				}
			}
		}
	}

	private SelectablePath bidirectionalAttributePath(
			ManagedMappingType declaringType,
			ManyToOne manyToOne,
			String name,
			PersistentClass entityBinding) {
		final String propertyName = manyToOne.getPropertyName();
		final String propertyPath = propertyName == null ? name : propertyName;
		return cardinality == Cardinality.LOGICAL_ONE_TO_ONE
				? findBidirectionalOneToOneAttributeName( propertyPath, declaringType, manyToOne, entityBinding )
				: findBidirectionalOneToManyAttributeName( declaringType, propertyPath, entityBinding );
	}

	private SelectablePath findBidirectionalOneToOneAttributeName(
			String propertyPath, ManagedMappingType declaringType,
			ManyToOne manyToOne,
			PersistentClass entityBinding) {
		SelectablePath bidirectionalAttributeName = null;
		//boolean foundJoinTable = false;
		// Handle join table cases
		for ( Join join : entityBinding.getJoinClosure() ) {
			if ( join.getPersistentClass().getEntityName().equals( entityBinding.getEntityName() )
					&& join.getPropertySpan() == 1
					&& join.getTable() == manyToOne.getTable()
					&& equal( join.getKey(), manyToOne ) ) {
				bidirectionalAttributeName = SelectablePath.parse( join.getProperties().get(0).getName() );
//				foundJoinTable = true;
				break;
			}
		}
		// Simple one-to-one mapped by cases
		if ( bidirectionalAttributeName == null ) {
			bidirectionalAttributeName = findBidirectionalOneToOneAttributeName(
					propertyPath,
					declaringType,
					null,
					entityBinding.getPropertyClosure()
			);
		}
//		assert hasJoinTable == foundJoinTable;
		return bidirectionalAttributeName;
	}

	private static SelectablePath findBidirectionalOneToManyAttributeName(
			ManagedMappingType declaringType,
			String propertyPath,
			PersistentClass entityBinding) {
		return findBidirectionalOneToManyAttributeName(
				propertyPath,
				declaringType,
				null,
				entityBinding.getPropertyClosure()
		);
	}

	private static SelectablePath findBidirectionalOneToManyAttributeName(
			String propertyPath,
			ManagedMappingType declaringType,
			SelectablePath parentSelectablePath,
			List<Property> properties) {
		for ( Property property : properties ) {
			final Value value = property.getValue();
			if ( value instanceof Component component ) {
				final SelectablePath bidirectionalAttributeName =
						findBidirectionalOneToManyAttributeName(
								propertyPath,
								declaringType,
								parentSelectablePath == null
										? SelectablePath.parse( property.getName() )
										: parentSelectablePath.append( property.getName() ),
								component.getProperties()
						);
				if ( bidirectionalAttributeName != null ) {
					return bidirectionalAttributeName;
				}
			}
			if ( value instanceof Collection collection ) {
				if ( propertyPath.equals( collection.getMappedByProperty() )
						&& collection.getElement().getType().getName()
							.equals( declaringType.getJavaType().getTypeName() ) ) {
					return parentSelectablePath == null
							? SelectablePath.parse( property.getName() )
							: parentSelectablePath.append( property.getName() );
				}
			}
		}
		return null;
	}

	private SelectablePath findBidirectionalOneToOneAttributeName(
			String propertyPath,
			ManagedMappingType declaringType,
			SelectablePath parentSelectablePath,
			List<Property> properties) {
		for ( Property property : properties ) {
			final Value value = property.getValue();
			final String name = property.getName();
			if ( value instanceof Component component ) {
				final SelectablePath bidirectionalAttributeName =
						findBidirectionalOneToOneAttributeName(
								propertyPath,
								declaringType,
								parentSelectablePath == null
										? SelectablePath.parse( name )
										: parentSelectablePath.append( name ),
								component.getProperties()
						);
				if ( bidirectionalAttributeName != null ) {
					return bidirectionalAttributeName;
				}
			}
			else if ( value instanceof OneToOne oneToOne ) {
				final String referencedEntityName = oneToOne.getReferencedEntityName();
				if ( declaringTableGroupProducer.getNavigableRole().getLocalName().equals( referencedEntityName )
						&& propertyPath.equals( oneToOne.getMappedByProperty() )
						&& referencedEntityName.equals( declaringType.getJavaType().getTypeName() ) ) {
					return parentSelectablePath == null
							? SelectablePath.parse( name )
							: parentSelectablePath.append( name );
				}
			}
		}
		return null;
	}

	private static FetchTiming adjustFetchTiming(FetchTiming mappedFetchTiming, ToOne bootValue) {
		return bootValue instanceof ManyToOne manyToOne
			&& manyToOne.getNotFoundAction() != null
				? FetchTiming.IMMEDIATE
				: mappedFetchTiming;
	}

	private static TableGroupProducer resolveDeclaringTableGroupProducer(
			EntityPersister declaringEntityPersister, NavigableRole navigableRole) {
		// Also handle cases where a collection contains an embeddable that contains an association
		final String collectionRole = collectionRole( navigableRole );
		if ( collectionRole != null ) {
			// This is a collection part i.e. to-many association
			return declaringEntityPersister.getFactory().getMappingMetamodel()
					.findCollectionDescriptor( collectionRole )
					.getAttributeMapping();
		}
		// This is a simple to-one association
		return declaringEntityPersister;
	}

	private static String collectionRole(NavigableRole navigableRole) {
		NavigableRole parentRole = navigableRole.getParent();
		do {
			if ( CollectionPart.Nature.fromNameExact( parentRole.getLocalName() ) != null ) {
				return parentRole.getParent().getFullPath();
			}
			parentRole = parentRole.getParent();
		}
		while ( parentRole != null );
		return null;
	}

	private ToOneAttributeMapping(
			ToOneAttributeMapping original,
			ManagedMappingType declaringType,
			TableGroupProducer declaringTableGroupProducer) {
		super(
				original.getAttributeName(),
				original.getStateArrayPosition(),
				original.getFetchableKey(),
				original.getAttributeMetadata(),
				original,
				declaringType,
				original.getPropertyAccess()
		);
		this.navigableRole = original.navigableRole;
		this.sqlAliasStem = original.sqlAliasStem;
		this.isNullable = original.isNullable;
		this.isLazy = original.isLazy;
		this.isKeyTableNullable = original.isKeyTableNullable;
		this.isOptional = original.isOptional;
		this.notFoundAction = original.notFoundAction;
		this.unwrapProxy = original.unwrapProxy;
		this.entityMappingType = original.entityMappingType;
		this.referencedPropertyName = original.referencedPropertyName;
		this.targetKeyPropertyName = original.targetKeyPropertyName;
		this.targetKeyPropertyNames = original.targetKeyPropertyNames;
		this.cardinality = original.cardinality;
		this.hasJoinTable = original.hasJoinTable;
		this.bidirectionalAttributePath = original.bidirectionalAttributePath;
		this.declaringTableGroupProducer = declaringTableGroupProducer;
		this.isInternalLoadNullable = original.isInternalLoadNullable;
	}

	private static boolean equal(Value lhsValue, Value rhsValue) {
		List<Selectable> lhsColumns = lhsValue.getSelectables();
		List<Selectable> rhsColumns = rhsValue.getSelectables();
		if ( lhsColumns.size() != rhsColumns.size() ) {
			return false;
		}
		else {
			for ( int i=0; i<lhsColumns.size(); i++ ) {
				Selectable lhs = lhsColumns.get( i );
				Selectable rhs = rhsColumns.get( i );
				if ( !lhs.getText().equals( rhs.getText() ) ) {
					return false;
				}
			}
			return true;
		}
	}

	static String findMapsIdPropertyName(EntityMappingType entityMappingType, String referencedPropertyName) {
		final EntityPersister persister = entityMappingType.getEntityPersister();
		return Arrays.equals( persister.getIdentifierColumnNames(),
					persister.getPropertyColumnNames( referencedPropertyName ) )
				? persister.getIdentifierPropertyName()
				: null;
	}

	public static void addPrefixedPropertyPaths(
			Set<String> targetKeyPropertyNames,
			String prefix,
			Type type,
			SessionFactoryImplementor factory) {
		addPrefixedPropertyNames(
				targetKeyPropertyNames,
				prefix,
				type,
				factory
		);
		addPrefixedPropertyNames(
				targetKeyPropertyNames,
				ForeignKeyDescriptor.PART_NAME,
				type,
				factory
		);
		addPrefixedPropertyNames(
				targetKeyPropertyNames,
				EntityIdentifierMapping.ID_ROLE_NAME,
				type,
				factory
		);
	}

	public static void addPrefixedPropertyNames(
			Set<String> targetKeyPropertyNames,
			String prefix,
			Type type,
			SessionFactoryImplementor factory) {
		if ( prefix != null ) {
			targetKeyPropertyNames.add( prefix );
		}
		if ( type instanceof ComponentType componentType ) {
			final String[] propertyNames = componentType.getPropertyNames();
			final Type[] componentTypeSubtypes = componentType.getSubtypes();
			for ( int i = 0, propertyNamesLength = propertyNames.length; i < propertyNamesLength; i++ ) {
				final String newPrefix = prefix == null ? propertyNames[i] : prefix + "." + propertyNames[i];
				addPrefixedPropertyNames( targetKeyPropertyNames, newPrefix, componentTypeSubtypes[i], factory );
			}
		}
		else if ( type instanceof EntityType entityType ) {
			final Type identifierOrUniqueKeyType =
					entityType.getIdentifierOrUniqueKeyType( factory.getRuntimeMetamodels() );
			final String propertyName = propertyName( factory, entityType, identifierOrUniqueKeyType );
			final String newPrefix;
			final String newPkPrefix;
			final String newFkPrefix;
			if ( prefix == null ) {
				newPrefix = propertyName;
				newPkPrefix = EntityIdentifierMapping.ID_ROLE_NAME;
				newFkPrefix = ForeignKeyDescriptor.PART_NAME;
			}
			else if ( propertyName == null ) {
				newPrefix = prefix;
				newPkPrefix = prefix + "." + EntityIdentifierMapping.ID_ROLE_NAME;
				newFkPrefix = prefix + "." + ForeignKeyDescriptor.PART_NAME;
			}
			else {
				newPrefix = prefix + "." + propertyName;
				newPkPrefix = prefix + "." + EntityIdentifierMapping.ID_ROLE_NAME;
				newFkPrefix = prefix + "." + ForeignKeyDescriptor.PART_NAME;
			}
			addPrefixedPropertyNames( targetKeyPropertyNames, newPrefix, identifierOrUniqueKeyType, factory );
			addPrefixedPropertyNames( targetKeyPropertyNames, newPkPrefix, identifierOrUniqueKeyType, factory );
			addPrefixedPropertyNames( targetKeyPropertyNames, newFkPrefix, identifierOrUniqueKeyType, factory );
			if ( identifierOrUniqueKeyType instanceof EmbeddedComponentType ) {
				final String newEmbeddedPkPrefix;
				final String newEmbeddedFkPrefix;
				if ( prefix == null ) {
					newEmbeddedPkPrefix = EntityIdentifierMapping.ID_ROLE_NAME;
					newEmbeddedFkPrefix = ForeignKeyDescriptor.PART_NAME;
				}
				else {
					newEmbeddedPkPrefix = prefix + "." + EntityIdentifierMapping.ID_ROLE_NAME;
					newEmbeddedFkPrefix = prefix + "." + ForeignKeyDescriptor.PART_NAME;
				}
				addPrefixedPropertyNames( targetKeyPropertyNames, newEmbeddedPkPrefix, identifierOrUniqueKeyType, factory );
				addPrefixedPropertyNames( targetKeyPropertyNames, newEmbeddedFkPrefix, identifierOrUniqueKeyType, factory );
			}
		}
	}

	private static String propertyName(SessionFactoryImplementor factory, EntityType entityType, Type identifierOrUniqueKeyType) {
		if ( entityType.isReferenceToPrimaryKey() ) {
			return entityType.getAssociatedEntityPersister( factory ).getIdentifierPropertyName();
		}
		else if ( identifierOrUniqueKeyType instanceof EmbeddedComponentType ) {
			return null;
		}
		else {
			return entityType.getRHSUniqueKeyPropertyName();
		}
	}

	public ToOneAttributeMapping copy(ManagedMappingType declaringType, TableGroupProducer declaringTableGroupProducer) {
		return new ToOneAttributeMapping( this, declaringType, declaringTableGroupProducer );
	}

	@Override
	public void setForeignKeyDescriptor(ForeignKeyDescriptor foreignKeyDescriptor) {
		assert identifyingColumnsTableExpression != null;
		this.foreignKeyDescriptor = foreignKeyDescriptor;
		if ( cardinality == Cardinality.ONE_TO_ONE && bidirectionalAttributePath != null ) {
			sideNature = ForeignKeyDescriptor.Nature.TARGET;
		}
		else {
			sideNature =
					foreignKeyDescriptor.getAssociationKey().table()
							.equals( identifyingColumnsTableExpression )
					? ForeignKeyDescriptor.Nature.KEY
					: ForeignKeyDescriptor.Nature.TARGET;
		}

		// We can only use the parent table group if
		// 		* the FK is located there
		// 		* the association does not force a join (`@NotFound`, nullable 1-1, ...)
		// Otherwise we need to join to the associated entity table(s)
		final boolean forceJoin = hasNotFoundAction()
				|| entityMappingType.getSoftDeleteMapping() != null
				|| cardinality == Cardinality.ONE_TO_ONE && isNullable();
		canUseParentTableGroup = !forceJoin
				&& sideNature == ForeignKeyDescriptor.Nature.KEY
				&& declaringTableGroupProducer.containsTableReference( identifyingColumnsTableExpression );
	}

	public String getIdentifyingColumnsTableExpression() {
		return identifyingColumnsTableExpression;
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

	@Override
	public boolean isReferenceToPrimaryKey() {
		return foreignKeyDescriptor.getSide( sideNature.inverse() ).getModelPart().isEntityIdentifierMapping();
	}

	@Override
	public boolean isFkOptimizationAllowed() {
		return canUseParentTableGroup;
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return foreignKeyDescriptor.hasPartitionedSelectionMapping();
	}

	public String getReferencedPropertyName() {
		return referencedPropertyName;
	}

	public String getTargetKeyPropertyName() {
		return targetKeyPropertyName;
	}

	@Override
	public Set<String> getTargetKeyPropertyNames() {
		return targetKeyPropertyNames;
	}

	public Cardinality getCardinality() {
		return cardinality;
	}

	public boolean hasJoinTable() {
		return hasJoinTable;
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
		if ( canUseParentTableGroup && targetKeyPropertyNames.contains( name ) ) {
			final ModelPart fkPart =
					sideNature == ForeignKeyDescriptor.Nature.KEY
							? foreignKeyDescriptor.getKeyPart()
							: foreignKeyDescriptor.getTargetPart();
			if ( fkPart instanceof EmbeddableValuedModelPart modelPart && fkPart instanceof VirtualModelPart
					&& !EntityIdentifierMapping.ID_ROLE_NAME.equals( name )
					&& !ForeignKeyDescriptor.PART_NAME.equals( name )
					&& !ForeignKeyDescriptor.TARGET_PART_NAME.equals( name )
					&& !fkPart.getPartName().equals( name ) ) {
				return modelPart.findSubPart( name, targetType );
			}
			return fkPart;
		}
		return EntityValuedFetchable.super.findSubPart( name, targetType );
	}

	private boolean requiresJoinForDelayedFetch() {
		return entityMappingType.isConcreteProxy() && sideNature == ForeignKeyDescriptor.Nature.TARGET;
//			|| entityMappingType.hasWhereRestrictions() && canAddRestriction()
	}

	private boolean canAddRestriction() {
		return hasJoinTable || sideNature == ForeignKeyDescriptor.Nature.TARGET;
	}

	@Override
	public Fetch resolveCircularFetch(
			NavigablePath fetchablePath,
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			DomainResultCreationState creationState) {
		final AssociationKey associationKey = foreignKeyDescriptor.getAssociationKey();
		final boolean associationKeyVisited = creationState.isAssociationKeyVisited( associationKey );
		if ( associationKeyVisited || bidirectionalAttributePath != null ) {
			if ( !associationKeyVisited && creationState.isRegisteringVisitedAssociationKeys() ) {
				// If the current association key hasn't been visited yet and we are registering keys,
				// then there can't be a circular fetch
				return null;
			}
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
			if ( parentNavigablePath.getLocalName().equals( ForeignKeyDescriptor.PART_NAME )
					|| parentNavigablePath.getLocalName().equals( ForeignKeyDescriptor.TARGET_PART_NAME ) ) {
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
						fetchTiming,
						creationState
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
			final TableGroup parentTableGroup =
					creationState.getSqlAstCreationState().getFromClauseAccess()
							.getTableGroup( fetchParent.getNavigablePath() );
			final DomainResult<?> foreignKeyDomainResult;
			assert !creationState.isResolvingCircularFetch();
			try {
				creationState.setResolvingCircularFetch( true );
				if ( sideNature == ForeignKeyDescriptor.Nature.KEY ) {
					foreignKeyDomainResult = foreignKeyDescriptor.createKeyDomainResult(
							fetchablePath,
							createTableGroupForDelayedFetch( fetchablePath, parentTableGroup, null, creationState ),
							fetchParent,
							creationState
					);
				}
				else {
					foreignKeyDomainResult = foreignKeyDescriptor.createTargetDomainResult(
							fetchablePath,
							parentTableGroup,
							fetchParent,
							creationState
					);
				}
			}
			finally {
				creationState.setResolvingCircularFetch( false );
			}
			return new CircularFetchImpl(
					this,
					fetchTiming,
					fetchablePath,
					fetchParent,
					isSelectByUniqueKey( sideNature ),
					parentNavigablePath,
					foreignKeyDomainResult,
					creationState
			);
		}
		return null;
	}

	protected boolean isBidirectionalAttributeName(
			NavigablePath parentNavigablePath,
			ModelPart parentModelPart,
			NavigablePath fetchablePath,
			DomainResultCreationState creationState) {
		if ( bidirectionalAttributePath == null ) {
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
			if ( parentModelPart instanceof ToOneAttributeMapping toOneAttributeMapping ) {
				if ( toOneAttributeMapping.bidirectionalAttributePath != null ) {
					return toOneAttributeMapping.isBidirectionalAttributeName(
							fetchablePath,
							this,
							parentNavigablePath,
							creationState
					);
				}
			}
			else if ( parentModelPart instanceof PluralAttributeMapping pluralAttributeMapping ) {
				// The parent must be non-null. If it is null, the root is a CollectionResult
				return parentNavigablePath.getParent() != null
					&& pluralAttributeMapping.isBidirectionalAttributeName( fetchablePath, this );
			}
			else if ( parentModelPart instanceof EntityCollectionPart ) {
				NavigablePath parentOfParent = parentNavigablePath.getParent();
				if ( parentOfParent instanceof EntityIdentifierNavigablePath ) {
					parentOfParent = parentOfParent.getParent();
				}
				// The parent must be non-null. If it is null, the root is a CollectionResult
				return parentOfParent.getParent() != null
					&& ( (PluralAttributeMapping) creationState.resolveModelPart( parentOfParent ) )
							.isBidirectionalAttributeName( fetchablePath, this );
			}
			return false;
		}
		else if ( isParentEmbeddedCollectionPart( creationState, parentNavigablePath.getParent() ) ) {
			/*
				class EntityA{
					@OneToOne(mappedBy = "identicallyNamedAssociation", fetch = FetchType.EAGER)
					private EntityB b;
				}

				class EntityB {
					@OneToOne
					private EntityA identicallyNamedAssociation;

					private EmbeddableB embeddable;
				}

				@Embeddable
				class EmbeddableB {
					>>>>>>>> this association is not bidirectional <<<<<<<<
					@OneToOne
					private EntityA identicallyNamedAssociation;
				}

			 */
			return false;
		}
		else if ( cardinality == Cardinality.MANY_TO_ONE ) {
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
			final NavigablePath grandparentNavigablePath = parentNavigablePath.getParent();
			if ( parentNavigablePath.getLocalName().equals( CollectionPart.Nature.ELEMENT.getName() )
					&& grandparentNavigablePath != null
					&& grandparentNavigablePath.isSuffix( bidirectionalAttributePath ) ) {
				final NavigablePath parentPath = grandparentNavigablePath.getParent();
				// This can be null for a collection loader
				if ( parentPath == null ) {
					final String fullPath =
							entityMappingType.findByPath( bidirectionalAttributePath )
									.getNavigableRole().getFullPath();
					return grandparentNavigablePath.getFullPath().equals( fullPath );
				}
				else {
					// If the parent is null, this is a simple collection fetch of a root, in which case the types must match
					if ( parentPath.getParent() == null ) {
						final String entityName = entityMappingType.getPartName();
						return parentPath.getFullPath().startsWith( entityName )
							&& ( parentPath.getFullPath().length() == entityName.length()
								// Ignore a possible alias
								|| parentPath.getFullPath().charAt( entityName.length() ) == '(' );
					}
					// If we have a parent, we ensure that the parent is the same as the attribute name
					else {
						return parentPath.getLocalName().equals( navigableRole.getLocalName() );
					}
				}
			}
			return false;
		}

		NavigablePath navigablePath = parentNavigablePath.trimSuffix( bidirectionalAttributePath );
		if ( navigablePath != null ) {
			final String localName = navigablePath.getLocalName();
			if ( localName.equals( EntityIdentifierMapping.ID_ROLE_NAME )
					|| localName.equals( ForeignKeyDescriptor.PART_NAME )
					|| localName.equals( ForeignKeyDescriptor.TARGET_PART_NAME ) ) {
				navigablePath = navigablePath.getParent();
			}
			return creationState.resolveModelPart( navigablePath ).getPartMappingType() == entityMappingType;
		}
		return false;
	}

	private boolean isParentEmbeddedCollectionPart(DomainResultCreationState creationState, NavigablePath parentNavigablePath) {
		while ( parentNavigablePath != null ) {
			final ModelPart parentModelPart = creationState.resolveModelPart( parentNavigablePath );
			if ( parentModelPart instanceof EmbeddedCollectionPart ) {
				return true;
			}
			else if ( parentModelPart instanceof EmbeddableValuedModelPart ) {
				parentNavigablePath = parentNavigablePath.getParent();
			}
			else {
				return false;
			}
		}
		return false;
	}

	private Fetch createCircularBiDirectionalFetch(
			NavigablePath fetchablePath,
			FetchParent fetchParent,
			NavigablePath parentNavigablePath,
			FetchTiming fetchTiming,
			DomainResultCreationState creationState) {
		final NavigablePath referencedNavigablePath;
		final boolean hasBidirectionalFetchParent;
		FetchParent realFetchParent = fetchParent;
		// Traverse up the embeddable fetches
		while ( realFetchParent.getNavigablePath() != parentNavigablePath ) {
			realFetchParent = ( (Fetch) fetchParent ).getFetchParent();
		}
		if ( parentNavigablePath.getParent() == null ) {
			referencedNavigablePath = parentNavigablePath;
			hasBidirectionalFetchParent = true;
		}
		else if ( CollectionPart.Nature.fromNameExact( parentNavigablePath.getLocalName() ) != null ) {
			referencedNavigablePath = getReferencedNavigablePath( creationState, parentNavigablePath.getParent() );
			hasBidirectionalFetchParent = fetchParent instanceof Fetch fetch && fetch.getFetchParent() instanceof Fetch;
		}
		else {
			referencedNavigablePath = getReferencedNavigablePath( creationState, parentNavigablePath );
			hasBidirectionalFetchParent = fetchParent instanceof Fetch;
		}

		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final FromClauseAccess fromClauseAccess = sqlAstCreationState.getFromClauseAccess();
		// The referencedNavigablePath can be null if this is a collection initialization
		if ( referencedNavigablePath != null ) {
			// If this is the key side, we must ensure that the key is not null, so we create a domain result for it
			// In the CircularBiDirectionalFetchImpl we return null if the key is null instead of the bidirectional value
			final DomainResult<?> keyDomainResult = keyDomainResult(
					fetchablePath,
					fetchParent,
					creationState,
					sqlAstCreationState,
					realFetchParent
			);

			if ( hasBidirectionalFetchParent ) {
				return new CircularBiDirectionalFetchImpl(
						FetchTiming.IMMEDIATE,
						fetchablePath,
						fetchParent,
						this,
						referencedNavigablePath,
						keyDomainResult
				);
			}
			else {
				// A query like `select ch from Phone p join p.callHistory ch` returns collection element domain results
				// but detects that Call#phone is bidirectional in the query.
				// The problem with a bidirectional fetch though is that we can't find an initializer
				// because there is none, as we don't fetch the data of the parent node.
				// To avoid creating another join, we create a special join fetch that uses the existing joined data
				final TableGroup tableGroup = fromClauseAccess.getTableGroup( referencedNavigablePath );
				fromClauseAccess.registerTableGroup( fetchablePath, tableGroup );
				// Register a PROJECTION usage as we're effectively selecting the bidirectional association
				sqlAstCreationState.registerEntityNameUsage(
						tableGroup,
						EntityNameUse.PROJECTION,
						entityMappingType.getEntityName()
				);
				return buildEntityFetchJoined(
						fetchParent,
						this,
						tableGroup,
						keyDomainResult,
						false,
						fetchablePath,
						creationState
				);
			}
		}
		else {
			// We get here is this is a lazy collection initialization for which we know the owner is in the PC
			// So we create a delayed fetch, as we are sure to find the entity in the PC
			final TableGroup parentTableGroup = fromClauseAccess.getTableGroup( parentNavigablePath );
			final DomainResult<?> domainResult =
					domainResult( fetchablePath, fetchParent, creationState, parentTableGroup );
			if ( fetchTiming == FetchTiming.IMMEDIATE ) {
				return buildEntityFetchSelect(
						fetchParent,
						this,
						fetchablePath,
						domainResult,
						isSelectByUniqueKey( sideNature ),
						false,
						creationState
				);
			}

			if ( requiresJoinForDelayedFetch() ) {
				createTableGroupForDelayedFetch( fetchablePath, parentTableGroup, null, creationState );
			}

			return buildEntityDelayedFetch(
					fetchParent,
					this,
					fetchablePath,
					domainResult,
					isSelectByUniqueKey( sideNature ),
					creationState
			);
		}
	}

	private DomainResult<?> keyDomainResult(
			NavigablePath fetchablePath,
			FetchParent fetchParent,
			DomainResultCreationState creationState,
			SqlAstCreationState sqlAstCreationState,
			FetchParent realFetchParent) {
		// For now, we don't do this if the key table is nullable to avoid an additional join
		if ( sideNature == ForeignKeyDescriptor.Nature.KEY && !isKeyTableNullable ) {
			return foreignKeyDescriptor.createKeyDomainResult(
					fetchablePath,
					createTableGroupForDelayedFetch(
							fetchablePath,
							sqlAstCreationState.getFromClauseAccess()
									.findTableGroup( realFetchParent.getNavigablePath() ),
							null,
							creationState
					),
					fetchParent,
					creationState
			);
		}
		else {
			return null;
		}
	}

	private DomainResult<?> domainResult(
			NavigablePath fetchablePath,
			FetchParent fetchParent,
			DomainResultCreationState creationState,
			TableGroup parentTableGroup) {
		if ( sideNature == ForeignKeyDescriptor.Nature.KEY ) {
			return foreignKeyDescriptor.createKeyDomainResult(
					fetchablePath,
					createTableGroupForDelayedFetch( fetchablePath, parentTableGroup, null, creationState ),
					fetchParent,
					creationState
			);
		}
		else {
			return foreignKeyDescriptor.createTargetDomainResult(
					fetchablePath,
					parentTableGroup,
					fetchParent,
					creationState
			);
		}
	}

	/**
	 * For Hibernate Reactive
	 */
	protected EntityFetch buildEntityDelayedFetch(
			FetchParent fetchParent,
			ToOneAttributeMapping fetchedAttribute,
			NavigablePath navigablePath,
			DomainResult<?> keyResult,
			boolean selectByUniqueKey,
			DomainResultCreationState creationState) {
		return new EntityDelayedFetchImpl(
				fetchParent,
				fetchedAttribute,
				navigablePath,
				keyResult,
				selectByUniqueKey,
				creationState
		);
	}

	/**
	 * For Hibernate Reactive
	 */
	protected EntityFetch buildEntityFetchSelect(
			FetchParent fetchParent,
			ToOneAttributeMapping fetchedAttribute,
			NavigablePath navigablePath,
			DomainResult<?> keyResult,
			boolean selectByUniqueKey,
			boolean isAffectedByFilter,
			@SuppressWarnings("unused") DomainResultCreationState creationState) {
		return new EntityFetchSelectImpl(
				fetchParent,
				fetchedAttribute,
				navigablePath,
				keyResult,
				selectByUniqueKey,
				isAffectedByFilter,
				creationState
		);
	}

	/**
	 * For Hibernate Reactive
	 */
	protected EntityFetch buildEntityFetchJoined(
			FetchParent fetchParent,
			ToOneAttributeMapping toOneMapping,
			TableGroup tableGroup,
			DomainResult<?> keyResult,
			boolean isAffectedByFilter,
			NavigablePath navigablePath,
			DomainResultCreationState creationState) {
		return new EntityFetchJoinedImpl(
				fetchParent,
				toOneMapping,
				tableGroup,
				keyResult,
				isAffectedByFilter,
				navigablePath,
				creationState
		);
	}

	private NavigablePath getReferencedNavigablePath(
			DomainResultCreationState creationState,
			NavigablePath parentNavigablePath) {
		/*
				class LineItem {
					@ManyToOne
					Order order;
				}

				class Order {
					@OneToOne(mappedBy = "order")
					Payment payment;

					@OneToOne
					LineItem sampleLineItem;
				}

				class Payment {
					@OneToOne
					Order order;
				}

				When we have `Payment -> order -> LIneItem -> Order -> payment`
				we need to navigate back till we find the root Payment and use ir as `referencedNavigablePath` (partMappingType == entityMappingType)

				In case of polymorphism

				class Level1 {
					@OneToOne(mappedBy = "level1Parent")
					DerivedLevel2 level2Child;
				}

				class Level2 {
					@OneToOne(mappedBy = "level2Parent")
					Level3 level3Child;
				}

				class DerivedLevel2 extends Level2 {
					@OneToOne
					Level1 level1Parent;
				}

				class Level3 {
					@OneToOne
					Level2 level2Parent;
				}

				We have Level1->leve2Child->level3Child->level2Parent

				where leve2Child is of type DerivedLevel2 while level2Parent of type Level2

				for this reason we need the check entityMappingType.isSubclassEntityName( partMappingType.getMappedJavaType().getTypeName() )
				to be sure that the referencedNavigablePath corresponds to leve2Child

		 */
		NavigablePath referencedNavigablePath = parentNavigablePath.getParent();
		MappingType partMappingType = creationState.resolveModelPart( referencedNavigablePath ).getPartMappingType();
		while ( !( partMappingType instanceof EntityMappingType entityMapping )
				|| ( partMappingType != entityMappingType
				&& !entityMappingType.getEntityPersister().isSubclassEntityName( partMappingType.getMappedJavaType().getTypeName() )
				&& !entityMapping.getEntityPersister().isSubclassEntityName( entityMappingType.getEntityName() ) ) ) {
			referencedNavigablePath = referencedNavigablePath.getParent();
			if ( referencedNavigablePath == null ) {
				return null;
			}
			partMappingType = creationState.resolveModelPart( referencedNavigablePath ).getPartMappingType();
		}
		return referencedNavigablePath;
	}

	@Override
	public EntityFetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {

		assert fetchablePath.getParent().equals( fetchParent.getNavigablePath() )
			|| fetchParent.getNavigablePath() instanceof TreatedNavigablePath
					&& fetchablePath.getParent().equals( fetchParent.getNavigablePath().getRealParent() );

		/*
			If selected is true, we're going to add a fetch for the fetchablePath only if
			there is not yet a TableGroupJoin. For example, given:

				public static class EntityA {
					...

				@ManyToOne(fetch = FetchType.EAGER)
				private EntityB entityB;
				}

				@Entity(name = "EntityB")
				public static class EntityB {
					...

					private String name;
				}

			Then, with the HQL query:

				Select a From EntityA a Left Join a.entityB b Where (b.name IS NOT NULL)

			having the 'left join', we don't want to add an extra implicit join that will be
			translated into an SQL inner join (see HHH-15342).
		*/

		final FromClauseAccess fromClauseAccess = creationState.getSqlAstCreationState().getFromClauseAccess();
		final TableGroup parentTableGroup = fromClauseAccess.getTableGroup( fetchParent.getNavigablePath() );

		final ForeignKeyDescriptor.Nature side =
				creationState.getCurrentlyResolvingForeignKeyPart() == ForeignKeyDescriptor.Nature.KEY
					&& sideNature == ForeignKeyDescriptor.Nature.TARGET
						// If we are currently resolving the key part of a foreign key we do not want to add joins.
						// So if the lhs of this association is the target of the FK, we have to use the KEY part to avoid a join
						? ForeignKeyDescriptor.Nature.KEY
						: sideNature;

		if ( fetchTiming == FetchTiming.IMMEDIATE && selected || needsJoinFetch( side ) ) {
			final TableGroup tableGroup = determineTableGroupForFetch(
					fetchablePath,
					fetchParent,
					parentTableGroup,
					resultVariable,
					fromClauseAccess,
					creationState
			);

			return withRegisteredAssociationKeys(
					() -> {
						// When a filter exists that affects a singular association, we have to enable NotFound handling
						// to force an exception if the filter would result in the entity not being found.
						// If we silently just read null, this could lead to data loss on flush.
						final boolean affectedByEnabledFilters = isAffectedByEnabledFilters( creationState );
						return buildEntityFetchJoined(
								fetchParent,
								this,
								tableGroup,
								keyResult(
										fetchParent,
										fetchablePath,
										creationState,
										affectedByEnabledFilters,
										tableGroup,
										parentTableGroup
								),
								affectedByEnabledFilters,
								fetchablePath,
								creationState
						);
					},
					creationState
			);
		}
		else {
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

			final DomainResult<?> keyResult = keyResult( fetchParent, fetchablePath, creationState, side, parentTableGroup );
			final boolean selectByUniqueKey = isSelectByUniqueKey( side );

			if ( needsImmediateFetch( fetchTiming ) ) {
				return buildEntityFetchSelect(
						fetchParent,
						this,
						fetchablePath,
						keyResult,
						selectByUniqueKey,
						isAffectedByEnabledFilters( creationState ),
						creationState
				);
			}
			else {
				if ( requiresJoinForDelayedFetch() ) {
					createTableGroupForDelayedFetch( fetchablePath, parentTableGroup, null, creationState );
				}

				return buildEntityDelayedFetch(
						fetchParent,
						this,
						fetchablePath,
						keyResult,
						selectByUniqueKey,
						creationState
				);
			}
		}
	}

	private DomainResult<?> keyResult(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			DomainResultCreationState creationState,
			boolean affectedByEnabledFilters,
			TableGroup tableGroup,
			TableGroup parentTableGroup) {
		if ( sideNature == ForeignKeyDescriptor.Nature.KEY ) {
			// If the key side is non-nullable we also need to add the keyResult
			// to be able to manually check invalid foreign key references
			if ( hasNotFoundAction() || !isInternalLoadNullable || affectedByEnabledFilters ) {
				return foreignKeyDescriptor.createKeyDomainResult(
						fetchablePath,
						tableGroup,
						fetchParent,
						creationState
				);
			}
		}
		else if ( hasNotFoundAction() || getAssociatedEntityMappingType().getSoftDeleteMapping() != null ) {
			// For the target side only add keyResult when a not-found action is present
			return foreignKeyDescriptor.createTargetDomainResult(
					fetchablePath,
					parentTableGroup,
					fetchParent,
					creationState
			);
		}
		return null;
	}

	private DomainResult<?> keyResult(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			DomainResultCreationState creationState,
			ForeignKeyDescriptor.Nature side,
			TableGroup parentTableGroup) {
		if ( side == ForeignKeyDescriptor.Nature.KEY ) {
			return foreignKeyDescriptor.createKeyDomainResult(
					fetchablePath,
					sideNature == ForeignKeyDescriptor.Nature.KEY
							? createTableGroupForDelayedFetch( fetchablePath, parentTableGroup, null, creationState )
							: parentTableGroup,
					fetchParent,
					creationState
			);
		}
		else {
			return foreignKeyDescriptor.createTargetDomainResult(
					fetchablePath,
					sideNature == ForeignKeyDescriptor.Nature.TARGET
							? parentTableGroup
							: createTableGroupForDelayedFetch( fetchablePath, parentTableGroup, null, creationState ),
					fetchParent,
					creationState
			);
		}
	}

	private boolean needsJoinFetch(ForeignKeyDescriptor.Nature side) {
		if ( side == ForeignKeyDescriptor.Nature.TARGET ) {
			// The target model part doesn't correspond to the identifier of the target entity mapping
			// so we must eagerly fetch with a join (subselect would still cause problems).
			final EntityIdentifierMapping identifier = entityMappingType.getIdentifierMapping();
			final ValuedModelPart targetPart = foreignKeyDescriptor.getTargetPart();
			if ( identifier != targetPart ) {
				// If the identifier and the target part of the same class, we can preserve laziness as deferred loading will still work
				return identifier.getExpressibleJavaType().getJavaTypeClass()
					!= targetPart.getExpressibleJavaType().getJavaTypeClass();
			}
		}

		return false;
	}

	private boolean isAffectedByEnabledFilters(DomainResultCreationState creationState) {
		final LoadQueryInfluencers loadQueryInfluencers = creationState.getSqlAstCreationState()
				.getLoadQueryInfluencers();
		return entityMappingType.isAffectedByEnabledFilters( loadQueryInfluencers, true );
	}

	private boolean needsImmediateFetch(FetchTiming fetchTiming) {
		if ( fetchTiming == FetchTiming.IMMEDIATE ) {
			return true;
		}
		else if ( !entityMappingType.isConcreteProxy() ) {
			// Consider all associations annotated with @NotFound as EAGER
			// and LAZY one-to-one that are not instrumented and not optional.
			// When resolving the concrete entity type we can preserve laziness
			// and handle not found actions based on the discriminator value
			return hasNotFoundAction()
				|| entityMappingType.getSoftDeleteMapping() != null
				|| isOptional && cardinality == Cardinality.ONE_TO_ONE
					&& !entityMappingType.getEntityPersister().isInstrumented();
		}
		else {
			return false;
		}
	}

	private TableGroup determineTableGroupForFetch(
			NavigablePath fetchablePath,
			FetchParent fetchParent,
			TableGroup parentTableGroup,
			String resultVariable,
			FromClauseAccess fromClauseAccess,
			DomainResultCreationState creationState) {
		final SqlAstJoinType joinType = joinType( fetchablePath, fetchParent, parentTableGroup );
		final TableGroup existingTableGroup = fromClauseAccess.findTableGroupForGetOrCreate( fetchablePath );
		if ( existingTableGroup != null && existingTableGroup.getModelPart() == this ) {
			return existingTableGroup;
		}
		else {
			// Try to reuse an existing join if possible,
			// and note that we prefer reusing an inner over a left join,
			// because a left join might stay uninitialized if unused
			TableGroup leftJoined = null;
			for ( TableGroupJoin tableGroupJoin : parentTableGroup.getTableGroupJoins() ) {
				if ( tableGroupJoin.getJoinedGroup().getModelPart() == this ) {
					switch ( tableGroupJoin.getJoinType() ) {
						case INNER:
							// If this is an inner joins, it's fine if the paths match
							// Since this inner join would filter the parent row anyway,
							// it makes no sense to add another left join for this association
							if ( tableGroupJoin.getNavigablePath().pathsMatch( fetchablePath ) ) {
								return tableGroupJoin.getJoinedGroup();
							}
							break;
						case LEFT:
							// For an existing left join on the other hand which is row preserving,
							// it is important to check if the predicate has user defined bits in it
							// and only if it doesn't, we can reuse the join
							if ( tableGroupJoin.getNavigablePath().pathsMatch( fetchablePath )
									&& isSimpleJoinPredicate( tableGroupJoin.getPredicate() ) ) {
								leftJoined = tableGroupJoin.getJoinedGroup();
							}
					}
				}
			}

			if ( leftJoined != null ) {
				return leftJoined;
			}

			final TableGroupJoin tableGroupJoin = createTableGroupJoin(
					fetchablePath,
					parentTableGroup,
					resultVariable,
					null,
					joinType,
					true,
					false,
					creationState.getSqlAstCreationState()
			);
			parentTableGroup.addTableGroupJoin( tableGroupJoin );
			final TableGroup joinedGroup = tableGroupJoin.getJoinedGroup();
			fromClauseAccess.registerTableGroup( fetchablePath, joinedGroup );
			return joinedGroup;
		}
	}

	private SqlAstJoinType joinType(NavigablePath fetchablePath, FetchParent fetchParent, TableGroup parentTableGroup) {
		return fetchParent.getReferencedMappingType() instanceof JoinedSubclassEntityPersister joinedSubclassEntityPersister
			&& joinedSubclassEntityPersister.findDeclaredAttributeMapping( getPartName() ) == null
				? getJoinTypeForFetch( fetchablePath, parentTableGroup )
				: null;
	}

	private TableGroup createTableGroupForDelayedFetch(
			NavigablePath fetchablePath,
			TableGroup parentTableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		// Check if we can reuse a table group join of the parent
		final TableGroup compatibleTableGroup =
				parentTableGroup.findCompatibleJoinedGroup( this, SqlAstJoinType.LEFT );
		if ( compatibleTableGroup != null ) {
			return compatibleTableGroup;
		}
		else {
			// We have to create the table group that points to the target so that table reference resolving works
			final var sqlAstCreationState = creationState.getSqlAstCreationState();
			final TableGroupJoin tableGroupJoin = createTableGroupJoin(
					fetchablePath,
					parentTableGroup,
					resultVariable,
					null,
					SqlAstJoinType.LEFT,
					false,
					false,
					sqlAstCreationState
			);
			parentTableGroup.addTableGroupJoin( tableGroupJoin );
			final TableGroup joinedGroup = tableGroupJoin.getJoinedGroup();
			sqlAstCreationState.getFromClauseAccess()
					.registerTableGroup( fetchablePath, joinedGroup );
			return joinedGroup;
		}
	}

	private boolean isSelectByUniqueKey(ForeignKeyDescriptor.Nature side) {
		if ( referencedPropertyName == null ) {
			return false;
		}
		final EntityIdentifierMapping identifierMapping = entityMappingType.getIdentifierMapping();
		if ( side == ForeignKeyDescriptor.Nature.KEY ) {
			// case 1.2
			return !foreignKeyDescriptor.getNavigableRole()
					.equals( identifierMapping.getNavigableRole() );
		}
		else {
			// case 1.1
			// Make sure the entity identifier is not a target key property i.e. this really is a unique key mapping
			return bidirectionalAttributePath != null
				&& !( identifierMapping instanceof SingleAttributeIdentifierMapping
						&& targetKeyPropertyNames.contains( identifierMapping.getAttributeName() ) );
		}
	}

	@Override
	public <T> DomainResult<T> createSnapshotDomainResult(
			NavigablePath navigablePath,
			TableGroup parentTableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		// it's a Snapshot then we just need the value of the FK when it belongs to the parentTableGroup
		if ( sideNature == ForeignKeyDescriptor.Nature.KEY ) {
			return foreignKeyDescriptor.getKeyPart().createDomainResult(
					navigablePath,
					parentTableGroup,
					resultVariable,
					creationState
			);
		}
		else {
			return new NullDomainResult( foreignKeyDescriptor.getKeyPart().getJavaType() );
		}
	}

	public static class NullDomainResult<J> implements DomainResult<J> {
		private final DomainResultAssembler<J> resultAssembler;
		private final JavaType<?> resultJavaType;

		public NullDomainResult(JavaType<J> javaType) {
			resultAssembler = new NullValueAssembler<>( javaType );
			this.resultJavaType = javaType;
		}

		@Override
		public String getResultVariable() {
			return null;
		}

		@Override
		public DomainResultAssembler<J> createResultAssembler(
				InitializerParent<?> parent,
				AssemblerCreationState creationState) {
			return resultAssembler;
		}

		@Override
		public JavaType<?> getResultJavaType() {
			return resultJavaType;
		}

		@Override
		public void collectValueIndexesToCache(BitSet valueIndexes) {
			// No-op
		}
	}

	private EntityFetch withRegisteredAssociationKeys(
			Supplier<EntityFetch> fetchCreator,
			DomainResultCreationState creationState) {
		final boolean added = creationState.registerVisitedAssociationKey( foreignKeyDescriptor.getAssociationKey() );
		final AssociationKey additionalAssociationKey = additionalAssociationKey( creationState );
		try {
			return fetchCreator.get();
		}
		finally {
			if ( added ) {
				creationState.removeVisitedAssociationKey( foreignKeyDescriptor.getAssociationKey() );
			}
			if ( additionalAssociationKey != null ) {
				creationState.removeVisitedAssociationKey( additionalAssociationKey );
			}
		}
	}

	private AssociationKey additionalAssociationKey(DomainResultCreationState creationState) {
		if ( cardinality == Cardinality.LOGICAL_ONE_TO_ONE && bidirectionalAttributePath != null ) {
			// Add the inverse association key side as well to be able to resolve to a CircularFetch
			if ( entityMappingType.findByPath( bidirectionalAttributePath )
					instanceof ToOneAttributeMapping bidirectionalAttribute ) {
				assert bidirectionalAttribute.getPartMappingType() == declaringTableGroupProducer;
				final AssociationKey secondKey = bidirectionalAttribute.getForeignKeyDescriptor().getAssociationKey();
				if ( creationState.registerVisitedAssociationKey( secondKey ) ) {
					return secondKey;
				}
			}
		}
		return null;
	}

	@Override
	public SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup) {
		if ( isKeyTableNullable || isNullable
				|| parentTableGroup.getModelPart() instanceof CollectionPart
				|| !parentTableGroup.canUseInnerJoins() ) {
			return SqlAstJoinType.LEFT;
		}
		else  {
			final Class<?> attributeDeclaringType = declaringTableGroupProducer.getJavaType().getJavaTypeClass();
			final Class<?> parentTableGroupType = parentTableGroup.getModelPart().getJavaType().getJavaTypeClass();
			// This attribute mapping must be declared on the parent table group type or one of its super types
			// If not, this is a fetch for a subtype of the parent table group, which might be left joined
			return attributeDeclaringType.isAssignableFrom( parentTableGroupType )
					? SqlAstJoinType.INNER
					: SqlAstJoinType.LEFT;
		}
	}

	@Override
	public boolean isSimpleJoinPredicate(Predicate predicate) {
		// Since the table group is lazy, the initial predicate is null,
		// but if we get null here, we can safely assume this will be a simple join predicate
		return predicate == null || foreignKeyDescriptor.isSimpleJoinPredicate( predicate );
	}

	@Override
	public boolean containsTableReference(String tableExpression) {
		return getEntityMappingType().containsTableReference( tableExpression );
	}

	@Override
	public int getNumberOfFetchables() {
		return getEntityMappingType().getNumberOfFetchables();
	}

	@Override
	public Fetchable getFetchable(int position) {
		return getEntityMappingType().getFetchable( position );
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable SqlAstJoinType requestedJoinType,
			boolean fetched,
			boolean addsPredicate,
			SqlAstCreationState creationState) {
		// Make sure the lhs is never a plural table group directly, but always a table group for a part
		// This is vital for the map key property check that comes next
		assert !( lhs instanceof PluralTableGroup );

		final FromClauseAccess fromClauseAccess = creationState.getFromClauseAccess();
		final SqlAstJoinType joinType = determineSqlJoinType( lhs, requestedJoinType, fetched );

		// If a parent is a collection part, there is no custom predicate and the join is INNER or LEFT
		// we check if this attribute is the map key property to reuse the existing index table group
		if ( !addsPredicate && ( joinType == SqlAstJoinType.INNER || joinType == SqlAstJoinType.LEFT ) ) {
			final TableGroupJoin tableGroupJoin =
					createTableGroupJoin(
							navigablePath,
							lhs,
							requestedJoinType,
							fetched,
							fromClauseAccess,
							joinType
					);
			if ( tableGroupJoin != null ) {
				return tableGroupJoin;
			}
		}

		final LazyTableGroup lazyTableGroup = createRootTableGroupJoin(
				navigablePath,
				lhs,
				explicitSourceAlias,
				explicitSqlAliasBase,
				requestedJoinType,
				fetched,
				null,
				creationState
		);

		final TableGroupJoin join = new TableGroupJoin(
				navigablePath,
				// Avoid checking for nested joins in here again, since this is already done in createRootTableGroupJoin
				// and simply rely on the canUseInnerJoins flag instead for override the join type to LEFT
				requestedJoinType == null && !lazyTableGroup.canUseInnerJoins()
						? SqlAstJoinType.LEFT
						: joinType,
				lazyTableGroup,
				null
		);
		final TableReference lhsTableReference = lhs.resolveTableReference(
				navigablePath,
				this,
				identifyingColumnsTableExpression
		);

		lazyTableGroup.setTableGroupInitializerCallback(
				tableGroup -> {
					final TableReference targetTableReference;
					final TableReference keyTableReference;
					if ( sideNature == ForeignKeyDescriptor.Nature.TARGET ) {
						targetTableReference = lhsTableReference;
						keyTableReference = tableGroup.resolveTableReference( foreignKeyDescriptor.getKeyTable() );
					}
					else {
						targetTableReference = tableGroup.resolveTableReference( foreignKeyDescriptor.getTargetTable() );
						keyTableReference = lhsTableReference;
					}

					join.applyPredicate( foreignKeyDescriptor.generateJoinPredicate(
							targetTableReference,
							keyTableReference,
							creationState
					) );

					final EntityMappingType associatedEntityMappingType = getAssociatedEntityMappingType();

					// Note specifically we only apply `@Filter` restrictions that are applyToLoadByKey = true
					// to make the behavior consistent with lazy loading of an association
					if ( associatedEntityMappingType.getEntityPersister().hasFilterForLoadByKey() ) {
						associatedEntityMappingType.applyBaseRestrictions(
								join::applyPredicate,
								tableGroup,
								true,
								creationState.getLoadQueryInfluencers().getEnabledFilters(),
								creationState.applyOnlyLoadByKeyFilters(),
								null,
								creationState
						);
					}
					// @SQLRestriction should not be applied when joining FK association,
					// because that would result in us setting the FK to null when the
					// owning entity is updated, that is, to data loss.
					// But we let it apply on the TARGET side of a @OneToOne, and we apply
					// it whenever there is a dedicated join table.
					if ( canAddRestriction() ) {
						associatedEntityMappingType.applyWhereRestrictions(
								join::applyPredicate,
								tableGroup,
								true,
								creationState
						);
					}
					if ( associatedEntityMappingType.getSuperMappingType() != null && !creationState.supportsEntityNameUsage() ) {
						associatedEntityMappingType.applyDiscriminator( null, null, tableGroup, creationState );
					}

					final SoftDeleteMapping softDeleteMapping = associatedEntityMappingType.getSoftDeleteMapping();
					if ( softDeleteMapping != null ) {
						// add the restriction
						final TableReference tableReference = lazyTableGroup.resolveTableReference(
								navigablePath,
								associatedEntityMappingType.getSoftDeleteTableDetails().getTableName()
						);
						join.applyPredicate( softDeleteMapping.createNonDeletedRestriction(
								tableReference,
								creationState.getSqlExpressionResolver()
						) );
					}
				}
		);

		return join;
	}

	private TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			SqlAstJoinType requestedJoinType,
			boolean fetched,
			FromClauseAccess fromClauseAccess,
			SqlAstJoinType joinType) {

		StringBuilder embeddablePath = null;
		TableGroup parentTableGroup = lhs;
		ModelPartContainer parentContainer = lhs.getModelPart();
		// Traverse up embeddable table groups until we find a table group for a collection part
		while ( !( parentContainer instanceof CollectionPart ) ) {
			if ( parentContainer instanceof EmbeddableValuedModelPart ) {
				if ( embeddablePath == null ) {
					embeddablePath = new StringBuilder();
				}
				embeddablePath.insert( 0, parentContainer.getPartName() + "." );
				final NavigablePath parentNavigablePath = parentTableGroup.getNavigablePath();
				final TableGroup tableGroup = fromClauseAccess.findTableGroup( parentNavigablePath.getParent() );
				if ( tableGroup == null ) {
					assert parentNavigablePath.getLocalName().equals( ForeignKeyDescriptor.PART_NAME )
						|| parentNavigablePath.getLocalName().equals( ForeignKeyDescriptor.TARGET_PART_NAME );
					// Might happen that we don't register a table group for the collection role if this is a
					// foreign key part and the collection is delayed. We can just break out in this case though,
					// since these checks here are only for reusing a map key property, which we won't have
					break;
				}
				parentTableGroup = tableGroup;
				parentContainer = tableGroup.getModelPart();
			}
			else {
				break;
			}
		}

		return createTableGroupJoin(
				navigablePath,
				lhs,
				requestedJoinType,
				fetched,
				fromClauseAccess,
				joinType,
				parentTableGroup,
				embeddablePath
		);
	}

	private TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			SqlAstJoinType requestedJoinType,
			boolean fetched,
			FromClauseAccess fromClauseAccess,
			SqlAstJoinType joinType,
			TableGroup parentTableGroup,
			StringBuilder embeddablePath) {
		final NavigablePath parentGroupNavigablePath = parentTableGroup.getNavigablePath();
		if ( CollectionPart.Nature.ELEMENT.getName().equals( parentGroupNavigablePath.getLocalName() ) ) {
			final NavigablePath parentParentPath = parentGroupNavigablePath.getParent();
			final PluralTableGroup pluralTableGroup =
					(PluralTableGroup) fromClauseAccess.findTableGroup( parentParentPath );
			if ( pluralTableGroup != null ) {
				final String indexPropertyName =
						pluralTableGroup.getModelPart().getIndexMetadata().getIndexPropertyName();
				final String pathName =
						embeddablePath == null
								? getAttributeName()
								: embeddablePath.append( getAttributeName() ).toString();
				if ( pathName.equals( indexPropertyName ) ) {
					final TableGroup indexTableGroup = pluralTableGroup.getIndexTableGroup();
					// If this is the map key property, we can reuse the index table group
					initializeIfNeeded( lhs, requestedJoinType, indexTableGroup );
					return new TableGroupJoin(
							navigablePath,
							joinType,
							new MappedByTableGroup(
									navigablePath,
									this,
									indexTableGroup,
									fetched,
									pluralTableGroup,
									this
							),
							null
					);
				}
			}
		}
		return null;
	}

	@Override
	public SqlAstJoinType determineSqlJoinType(TableGroup lhs, @Nullable SqlAstJoinType requestedJoinType, boolean fetched) {
		if ( requestedJoinType != null ) {
			return requestedJoinType;
		}
		else if ( fetched ) {
			return getDefaultSqlAstJoinType( lhs );
		}
		else {
			return SqlAstJoinType.INNER;
		}
	}

	@Override
	public LazyTableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable SqlAstJoinType requestedJoinType,
			boolean fetched,
			@Nullable Consumer<Predicate> predicateConsumer,
			SqlAstCreationState creationState) {
		final SqlAliasBase sqlAliasBase = SqlAliasBase.from(
				explicitSqlAliasBase,
				explicitSourceAlias,
				this,
				creationState.getSqlAliasBaseGenerator()
		);

		final EntityMappingType associatedEntityMappingType = getAssociatedEntityMappingType();
		final SoftDeleteMapping softDeleteMapping = associatedEntityMappingType.getSoftDeleteMapping();
		final boolean canUseInnerJoin = canUseInnerJoin( lhs, requestedJoinType, fetched, creationState );

		final TableGroup realParentTableGroup = realParentTableGroup( lhs, creationState );

		// If the parent is a correlated table group, and we're explicitly joining, we can't refer to columns of the
		// table in the outer query, because the context in which a column is used could be an aggregate function.
		// Using a parent column in such a case would lead to an error if the parent query lacks a proper group by
		final TableGroupProducer tableGroupProducer =
				requestedJoinType != null && realParentTableGroup instanceof CorrelatedTableGroup
						? entityMappingType
						: this;

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
						creationState
				),
				this,
				tableGroupProducer,
				explicitSourceAlias,
				sqlAliasBase,
				associatedEntityMappingType.getEntityPersister().getFactory(),
				lhs
		);

		if ( predicateConsumer != null ) {
			final TableReference lhsTableReference =
					lhs.resolveTableReference( navigablePath, identifyingColumnsTableExpression );
			final boolean targetSide = sideNature == ForeignKeyDescriptor.Nature.TARGET;
			lazyTableGroup.setTableGroupInitializerCallback(
					tableGroup -> predicateConsumer.accept(
							foreignKeyDescriptor.generateJoinPredicate(
									targetSide ? lhsTableReference : tableGroup.getPrimaryTableReference(),
									targetSide ? tableGroup.getPrimaryTableReference() : lhsTableReference,
									creationState
							)
					)
			);

			if ( fetched && softDeleteMapping != null ) {
				// add the restriction
				final TableReference tableReference = lazyTableGroup.resolveTableReference(
						navigablePath,
						associatedEntityMappingType.getSoftDeleteTableDetails().getTableName()
				);
				predicateConsumer.accept( softDeleteMapping.createNonDeletedRestriction(
						tableReference,
						creationState.getSqlExpressionResolver()
				) );
			}
		}

		if ( requestedJoinType != null && realParentTableGroup instanceof CorrelatedTableGroup ) {
			// Force initialization of the underlying table group join to retain cardinality
			lazyTableGroup.getPrimaryTableReference();
		}
		else {
			initializeIfNeeded( lhs, requestedJoinType, lazyTableGroup );
		}

		return lazyTableGroup;
	}

	private static TableGroup realParentTableGroup(TableGroup lhs, SqlAstCreationState creationState) {
		TableGroup realParentTableGroup = lhs;
		final FromClauseAccess fromClauseAccess = creationState.getFromClauseAccess();
		while ( realParentTableGroup.getModelPart() instanceof EmbeddableValuedModelPart ) {
			final NavigablePath parentNavigablePath = realParentTableGroup.getNavigablePath();
			final TableGroup tableGroup = fromClauseAccess.findTableGroup( parentNavigablePath.getParent() );
			if ( tableGroup == null ) {
				assert parentNavigablePath.getLocalName().equals( ForeignKeyDescriptor.PART_NAME )
					|| parentNavigablePath.getLocalName().equals( ForeignKeyDescriptor.TARGET_PART_NAME );
				// Might happen that we don't register a table group for the collection role if this is a
				// foreign key part and the collection is delayed. We can just break out in this case though,
				// since the realParentTableGroup is only relevant if this association is actually joined,
				// which it is not, because this is part of the target FK
				realParentTableGroup = null;
				break;
			}
			realParentTableGroup = tableGroup;
		}
		return realParentTableGroup;
	}

	private boolean canUseInnerJoin(
			TableGroup lhs,
			SqlAstJoinType requestedJoinType,
			boolean fetched,
			SqlAstCreationState creationState) {
		final SqlAstJoinType currentlyProcessingJoinType =
				creationState instanceof SqmToSqlAstConverter sqmToSqlAstConverter
						? sqmToSqlAstConverter.getCurrentlyProcessingJoinType()
						: null;
		if ( currentlyProcessingJoinType == null || currentlyProcessingJoinType == SqlAstJoinType.INNER ) {
			return determineSqlJoinType( lhs, requestedJoinType, fetched ) == SqlAstJoinType.INNER;
		}
		else {
			// Don't change the join type though, as that has implications for eager initialization of a LazyTableGroup
			return false;
		}
	}

	@Override
	public boolean canUseParentTableGroup(
			TableGroupProducer producer,
			NavigablePath navigablePath,
			ValuedModelPart valuedModelPart) {
		return producer == this
			&& sideNature == ForeignKeyDescriptor.Nature.KEY
			&& foreignKeyDescriptor.isKeyPart( valuedModelPart );
	}

	private void initializeIfNeeded(TableGroup lhs, SqlAstJoinType sqlAstJoinType, TableGroup tableGroup) {
		if ( sqlAstJoinType == SqlAstJoinType.INNER && ( isNullable || !lhs.canUseInnerJoins() ) ) {
			if ( hasJoinTable ) {
				// Set the join type of the table reference join to INNER to retain cardinality expectation
				final TableReference lhsTableReference =
						lhs.resolveTableReference( tableGroup.getNavigablePath(),
								identifyingColumnsTableExpression );
				final List<TableReferenceJoin> tableReferenceJoins = lhs.getTableReferenceJoins();
				for ( int i = 0; i < tableReferenceJoins.size(); i++ ) {
					final TableReferenceJoin tableReferenceJoin = tableReferenceJoins.get( i );
					if ( tableReferenceJoin.getJoinType() != SqlAstJoinType.INNER
							&& tableReferenceJoin.getJoinedTableReference() == lhsTableReference ) {
						tableReferenceJoins.set(
								i,
								new TableReferenceJoin(
										true,
										tableReferenceJoin.getJoinedTableReference(),
										tableReferenceJoin.getPredicate()
								)
						);
						return;
					}
				}
				throw new AssertionFailure( "Couldn't find table reference join for join table: " + lhsTableReference );
			}
			else {
				// Force initialization of the underlying table group join to retain cardinality
				tableGroup.getPrimaryTableReference();
			}
		}
	}

	private SqlAstJoinType getJoinTypeForFetch(NavigablePath navigablePath, TableGroup tableGroup) {
		for ( TableGroupJoin tableGroupJoin : tableGroup.getTableGroupJoins() ) {
			if ( tableGroupJoin.getNavigablePath().equals( navigablePath ) ) {
				return tableGroupJoin.getJoinType();
			}
		}
		return null;
	}

	public TableGroup createTableGroupInternal(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			boolean fetched,
			String sourceAlias,
			final SqlAliasBase sqlAliasBase,
			SqlAstCreationState creationState) {
		final EntityMappingType entityMappingType = getEntityMappingType();
		final TableReference primaryTableReference =
				entityMappingType.createPrimaryTableReference( sqlAliasBase, creationState );
		return new StandardTableGroup(
				canUseInnerJoins,
				navigablePath,
				this,
				fetched,
				sourceAlias,
				primaryTableReference,
				true,
				sqlAliasBase,
				entityMappingType.getRootEntityDescriptor()::containsTableReference,
				(tableExpression, tg) -> entityMappingType.createTableReferenceJoin(
						tableExpression,
						sqlAliasBase,
						primaryTableReference,
						creationState
				),
				entityMappingType.getEntityPersister().getFactory()
		);
	}

	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	public boolean isNullable() {
		return isNullable;
	}

	public boolean isLazy() {
		return isLazy;
	}

	@Override
	public boolean isOptional(){
		return isOptional;
	}

	public boolean isInternalLoadNullable() {
		return isInternalLoadNullable;
	}

	public NotFoundAction getNotFoundAction() {
		return notFoundAction;
	}

	public boolean isIgnoreNotFound(){
		return notFoundAction == NotFoundAction.IGNORE;
	}

	public boolean hasNotFoundAction() {
		return notFoundAction != null;
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
		return foreignKeyDescriptor.getPart( sideNature );
	}

	@Override
	public String toString() {
		return "ToOneAttributeMapping(" + navigableRole + ")@" + System.identityHashCode( this );
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		if ( cardinality == Cardinality.ONE_TO_ONE && sideNature == ForeignKeyDescriptor.Nature.TARGET ) {
			return 0;
		}

		final Object value = extractValue( domainValue, session );
		return foreignKeyDescriptor.breakDownJdbcValues( value, offset, x, y, valueConsumer, session );
	}

	protected Object extractValue(Object domainValue, SharedSessionContractImplementor session) {
		if ( domainValue == null ) {
			return null;
		}
		else {
			if ( referencedPropertyName != null ) {
				final Object initializedValue = lazyInitialize( domainValue );
				final EntityMappingType mappingType = getAssociatedEntityMappingType();
				assert mappingType.getRepresentationStrategy().getInstantiator().isInstance( initializedValue );
				return extractAttributePathValue( initializedValue, mappingType, referencedPropertyName );
			}
			else {
				return foreignKeyDescriptor.getAssociationKeyFromSide( domainValue, sideNature.inverse(), session );
			}
		}
	}

	/**
	 * For Hibernate Reactive, because it doesn't support lazy initialization, it will override this method and skip it
	 * when possible.
	 */
	protected Object lazyInitialize(Object domainValue) {
		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( domainValue );
		return lazyInitializer == null ? domainValue : lazyInitializer.getImplementation();
	}

	protected static Object extractAttributePathValue(Object domainValue, EntityMappingType entityType, String attributePath) {
		if ( attributePath.contains( "." ) ) {
			Object value = domainValue;
			ManagedMappingType managedType = entityType;
			for ( String part : split( ".", attributePath ) ) {
				assert managedType != null;
				final AttributeMapping attributeMapping = managedType.findAttributeMapping( part );
				value = attributeMapping.getValue( value );
				managedType =
						attributeMapping.getMappedType() instanceof ManagedMappingType managedMappingType
								? managedMappingType
								: null;
			}
			return value;
		}
		else {
			return entityType.findAttributeMapping( attributePath ).getValue( domainValue );
		}
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		return sideNature == ForeignKeyDescriptor.Nature.KEY
				? foreignKeyDescriptor.visitKeySelectables( offset, consumer )
				: 0;
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		if ( sideNature == ForeignKeyDescriptor.Nature.KEY ) {
			foreignKeyDescriptor.getKeyPart()
					.applySqlSelections( navigablePath, tableGroup, creationState );
		}
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		if ( sideNature == ForeignKeyDescriptor.Nature.KEY ) {
			foreignKeyDescriptor.getKeyPart()
					.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
		}
	}

	@Override
	public String getContainingTableExpression() {
		return sideNature == ForeignKeyDescriptor.Nature.KEY
				? foreignKeyDescriptor.getKeyTable()
				: foreignKeyDescriptor.getTargetTable();
	}

	@Override
	public int getJdbcTypeCount() {
		return sideNature == ForeignKeyDescriptor.Nature.KEY
				? foreignKeyDescriptor.getJdbcTypeCount()
				: 0;
	}

	@Override
	public JdbcMapping getJdbcMapping(final int index) {
		return foreignKeyDescriptor.getJdbcMapping( index );
	}

	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		return sideNature == ForeignKeyDescriptor.Nature.KEY
				? foreignKeyDescriptor.getSelectable( columnIndex )
				: null;
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return foreignKeyDescriptor.forEachJdbcType( offset, action );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return foreignKeyDescriptor.disassemble(
				foreignKeyDescriptor.getAssociationKeyFromSide( value, sideNature.inverse(), session ),
				session
		);
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		// the value may come from a database snapshot,
		// in this case it corresponds to the value of
		// the key and can be added to the cache key
		final Object cacheValue =
				value != null && foreignKeyDescriptor.getJavaType().getJavaTypeClass() == value.getClass()
						? value
						: foreignKeyDescriptor.getAssociationKeyFromSide( value, sideNature.inverse(), session );
		foreignKeyDescriptor.addToCacheKey( cacheKey, cacheValue, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return foreignKeyDescriptor.forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> consumer,
			SharedSessionContractImplementor session) {
		return foreignKeyDescriptor.forEachDisassembledJdbcValue(
				foreignKeyDescriptor.disassemble(
						foreignKeyDescriptor.getAssociationKeyFromSide( value, sideNature.inverse(), session ),
						session
				),
				offset,
				x,
				y,
				consumer,
				session
		);
	}
}
