/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.LockMode;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.AssociationKey;
import org.hibernate.metamodel.mapping.AttributeMetadataAccess;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.VirtualModelPart;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.spi.NavigablePath;
import org.hibernate.spi.TreatedNavigablePath;
import org.hibernate.spi.EntityIdentifierNavigablePath;
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
import org.hibernate.sql.results.graph.entity.internal.NotFoundSnapshotResult;
import org.hibernate.sql.results.internal.domain.CircularBiDirectionalFetchImpl;
import org.hibernate.sql.results.internal.domain.CircularFetchImpl;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import static java.util.Objects.requireNonNullElse;

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
	// The nullability of the actual FK column
	private final boolean isNullable;
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
	private final boolean isConstrained;
	private final NotFoundAction notFoundAction;
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
			AttributeMetadataAccess attributeMetadataAccess,
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
			AttributeMetadataAccess attributeMetadataAccess,
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
				adjustFetchTiming( mappedFetchTiming, bootValue ),
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
			this.notFoundAction = ( (ManyToOne) bootValue ).getNotFoundAction();
			if ( manyToOne.isLogicalOneToOne() ) {
				cardinality = Cardinality.LOGICAL_ONE_TO_ONE;
			}
			else {
				cardinality = Cardinality.MANY_TO_ONE;
			}
			if ( referencedPropertyName == null ) {
				String bidirectionalAttributeName = null;
				final PersistentClass entityBinding = manyToOne.getMetadata()
						.getEntityBinding( manyToOne.getReferencedEntityName() );
				if ( cardinality == Cardinality.LOGICAL_ONE_TO_ONE ) {
					// Handle join table cases
					for ( Join join : entityBinding.getJoinClosure() ) {
						if ( join.getPersistentClass().getEntityName().equals( entityBinding.getEntityName() )
								&& join.getPropertySpan() == 1
								&& join.getTable() == manyToOne.getTable()
								&& equal( join.getKey(), manyToOne ) ) {
							//noinspection deprecation
							bidirectionalAttributeName = join.getPropertyIterator().next().getName();
							break;
						}
					}
					// Simple one-to-one mapped by cases
					if ( bidirectionalAttributeName == null ) {
						//noinspection deprecation
						final Iterator<Property> propertyClosureIterator = entityBinding.getPropertyClosureIterator();
						while ( propertyClosureIterator.hasNext() ) {
							final Property property = propertyClosureIterator.next();
							if ( property.getValue() instanceof OneToOne
									&& name.equals( ( (OneToOne) property.getValue() ).getMappedByProperty() )
									&& ( (OneToOne) property.getValue() ).getReferencedEntityName().equals(
									declaringType.getJavaType().getJavaType().getTypeName() ) ) {
								bidirectionalAttributeName = property.getName();
								break;
							}
						}
					}
				}
				else {
					//noinspection deprecation
					final Iterator<Property> propertyClosureIterator = entityBinding.getPropertyClosureIterator();
					while ( propertyClosureIterator.hasNext() ) {
						final Property property = propertyClosureIterator.next();
						final Value value = property.getValue();
						if ( value instanceof Collection
								&& name.equals( ( (Collection) value ).getMappedByProperty() )
								&& ( (Collection) value ).getElement().getType().getName()
								.equals( declaringType.getJavaType().getJavaType().getTypeName() ) ) {
							bidirectionalAttributeName = property.getName();
							break;
						}
					}
				}
				this.bidirectionalAttributeName = bidirectionalAttributeName;
			}
			else {
				this.bidirectionalAttributeName = referencedPropertyName;
			}
			if ( bootValue.isNullable() ) {
				isKeyTableNullable = true;
			}
			else {
				final String targetTableName = MappingModelCreationHelper.getTableIdentifierExpression( manyToOne.getTable(), declaringEntityPersister.getFactory() );
				if ( CollectionPart.Nature.fromNameExact( navigableRole.getParent().getLocalName() ) != null ) {
					final PluralAttributeMapping pluralAttribute = (PluralAttributeMapping) declaringEntityPersister.resolveSubPart(
							navigableRole.getParent().getParent()
					);
					final QueryableCollection persister = (QueryableCollection) pluralAttribute.getCollectionDescriptor();
					isKeyTableNullable = !persister.getTableName().equals( targetTableName );
				}
				else {
					final AbstractEntityPersister persister = (AbstractEntityPersister) declaringEntityPersister;
					final int tableIndex = ArrayHelper.indexOf(
							persister.getTableNames(),
							targetTableName
					);
					isKeyTableNullable = persister.isNullableTable( tableIndex );
				}
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
			notFoundAction = null;
			isKeyTableNullable = isNullable();
			isOptional = ! bootValue.isConstrained();
		}
		isConstrained = bootValue.isConstrained();

		this.navigableRole = navigableRole;
		this.declaringTableGroupProducer = resolveDeclaringTableGroupProducer( declaringEntityPersister );
		if ( referencedPropertyName == null ) {
			final Set<String> targetKeyPropertyNames = new HashSet<>( 2 );
			targetKeyPropertyNames.add( EntityIdentifierMapping.ROLE_LOCAL_NAME );
			final PersistentClass entityBinding = bootValue.getBuildingContext().getMetadataCollector()
					.getEntityBinding( entityMappingType.getEntityName() );
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
					this.targetKeyPropertyName = compositeType.getPropertyNames()[0];
					addPrefixedPropertyNames(
							targetKeyPropertyNames,
							targetKeyPropertyName,
							compositeType.getSubtypes()[0],
							declaringEntityPersister.getFactory()
					);
				}
				else {
					this.targetKeyPropertyName = EntityIdentifierMapping.ROLE_LOCAL_NAME;
					addPrefixedPropertyNames(
							targetKeyPropertyNames,
							null,
							propertyType,
							declaringEntityPersister.getFactory()
					);
					addPrefixedPropertyNames(
							targetKeyPropertyNames,
							targetKeyPropertyName,
							propertyType,
							declaringEntityPersister.getFactory()
					);
				}
			}
			else {
				this.targetKeyPropertyName = entityBinding.getIdentifierProperty().getName();
				addPrefixedPropertyNames(
						targetKeyPropertyNames,
						targetKeyPropertyName,
						propertyType,
						declaringEntityPersister.getFactory()
				);
			}
			this.targetKeyPropertyNames = targetKeyPropertyNames;
		}
		else if ( bootValue.isReferenceToPrimaryKey() ) {
			this.targetKeyPropertyName = referencedPropertyName;
			final Set<String> targetKeyPropertyNames = new HashSet<>( 2 );
			addPrefixedPropertyNames(
					targetKeyPropertyNames,
					targetKeyPropertyName,
					bootValue.getType(),
					declaringEntityPersister.getFactory()
			);
			this.targetKeyPropertyNames = targetKeyPropertyNames;
		}
		else {
			final PersistentClass entityBinding = bootValue.getBuildingContext().getMetadataCollector()
					.getEntityBinding( entityMappingType.getEntityName() );
			final Type propertyType = entityBinding.getRecursiveProperty( referencedPropertyName ).getType();
			final CompositeType compositeType;
			if ( propertyType.isComponentType() && ( compositeType = (CompositeType) propertyType ).isEmbedded()
					&& compositeType.getPropertyNames().length == 1 ) {
				final Set<String> targetKeyPropertyNames = new HashSet<>( 2 );
				this.targetKeyPropertyName = compositeType.getPropertyNames()[0];
				addPrefixedPropertyNames(
						targetKeyPropertyNames,
						targetKeyPropertyName,
						compositeType.getSubtypes()[0],
						declaringEntityPersister.getFactory()
				);
				this.targetKeyPropertyNames = targetKeyPropertyNames;
			}
			else {
				this.targetKeyPropertyName = referencedPropertyName;
				final String mapsIdAttributeName;
				// If there is a "virtual property" for a non-PK join mapping, we try to see if the columns match the
				// primary key columns and if so, we add the primary key property name as target key property
				if ( ( mapsIdAttributeName = findMapsIdPropertyName( entityMappingType, referencedPropertyName ) ) != null ) {
					final Set<String> targetKeyPropertyNames = new HashSet<>( 2 );
					targetKeyPropertyNames.add( targetKeyPropertyName );
					addPrefixedPropertyNames(
							targetKeyPropertyNames,
							mapsIdAttributeName,
							entityMappingType.getEntityPersister().getIdentifierType(),
							declaringEntityPersister.getFactory()
					);
					this.targetKeyPropertyNames = targetKeyPropertyNames;
				}
				else {
					this.targetKeyPropertyNames = Collections.singleton( targetKeyPropertyName );
				}
			}
		}
	}

	private static FetchTiming adjustFetchTiming(FetchTiming mappedFetchTiming, ToOne bootValue) {
		if ( bootValue instanceof ManyToOne ) {
			if ( ( (ManyToOne) bootValue ).getNotFoundAction() != null ) {
				return FetchTiming.IMMEDIATE;
			}
		}
		return mappedFetchTiming;
	}

	private TableGroupProducer resolveDeclaringTableGroupProducer(EntityPersister declaringEntityPersister) {
		// Also handle cases where a collection contains an embeddable, that contains an association
		NavigableRole parentRole = getNavigableRole().getParent();
		String collectionRole = null;
		do {
			final CollectionPart.Nature nature = CollectionPart.Nature.fromNameExact(
					parentRole.getLocalName()
			);
			if (nature != null) {
				collectionRole = parentRole.getParent().getFullPath();
				break;
			}
			parentRole = parentRole.getParent();
		} while (parentRole != null);

		if ( collectionRole != null ) {
			// This is a collection part i.e. to-many association
			return declaringEntityPersister.getFactory().getMappingMetamodel()
					.findCollectionDescriptor( collectionRole )
					.getAttributeMapping();
		}
		// This is a simple to-one association
		return declaringEntityPersister;
	}

	private ToOneAttributeMapping(
			ToOneAttributeMapping original,
			ManagedMappingType declaringType,
			TableGroupProducer declaringTableGroupProducer) {
		super(
				original.getAttributeName(),
				original.getStateArrayPosition(),
				original.getAttributeMetadataAccess(),
				original,
				declaringType,
				original.getPropertyAccess(),
				original.getValueGeneration()
		);
		this.navigableRole = original.navigableRole;
		this.sqlAliasStem = original.sqlAliasStem;
		this.isNullable = original.isNullable;
		this.isKeyTableNullable = original.isKeyTableNullable;
		this.isOptional = original.isOptional;
		this.notFoundAction = original.notFoundAction;
		this.unwrapProxy = original.unwrapProxy;
		this.entityMappingType = original.entityMappingType;
		this.referencedPropertyName = original.referencedPropertyName;
		this.targetKeyPropertyName = original.targetKeyPropertyName;
		this.targetKeyPropertyNames = original.targetKeyPropertyNames;
		this.cardinality = original.cardinality;
		this.bidirectionalAttributeName = original.bidirectionalAttributeName;
		this.declaringTableGroupProducer = declaringTableGroupProducer;
		this.isConstrained = original.isConstrained;
	}

	private static boolean equal(Value lhsValue, Value rhsValue) {
		//noinspection deprecation
		Iterator<Selectable> lhsColumns = lhsValue.getColumnIterator();
		//noinspection deprecation
		Iterator<Selectable> rhsColumns = rhsValue.getColumnIterator();
		boolean hasNext;
		do {
			final Selectable lhs = lhsColumns.next();
			final Selectable rhs = rhsColumns.next();
			if ( !lhs.getText().equals( rhs.getText() ) ) {
				return false;
			}

			hasNext = lhsColumns.hasNext();
			if ( hasNext != rhsColumns.hasNext() ) {
				return false;
			}
		} while ( hasNext );
		return true;
	}

	static String findMapsIdPropertyName(EntityMappingType entityMappingType, String referencedPropertyName) {
		final AbstractEntityPersister persister = (AbstractEntityPersister) entityMappingType.getEntityPersister();
		if ( Arrays.equals( persister.getKeyColumnNames(), persister.getPropertyColumnNames( referencedPropertyName ) ) ) {
			return persister.getIdentifierPropertyName();
		}
		return null;
	}

	static void addPrefixedPropertyNames(
			Set<String> targetKeyPropertyNames,
			String prefix,
			Type type,
			SessionFactoryImplementor factory) {
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
				addPrefixedPropertyNames( targetKeyPropertyNames, newPrefix, componentTypeSubtypes[i], factory );
			}
		}
		else if ( type.isEntityType() ) {
			final EntityType entityType = (EntityType) type;
			final Type identifierOrUniqueKeyType = entityType.getIdentifierOrUniqueKeyType( factory );
			final String propertyName;
			if ( entityType.isReferenceToPrimaryKey() ) {
				propertyName = entityType.getAssociatedEntityPersister( factory ).getIdentifierPropertyName();
			}
			else if ( identifierOrUniqueKeyType instanceof EmbeddedComponentType ) {
				propertyName = null;
			}
			else {
				propertyName = entityType.getRHSUniqueKeyPropertyName();
			}
			final String newPrefix;
			if ( prefix == null ) {
				newPrefix = propertyName;
			}
			else if ( propertyName == null ) {
				newPrefix = prefix;
			}
			else {
				newPrefix = prefix + "." + propertyName;
			}
			addPrefixedPropertyNames( targetKeyPropertyNames, newPrefix, identifierOrUniqueKeyType, factory );
		}
	}

	public ToOneAttributeMapping copy(ManagedMappingType declaringType, TableGroupProducer declaringTableGroupProducer) {
		return new ToOneAttributeMapping( this, declaringType, declaringTableGroupProducer );
	}

	@Override
	public void setForeignKeyDescriptor(ForeignKeyDescriptor foreignKeyDescriptor) {
		assert identifyingColumnsTableExpression != null;
		this.foreignKeyDescriptor = foreignKeyDescriptor;
		if ( cardinality == Cardinality.ONE_TO_ONE && bidirectionalAttributeName != null ) {
			this.sideNature = ForeignKeyDescriptor.Nature.TARGET;
		}
		else {
			this.sideNature = foreignKeyDescriptor.getAssociationKey().getTable().equals(
					identifyingColumnsTableExpression )
					? ForeignKeyDescriptor.Nature.KEY
					: ForeignKeyDescriptor.Nature.TARGET;
		}

		// We can only use the parent table group if
		// 		* the FK is located there
		// 		* the association does not force a join (`@NotFound`, nullable 1-1, ...)
		// Otherwise we need to join to the associated entity table(s)
		final boolean forceJoin = hasNotFoundAction()
				|| ( cardinality == Cardinality.ONE_TO_ONE && isNullable() );
		this.canUseParentTableGroup = ! forceJoin
				&& sideNature == ForeignKeyDescriptor.Nature.KEY
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

	public String getReferencedPropertyName() {
		return referencedPropertyName;
	}

	public String getTargetKeyPropertyName() {
		return targetKeyPropertyName;
	}

	public Set<String> getTargetKeyPropertyNames() {
		return targetKeyPropertyNames;
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
		if ( canUseParentTableGroup && targetKeyPropertyNames.contains( name ) ) {
			final ModelPart fkPart;
			if ( sideNature == ForeignKeyDescriptor.Nature.KEY ) {
				fkPart = foreignKeyDescriptor.getKeyPart();
			}
			else {
				fkPart = foreignKeyDescriptor.getTargetPart();
			}
			if ( fkPart instanceof EmbeddableValuedModelPart && fkPart instanceof VirtualModelPart ) {
				return ( (ModelPartContainer) fkPart ).findSubPart( name, targetType );
			}
			return fkPart;
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
			if ( parentNavigablePath.getUnaliasedLocalName().equals( ForeignKeyDescriptor.PART_NAME )
					|| parentNavigablePath.getUnaliasedLocalName().equals( ForeignKeyDescriptor.TARGET_PART_NAME ) ) {
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
			final TableGroup parentTableGroup = creationState
					.getSqlAstCreationState()
					.getFromClauseAccess()
					.getTableGroup( fetchParent.getNavigablePath() );
			final DomainResult<?> foreignKeyDomainResult;
			assert !creationState.isResolvingCircularFetch();
			try {
				creationState.setResolvingCircularFetch( true );
				foreignKeyDomainResult = foreignKeyDescriptor.createDomainResult(
						fetchablePath,
						parentTableGroup,
						sideNature,
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
				// The parent must be non-null. If it is null, the root is a CollectionResult
				return parentNavigablePath.getParent() != null
						&& ( (PluralAttributeMapping) parentModelPart ).isBidirectionalAttributeName( fetchablePath, this );
			}
			else if ( parentModelPart instanceof EntityCollectionPart ) {
				NavigablePath parentOfParent = parentNavigablePath.getParent();
				if ( parentOfParent instanceof EntityIdentifierNavigablePath ) {
					parentOfParent = parentOfParent.getParent();
				}
				// The parent must be non-null. If it is null, the root is a CollectionResult
				return parentOfParent.getParent() != null && ( (PluralAttributeMapping) creationState.resolveModelPart( parentOfParent ) )
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
			final NavigablePath grandparentNavigablePath = parentNavigablePath.getParent();
			if ( parentNavigablePath.getUnaliasedLocalName().equals( CollectionPart.Nature.ELEMENT.getName() )
					&& grandparentNavigablePath != null
					&& grandparentNavigablePath.getUnaliasedLocalName().equals( bidirectionalAttributeName ) ) {
				final NavigablePath parentPath = grandparentNavigablePath.getParent();
				// This can be null for a collection loader
				if ( parentPath == null ) {
					return grandparentNavigablePath.getFullPath().equals(
							entityMappingType.findSubPart( bidirectionalAttributeName ).getNavigableRole().getFullPath()
					);
				}
				else {
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
		else if ( CollectionPart.Nature.fromNameExact( parentNavigablePath.getUnaliasedLocalName() ) != null ) {
			referencedNavigablePath = parentNavigablePath.getParent().getParent();
			hasBidirectionalFetchParent = fetchParent instanceof Fetch
				&& ( (Fetch) fetchParent ).getFetchParent() instanceof Fetch;
		}
		else {
			referencedNavigablePath = parentNavigablePath.getParent();
			hasBidirectionalFetchParent = fetchParent instanceof Fetch;
		}
		// The referencedNavigablePath can be null if this is a collection initialization
		if ( referencedNavigablePath != null ) {
			// If this is the key side, we must ensure that the key is not null, so we create a domain result for it
			// In the CircularBiDirectionalFetchImpl we return null if the key is null instead of the bidirectional value
			final DomainResult<?> keyDomainResult;
			// For now, we don't do this if the key table is nullable to avoid an additional join
			if ( sideNature == ForeignKeyDescriptor.Nature.KEY && !isKeyTableNullable ) {
				keyDomainResult = foreignKeyDescriptor.createKeyDomainResult(
						fetchablePath,
						creationState.getSqlAstCreationState()
								.getFromClauseAccess()
								.findTableGroup( realFetchParent.getNavigablePath() ),
						creationState
				);
			}
			else {
				keyDomainResult = null;
			}

			if ( hasBidirectionalFetchParent ) {
				return new CircularBiDirectionalFetchImpl(
						FetchTiming.IMMEDIATE,
						fetchablePath,
						fetchParent,
						this,
						LockMode.READ,
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
				final FromClauseAccess fromClauseAccess = creationState.getSqlAstCreationState().getFromClauseAccess();
				final TableGroup tableGroup = fromClauseAccess.getTableGroup( referencedNavigablePath );
				fromClauseAccess.registerTableGroup( fetchablePath, tableGroup );
				return new EntityFetchJoinedImpl(
						fetchParent,
						this,
						tableGroup,
						keyDomainResult,
						fetchablePath,
						creationState
				);
			}
		}
		else {
			// We get here is this is a lazy collection initialization for which we know the owner is in the PC
			// So we create a delayed fetch, as we are sure to find the entity in the PC
			final FromClauseAccess fromClauseAccess = creationState.getSqlAstCreationState().getFromClauseAccess();
			final NavigablePath realParent;
			if ( CollectionPart.Nature.fromNameExact( parentNavigablePath.getUnaliasedLocalName() ) != null ) {
				realParent = parentNavigablePath.getParent();
			}
			else {
				realParent = parentNavigablePath;
			}
			final TableGroup tableGroup = fromClauseAccess.getTableGroup( realParent );
			return new EntityDelayedFetchImpl(
					fetchParent,
					this,
					fetchablePath,
					foreignKeyDescriptor.createDomainResult(
							fetchablePath,
							tableGroup,
							sideNature,
							creationState
					),
					isSelectByUniqueKey( sideNature )
			);
		}
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

		final TableGroup parentTableGroup = fromClauseAccess.getTableGroup( fetchParent.getNavigablePath() );

		final NavigablePath parentNavigablePath = fetchablePath.getParent();
		assert parentNavigablePath.equals( fetchParent.getNavigablePath() )
				|| fetchParent.getNavigablePath() instanceof TreatedNavigablePath
				&& parentNavigablePath.equals( fetchParent.getNavigablePath().getRealParent() );


		if ( hasNotFoundAction()
				|| ( fetchTiming == FetchTiming.IMMEDIATE && selected ) ) {
			final TableGroup tableGroup = determineTableGroup(
					fetchablePath,
					fetchParent,
					parentTableGroup,
					resultVariable,
					fromClauseAccess,
					creationState
			);

			return withRegisteredAssociationKeys(
					() -> {
						final DomainResult<?> keyResult;
						if ( notFoundAction != null ) {
							if ( sideNature == ForeignKeyDescriptor.Nature.KEY ) {
								keyResult = foreignKeyDescriptor.createKeyDomainResult(
										fetchablePath,
										parentTableGroup,
										creationState
								);
							}
							else {
								keyResult = foreignKeyDescriptor.createTargetDomainResult(
										fetchablePath,
										parentTableGroup,
										creationState
								);
							}
						}
						else {
							keyResult = null;
						}

						return new EntityFetchJoinedImpl(
								fetchParent,
								this,
								tableGroup,
								keyResult,
								fetchablePath,creationState
						);
					},
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
				keyResult,
				selectByUniqueKey
		);
	}

	private TableGroup determineTableGroup(NavigablePath fetchablePath, FetchParent fetchParent, TableGroup parentTableGroup, String resultVariable, FromClauseAccess fromClauseAccess, DomainResultCreationState creationState) {
		final TableGroup tableGroup;
		if ( fetchParent instanceof EntityResultJoinedSubclassImpl
				&& ( (EntityPersister) fetchParent.getReferencedModePart() ).findDeclaredAttributeMapping( getPartName() ) == null ) {
			final TableGroupJoin tableGroupJoin = createTableGroupJoin(
					fetchablePath,
					parentTableGroup,
					resultVariable,
					getJoinType( fetchablePath, parentTableGroup ),
					true,
					false,
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
								false,
								creationState.getSqlAstCreationState()
						);
						parentTableGroup.addTableGroupJoin( tableGroupJoin );
						return tableGroupJoin.getJoinedGroup();
					}
			);
		}
		return tableGroup;
	}

	private boolean isSelectByUniqueKey(ForeignKeyDescriptor.Nature side) {
		if ( side == ForeignKeyDescriptor.Nature.KEY ) {
			// case 1.2
			return !foreignKeyDescriptor.getNavigableRole()
					.equals( entityMappingType.getIdentifierMapping().getNavigableRole() );
		}
		else {
			// case 1.1
			// Make sure the entity identifier is not a target key property i.e. this really is a unique key mapping
			return bidirectionalAttributeName != null && (
					!( entityMappingType.getIdentifierMapping() instanceof SingleAttributeIdentifierMapping )
							|| !targetKeyPropertyNames.contains(
							( (SingleAttributeIdentifierMapping) entityMappingType.getIdentifierMapping() ).getAttributeName()
					)
			);
		}
	}

	@Override
	public <T> DomainResult<T> createSnapshotDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		// We need a join if either
		//		- the association is mapped with `@NotFound`
		// 		- the key is on the referring side i.e. this is an inverse to-one
		// 			and if the FK refers to a non-PK
		final boolean forceJoin = hasNotFoundAction()
				|| sideNature == ForeignKeyDescriptor.Nature.TARGET
				|| referencedPropertyName != null;
		final TableGroup tableGroupToUse;
		if ( forceJoin ) {
			tableGroupToUse = creationState.getSqlAstCreationState().getFromClauseAccess().resolveTableGroup(
					navigablePath,
					np -> {
						final TableGroupJoin tableGroupJoin = createTableGroupJoin(
								navigablePath,
								tableGroup,
								null,
								getDefaultSqlAstJoinType( tableGroup ),
								true,
								false,
								creationState.getSqlAstCreationState()
						);
						tableGroup.addTableGroupJoin( tableGroupJoin );
						return tableGroupJoin.getJoinedGroup();
					}
			);
		}
		else {
			tableGroupToUse = tableGroup;
		}

		if ( hasNotFoundAction() ) {
			assert tableGroupToUse != tableGroup;
			//noinspection unchecked
			return new NotFoundSnapshotResult(
					navigablePath,
					this,
					tableGroupToUse,
					tableGroup,
					creationState
			);
		}
		if ( referencedPropertyName == null ) {
			//noinspection unchecked
			return new EntityDelayedResultImpl(
					navigablePath.append( EntityIdentifierMapping.ROLE_LOCAL_NAME ),
					this,
					tableGroupToUse,
					creationState
			);
		}
		else {
			// We don't support proxies based on a non-PK yet, so we must fetch the whole entity
			final EntityResultImpl entityResult = new EntityResultImpl(
					navigablePath,
					this,
					tableGroupToUse,
					null,
					creationState
			);
			entityResult.afterInitialize( entityResult, creationState );
			//noinspection unchecked
			return entityResult;
		}
	}

	private EntityFetch withRegisteredAssociationKeys(
			Supplier<EntityFetch> fetchCreator,
			DomainResultCreationState creationState) {
		final boolean added = creationState.registerVisitedAssociationKey( foreignKeyDescriptor.getAssociationKey() );
		AssociationKey additionalAssociationKey = null;
		if ( cardinality == Cardinality.LOGICAL_ONE_TO_ONE && bidirectionalAttributeName != null ) {
			final ModelPart bidirectionalModelPart = entityMappingType.findSubPart( bidirectionalAttributeName );
			// Add the inverse association key side as well to be able to resolve to a CircularFetch
			if ( bidirectionalModelPart instanceof ToOneAttributeMapping ) {
				assert bidirectionalModelPart.getPartMappingType() == declaringTableGroupProducer;
				final ToOneAttributeMapping bidirectionalAttribute = (ToOneAttributeMapping) bidirectionalModelPart;
				final AssociationKey secondKey = bidirectionalAttribute.getForeignKeyDescriptor().getAssociationKey();
				if ( creationState.registerVisitedAssociationKey( secondKey ) ) {
					additionalAssociationKey = secondKey;
				}
			}
		}

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

	@Override
	public SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup) {
		if ( isKeyTableNullable || isNullable ) {
			return SqlAstJoinType.LEFT;
		}
		else if ( parentTableGroup.getModelPart() instanceof CollectionPart ) {
			return SqlAstJoinType.LEFT;
		}
		else {
			if ( parentTableGroup.canUseInnerJoins() ) {
				final Class<?> attributeDeclaringType = declaringTableGroupProducer.getJavaType().getJavaTypeClass();
				final Class<?> parentTableGroupType = parentTableGroup.getModelPart().getJavaType().getJavaTypeClass();

				// This attribute mapping must be declared on the parent table group type or one of its super types
				// If not, this is a fetch for a subtype of the parent table group, which might be left joined
				if ( attributeDeclaringType.isAssignableFrom( parentTableGroupType ) ) {
					return SqlAstJoinType.INNER;
				}
			}
			return SqlAstJoinType.LEFT;
		}
	}

	@Override
	public boolean isSimpleJoinPredicate(Predicate predicate) {
		return foreignKeyDescriptor.isSimpleJoinPredicate( predicate );
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
			SqlAstJoinType requestedJoinType,
			boolean fetched,
			boolean addsPredicate,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext) {
		// Make sure the lhs is never a plural table group directly, but always a table group for a part
		// This is vital for the map key property check that comes next
		assert !( lhs instanceof PluralTableGroup );

		final SqlAstJoinType joinType = requireNonNullElse( requestedJoinType, SqlAstJoinType.INNER );

		// If a parent is a collection part, there is no custom predicate and the join is INNER or LEFT
		// we check if this attribute is the map key property to reuse the existing index table group
		if ( !addsPredicate && ( joinType == SqlAstJoinType.INNER || joinType == SqlAstJoinType.LEFT ) ) {
			TableGroup parentTableGroup = lhs;
			ModelPartContainer parentContainer = lhs.getModelPart();
			StringBuilder embeddablePathSb = null;
			// Traverse up embeddable table groups until we find a table group for a collection part
			while ( !( parentContainer instanceof CollectionPart ) ) {
				if ( parentContainer instanceof EmbeddableValuedModelPart ) {
					if ( embeddablePathSb == null ) {
						embeddablePathSb = new StringBuilder();
					}
					embeddablePathSb.insert( 0, parentContainer.getPartName() + "." );
					parentTableGroup = fromClauseAccess.findTableGroup( parentTableGroup.getNavigablePath().getParent() );
					parentContainer = parentTableGroup.getModelPart();
				}
				else {
					break;
				}
			}
			if ( CollectionPart.Nature.ELEMENT.getName().equals( parentTableGroup.getNavigablePath().getUnaliasedLocalName() ) ) {
				final PluralTableGroup pluralTableGroup = (PluralTableGroup) fromClauseAccess.findTableGroup(
						parentTableGroup.getNavigablePath().getParent()
				);
				final String indexPropertyName = pluralTableGroup.getModelPart()
						.getIndexMetadata()
						.getIndexPropertyName();
				final String pathName;
				if ( embeddablePathSb != null ) {
					pathName = embeddablePathSb.append( getAttributeName() ).toString();
				}
				else {
					pathName = getAttributeName();
				}
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
									(np, tableExpression) -> {
										if ( !canUseParentTableGroup ) {
											return false;
										}
										NavigablePath path = np.getParent();
										// Fast path
										if ( navigablePath.equals( path ) ) {
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
										return navigablePath.equals( path )
												&& targetKeyPropertyNames.contains( sb.toString() )
												&& identifyingColumnsTableExpression.equals( tableExpression );
									}
							),
							null
					);
				}
			}
		}

		final LazyTableGroup lazyTableGroup = createRootTableGroupJoin(
				navigablePath,
				lhs,
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

		final TableReference lhsTableReference = lhs.resolveTableReference( navigablePath, identifyingColumnsTableExpression );

		lazyTableGroup.setTableGroupInitializerCallback( (tableGroup) -> join.applyPredicate(
				foreignKeyDescriptor.generateJoinPredicate(
						sideNature == ForeignKeyDescriptor.Nature.TARGET ? lhsTableReference : tableGroup.getPrimaryTableReference(),
						sideNature == ForeignKeyDescriptor.Nature.TARGET ? tableGroup.getPrimaryTableReference() : lhsTableReference,
						sqlExpressionResolver,
						creationContext
				)
		) );

		if ( hasNotFoundAction() ) {
			getAssociatedEntityMappingType().applyWhereRestrictions(
					join::applyPredicate,
					lazyTableGroup.getTableGroup(),
					true,
					null
			);
		}

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
		final SqlAliasBase sqlAliasBase = aliasBaseGenerator.createSqlAliasBase( sqlAliasStem );

		final boolean canUseInnerJoin;
		if ( ! lhs.canUseInnerJoins() ) {
			canUseInnerJoin = false;
		}
		else if ( isNullable || hasNotFoundAction() ) {
			canUseInnerJoin = false;
		}
		else {
			canUseInnerJoin = requestedJoinType == SqlAstJoinType.INNER;
		}

		TableGroup realParentTableGroup = lhs;
		while ( realParentTableGroup.getModelPart() instanceof EmbeddableValuedModelPart ) {
			realParentTableGroup = fromClauseAccess.findTableGroup( realParentTableGroup.getNavigablePath().getParent() );
		}
		final TableGroupProducer tableGroupProducer;
		if ( realParentTableGroup instanceof CorrelatedTableGroup ) {
			// If the parent is a correlated table group, we can't refer to columns of the table in the outer query,
			// because the context in which a column is used could be an aggregate function.
			// Using a parent column in such a case would lead to an error if the parent query lacks a proper group by
			tableGroupProducer = entityMappingType;
		}
		else {
			tableGroupProducer = this;
		}
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
					if ( !canUseParentTableGroup || tableGroupProducer != ToOneAttributeMapping.this ) {
						return false;
					}
					NavigablePath path = np.getParent();
					// Fast path
					if ( navigablePath.equals( path ) ) {
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
					return navigablePath.equals( path )
							&& targetKeyPropertyNames.contains( sb.toString() )
							&& identifyingColumnsTableExpression.equals( tableExpression );
				},
				tableGroupProducer,
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
									sqlExpressionResolver,
									creationContext
							)
					)
			);
		}

		if ( realParentTableGroup instanceof CorrelatedTableGroup ) {
			// Force initialization of the underlying table group join to retain cardinality
			lazyTableGroup.getPrimaryTableReference();
		}
		else {
			initializeIfNeeded( lhs, requestedJoinType, lazyTableGroup );
		}

		return lazyTableGroup;
	}

	private void initializeIfNeeded(TableGroup lhs, SqlAstJoinType sqlAstJoinType, TableGroup tableGroup) {
		if ( sqlAstJoinType == SqlAstJoinType.INNER && ( isNullable || !lhs.canUseInnerJoins() ) ) {
			// Force initialization of the underlying table group join to retain cardinality
			tableGroup.getPrimaryTableReference();
		}
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
	public void breakDownJdbcValues(
			Object domainValue,
			JdbcValueConsumer valueConsumer,
			SharedSessionContractImplementor session) {
		foreignKeyDescriptor.breakDownJdbcValues(
				foreignKeyDescriptor.getAssociationKeyFromSide( domainValue, sideNature.inverse(), session ),
				valueConsumer,
				session
		);
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
		return foreignKeyDescriptor.disassemble(
				foreignKeyDescriptor.getAssociationKeyFromSide( value, sideNature.inverse(), session ),
				session
		);
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return foreignKeyDescriptor.forEachDisassembledJdbcValue( value, clause, offset, valuesConsumer, session );
	}

	@Override
	public int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer consumer,
			SharedSessionContractImplementor session) {
		return foreignKeyDescriptor.forEachDisassembledJdbcValue(
				foreignKeyDescriptor.disassemble(
						foreignKeyDescriptor.getAssociationKeyFromSide( value, sideNature.inverse(), session ),
						session
				),
				clause,
				offset,
				consumer,
				session
		);
	}
}
