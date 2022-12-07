/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.collection.internal.StandardArraySemantics;
import org.hibernate.collection.internal.StandardBagSemantics;
import org.hibernate.collection.internal.StandardIdentifierBagSemantics;
import org.hibernate.collection.internal.StandardListSemantics;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Resolvable;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SortableValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.AttributeMetadataAccess;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.CollectionIdentifierDescriptor;
import org.hibernate.metamodel.mapping.CollectionMappingType;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.PropertyBasedMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.VirtualModelPart;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.collection.SQLLoadableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.property.access.internal.ChainedPropertyAccessImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.ast.spi.SqlAliasStemHelper;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.tuple.ValueGeneration;
import org.hibernate.type.AssociationType;
import org.hibernate.type.BasicType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.metamodel.mapping.MappingModelCreationLogger.LOGGER;

/**
 * @author Steve Ebersole
 */
public class MappingModelCreationHelper {
	/**
	 * A factory - disallow direct instantiation
	 */
	private MappingModelCreationHelper() {
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static EntityIdentifierMapping buildEncapsulatedCompositeIdentifierMapping(
			EntityPersister entityPersister,
			Property bootProperty,
			String attributeName,
			String rootTableName,
			String[] rootTableKeyColumnNames,
			CompositeType cidType,
			MappingModelCreationProcess creationProcess) {
		final PersistentClass bootEntityDescriptor = creationProcess.getCreationContext()
				.getBootModel()
				.getEntityBinding( entityPersister.getEntityName() );

		final PropertyAccess propertyAccess = entityPersister.getRepresentationStrategy()
				.resolvePropertyAccess( bootEntityDescriptor.getIdentifierProperty() );

		final EmbeddableMappingTypeImpl embeddableMappingType = EmbeddableMappingTypeImpl.from(
				(Component) bootProperty.getValue(),
				cidType,
				rootTableName,
				rootTableKeyColumnNames,
				embeddable -> new EmbeddedIdentifierMappingImpl(
						entityPersister,
						attributeName,
						embeddable,
						propertyAccess,
						rootTableName,
						creationProcess
				),
				creationProcess
		);


		return (EmbeddedIdentifierMappingImpl) embeddableMappingType.getEmbeddedValueMapping();
	}

	public static CompositeIdentifierMapping buildNonEncapsulatedCompositeIdentifierMapping(
			EntityPersister entityPersister,
			String rootTableName,
			String[] rootTableKeyColumnNames,
			PersistentClass bootEntityDescriptor,
			MappingModelCreationProcess creationProcess) {
		return new NonAggregatedIdentifierMappingImpl(
				entityPersister,
				bootEntityDescriptor.getRootClass(),
				rootTableName,
				rootTableKeyColumnNames,
				creationProcess
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Non-identifier attributes

	@SuppressWarnings("rawtypes")
	public static BasicAttributeMapping buildBasicAttributeMapping(
			String attrName,
			NavigableRole navigableRole,
			int stateArrayPosition,
			Property bootProperty,
			ManagedMappingType declaringType,
			BasicType attrType,
			String tableExpression,
			String attrColumnName,
			boolean isAttrFormula,
			String readExpr,
			String writeExpr,
			String columnDefinition,
			Long length,
			Integer precision,
			Integer scale,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess) {
		final Value value = bootProperty.getValue();
		final BasicValue.Resolution<?> resolution = ( (Resolvable) value ).resolve();

		final AttributeMetadataAccess attributeMetadataAccess = entityMappingType -> new AttributeMetadata() {
			private final MutabilityPlan mutabilityPlan = resolution.getMutabilityPlan();
			private final boolean nullable = value.isNullable();
			private final boolean insertable = bootProperty.isInsertable();
			private final boolean updateable = bootProperty.isUpdateable();
			private final boolean includeInOptimisticLocking = bootProperty.isOptimisticLocked();

			@Override
			public PropertyAccess getPropertyAccess() {
				return propertyAccess;
			}

			@Override
			public MutabilityPlan getMutabilityPlan() {
				return mutabilityPlan;
			}

			@Override
			public boolean isNullable() {
				return nullable;
			}

			@Override
			public boolean isInsertable() {
				return insertable;
			}

			@Override
			public boolean isUpdatable() {
				return updateable;
			}

			@Override
			public boolean isIncludedInDirtyChecking() {
				// todo (6.0) : do not believe this is correct
				return updateable;
			}

			@Override
			public boolean isIncludedInOptimisticLocking() {
				return includeInOptimisticLocking;
			}

			@Override
			public CascadeStyle getCascadeStyle() {
				return cascadeStyle;
			}
		};

		final FetchTiming fetchTiming;
		final FetchStyle fetchStyle;
		if ( declaringType instanceof EmbeddableMappingType ) {
			if ( bootProperty.isLazy() ) {
				LOGGER.debugf(
						"Attribute was declared lazy, but is part of an embeddable - `%s#%s` - LAZY will be ignored",
						declaringType.getNavigableRole().getFullPath(),
						bootProperty.getName()
				);
			}
			fetchTiming = FetchTiming.IMMEDIATE;
			fetchStyle = FetchStyle.JOIN;
		}
		else {
			fetchTiming = bootProperty.isLazy() ? FetchTiming.DELAYED : FetchTiming.IMMEDIATE;
			fetchStyle = bootProperty.isLazy() ? FetchStyle.SELECT : FetchStyle.JOIN;
		}
		final ValueGeneration valueGeneration = bootProperty.getValueGenerationStrategy();

		return new BasicAttributeMapping(
				attrName,
				navigableRole,
				stateArrayPosition,
				attributeMetadataAccess,
				fetchTiming,
				fetchStyle,
				tableExpression,
				attrColumnName,
				isAttrFormula,
				readExpr,
				writeExpr,
				columnDefinition,
				length,
				precision,
				scale,
				attrType,
				declaringType,
				propertyAccess,
				valueGeneration
		);
	}


	public static EmbeddedAttributeMapping buildEmbeddedAttributeMapping(
			String attrName,
			int stateArrayPosition,
			Property bootProperty,
			ManagedMappingType declaringType,
			CompositeType attrType,
			String tableExpression,
			String[] rootTableKeyColumnNames,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess) {
		final AttributeMetadataAccess attributeMetadataAccess = getAttributeMetadataAccess(
				bootProperty,
				attrType,
				propertyAccess,
				cascadeStyle,
				creationProcess
		);

		final Component component = (Component) bootProperty.getValue();
		final EmbeddableMappingTypeImpl embeddableMappingType = EmbeddableMappingTypeImpl.from(
				component,
				attrType,
				tableExpression,
				rootTableKeyColumnNames,
				attributeMappingType -> {
					if ( component.isEmbedded() ) {
						return new VirtualEmbeddedAttributeMapping(
								attrName,
								declaringType.getNavigableRole().append( attrName ),
								stateArrayPosition,
								tableExpression,
								attributeMetadataAccess,
								component.getParentProperty(),
								FetchTiming.IMMEDIATE,
								FetchStyle.JOIN,
								attributeMappingType,
								declaringType,
								propertyAccess,
								bootProperty.getValueGenerationStrategy()
						);
					}
					else {
						return new EmbeddedAttributeMapping(
								attrName,
								declaringType.getNavigableRole().append( attrName ),
								stateArrayPosition,
								tableExpression,
								attributeMetadataAccess,
								component.getParentProperty(),
								FetchTiming.IMMEDIATE,
								FetchStyle.JOIN,
								attributeMappingType,
								declaringType,
								propertyAccess,
								bootProperty.getValueGenerationStrategy()
						);
					}
				},
				creationProcess
		);

		return (EmbeddedAttributeMapping) embeddableMappingType.getEmbeddedValueMapping();
	}

	@SuppressWarnings("rawtypes")
	protected static AttributeMetadataAccess getAttributeMetadataAccess(
			Property bootProperty,
			Type attrType,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess) {
		return entityMappingType -> new AttributeMetadata() {
			private final boolean nullable = bootProperty.getValue().isNullable();
			private final boolean insertable = bootProperty.isInsertable();
			private final boolean updateable = bootProperty.isUpdateable();
			private final boolean includeInOptimisticLocking = bootProperty.isOptimisticLocked();

			private final MutabilityPlan mutabilityPlan;

			{
				if ( updateable ) {
					mutabilityPlan = new MutabilityPlan() {
						@Override
						public boolean isMutable() {
							return true;
						}

						@Override
						public Object deepCopy(Object value) {
							if ( value == null ) {
								return null;
							}

							return attrType.deepCopy( value, creationProcess.getCreationContext().getSessionFactory() );
						}

						@Override
						public Object disassemble(Object value, SharedSessionContract session) {
							throw new NotYetImplementedFor6Exception( getClass() );
						}

						@Override
						public Object assemble(Object cached, SharedSessionContract session) {
							throw new NotYetImplementedFor6Exception( getClass() );
						}
					};
				}
				else {
					mutabilityPlan = ImmutableMutabilityPlan.INSTANCE;
				}
			}

			@Override
			public PropertyAccess getPropertyAccess() {
				return propertyAccess;
			}

			@Override
			public MutabilityPlan getMutabilityPlan() {
				return mutabilityPlan;
			}

			@Override
			public boolean isNullable() {
				return nullable;
			}

			@Override
			public boolean isInsertable() {
				return insertable;
			}

			@Override
			public boolean isUpdatable() {
				return updateable;
			}

			@Override
			public boolean isIncludedInDirtyChecking() {
				// todo (6.0) : do not believe this is correct
				return updateable;
			}

			@Override
			public boolean isIncludedInOptimisticLocking() {
				return includeInOptimisticLocking;
			}

			@Override
			public CascadeStyle getCascadeStyle() {
				return cascadeStyle;
			}
		};
	}

	@SuppressWarnings("rawtypes")
	public static AttributeMetadataAccess getAttributeMetadataAccess(PropertyAccess propertyAccess) {
		return new AttributeMetadataAccess() {
			final AttributeMetadata attributeMetadata = new AttributeMetadata() {
				private final MutabilityPlan mutabilityPlan = ImmutableMutabilityPlan.INSTANCE;

				@Override
				public PropertyAccess getPropertyAccess() {
					return propertyAccess;
				}

				@Override
				public MutabilityPlan getMutabilityPlan() {
					return mutabilityPlan;
				}

				@Override
				public boolean isNullable() {
					return false;
				}

				@Override
				public boolean isInsertable() {
					return true;
				}

				@Override
				public boolean isUpdatable() {
					return false;
				}

				@Override
				public boolean isIncludedInDirtyChecking() {

					return false;
				}

				@Override
				public boolean isIncludedInOptimisticLocking() {
					// todo (6.0) : do not sure this is correct
					return true;
				}

				@Override
				public CascadeStyle getCascadeStyle() {
					// todo (6.0) : do not sure this is correct
					return null;
				}
			};

			@Override
			public AttributeMetadata resolveAttributeMetadata(EntityMappingType entityMappingType) {
				return attributeMetadata;
			}
		};
	}

	@SuppressWarnings("rawtypes")
	public static PluralAttributeMapping buildPluralAttributeMapping(
			String attrName,
			int stateArrayPosition,
			Property bootProperty,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			FetchMode fetchMode,
			MappingModelCreationProcess creationProcess) {

		final Collection bootValueMapping = (Collection) bootProperty.getValue();

		final RuntimeModelCreationContext creationContext = creationProcess.getCreationContext();
		final SessionFactoryImplementor sessionFactory = creationContext.getSessionFactory();
		final SqlStringGenerationContext sqlStringGenerationContext = sessionFactory.getSqlStringGenerationContext();
		final Dialect dialect = sqlStringGenerationContext.getDialect();
		final MappingMetamodel domainModel = creationContext.getDomainModel();

		final CollectionPersister collectionDescriptor = domainModel.findCollectionDescriptor( bootValueMapping.getRole() );
		assert collectionDescriptor != null;

		final String tableExpression = ( (Joinable) collectionDescriptor ).getTableName();

		final String sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( bootProperty.getName() );

		final CollectionMappingType<?> collectionMappingType;
		final JavaTypeRegistry jtdRegistry = creationContext.getJavaTypeRegistry();

		final CollectionPart elementDescriptor = interpretElement(
				bootValueMapping,
				tableExpression,
				collectionDescriptor,
				sqlAliasStem,
				dialect,
				creationProcess
		);

		final CollectionPart indexDescriptor;
		CollectionIdentifierDescriptor identifierDescriptor = null;

		final CollectionSemantics<?,?> collectionSemantics = collectionDescriptor.getCollectionSemantics();
		switch ( collectionSemantics.getCollectionClassification() ) {
			case ARRAY: {
				collectionMappingType = new CollectionMappingTypeImpl(
						jtdRegistry.getDescriptor( Object[].class ),
						StandardArraySemantics.INSTANCE
				);

				final BasicValue index = (BasicValue) ( (IndexedCollection) bootValueMapping ).getIndex();
				final SelectableMapping selectableMapping = SelectableMappingImpl.from(
						tableExpression,
						index.getSelectables().get(0),
						creationContext.getTypeConfiguration().getBasicTypeForJavaType( Integer.class ),
						creationProcess.getCreationContext().getTypeConfiguration(),
						dialect,
						creationProcess.getSqmFunctionRegistry()
				);
				indexDescriptor = new BasicValuedCollectionPart(
						collectionDescriptor,
						CollectionPart.Nature.INDEX,
						selectableMapping
				);

				break;
			}
			case BAG: {
				collectionMappingType = new CollectionMappingTypeImpl(
						jtdRegistry.getDescriptor( java.util.Collection.class ),
						StandardBagSemantics.INSTANCE
				);

				indexDescriptor = null;

				break;
			}
			case ID_BAG: {
				collectionMappingType = new CollectionMappingTypeImpl(
						jtdRegistry.getDescriptor( java.util.Collection.class ),
						StandardIdentifierBagSemantics.INSTANCE
				);

				indexDescriptor = null;

				assert collectionDescriptor instanceof SQLLoadableCollection;
				final SQLLoadableCollection loadableCollection = (SQLLoadableCollection) collectionDescriptor;
				final String identifierColumnName = loadableCollection.getIdentifierColumnName();
				assert identifierColumnName != null;

				identifierDescriptor = new CollectionIdentifierDescriptorImpl(
						collectionDescriptor,
						tableExpression,
						identifierColumnName,
						(BasicType) loadableCollection.getIdentifierType()
				);

				break;
			}
			case LIST: {
				final BasicValue index = (BasicValue) ( (IndexedCollection) bootValueMapping ).getIndex();
				final SelectableMapping selectableMapping = SelectableMappingImpl.from(
						tableExpression,
						index.getSelectables().get(0),
						creationContext.getTypeConfiguration().getBasicTypeForJavaType( Integer.class ),
						creationProcess.getCreationContext().getTypeConfiguration(),
						dialect,
						creationProcess.getSqmFunctionRegistry()
				);
				indexDescriptor = new BasicValuedCollectionPart(
						collectionDescriptor,
						CollectionPart.Nature.INDEX,
						selectableMapping
				);

				collectionMappingType = new CollectionMappingTypeImpl(
						jtdRegistry.getDescriptor( List.class ),
						StandardListSemantics.INSTANCE
				);

				break;
			}
			case MAP:
			case ORDERED_MAP:
			case SORTED_MAP: {
				final Class<? extends java.util.Map> mapJavaType = collectionSemantics.getCollectionClassification() == CollectionClassification.SORTED_MAP
						? SortedMap.class
						: java.util.Map.class;
				collectionMappingType = new CollectionMappingTypeImpl(
						jtdRegistry.getDescriptor( mapJavaType ),
						collectionSemantics
				);
				final String mapKeyTableExpression;
				if ( bootValueMapping instanceof Map && ( (Map) bootValueMapping ).getMapKeyPropertyName() != null ) {
					mapKeyTableExpression = getTableIdentifierExpression(
							( (Map) bootValueMapping ).getIndex().getTable(),
							creationProcess
					);
				}
				else {
					mapKeyTableExpression = tableExpression;
				}
				indexDescriptor = interpretMapKey(
						bootValueMapping,
						collectionDescriptor,
						mapKeyTableExpression,
						sqlAliasStem,
						dialect,
						creationProcess
				);

				break;
			}
			case SET:
			case ORDERED_SET:
			case SORTED_SET: {
				final Class<? extends java.util.Set> setJavaType = collectionSemantics.getCollectionClassification() == CollectionClassification.SORTED_MAP
						? SortedSet.class
						: java.util.Set.class;
				collectionMappingType = new CollectionMappingTypeImpl(
						jtdRegistry.getDescriptor( setJavaType ),
						collectionSemantics
				);

				indexDescriptor = null;

				break;
			}
			default: {
				throw new MappingException(
						"Unexpected CollectionClassification : " + collectionSemantics.getCollectionClassification()
				);
			}
		}

		final AttributeMetadata attributeMetadata = new AttributeMetadata() {
			@Override
			public PropertyAccess getPropertyAccess() {
				return propertyAccess;
			}

			@Override
			public MutabilityPlan getMutabilityPlan() {
				return ImmutableMutabilityPlan.instance();
			}

			@Override
			public boolean isNullable() {
				return bootProperty.isOptional();
			}

			@Override
			public boolean isInsertable() {
				return bootProperty.isInsertable();
			}

			@Override
			public boolean isUpdatable() {
				return bootProperty.isUpdateable();
			}

			@Override
			public boolean isIncludedInDirtyChecking() {
				return false;
			}

			@Override
			public boolean isIncludedInOptimisticLocking() {
				return bootProperty.isOptimisticLocked();
			}

			@Override
			public CascadeStyle getCascadeStyle() {
				return cascadeStyle;
			}
		};

		final FetchStyle style = FetchOptionsHelper.determineFetchStyleByMetadata(
				fetchMode,
				collectionDescriptor.getCollectionType(),
				sessionFactory
		);

		final FetchTiming timing = FetchOptionsHelper.determineFetchTiming(
				style,
				collectionDescriptor.getCollectionType(),
				collectionDescriptor.isLazy(),
				collectionDescriptor.getRole(),
				sessionFactory
		);

		final PluralAttributeMappingImpl pluralAttributeMapping = new PluralAttributeMappingImpl(
				attrName,
				bootValueMapping,
				propertyAccess,
				entityMappingType -> attributeMetadata,
				collectionMappingType,
				stateArrayPosition,
				elementDescriptor,
				indexDescriptor,
				identifierDescriptor,
				timing,
				style,
				cascadeStyle,
				declaringType,
				collectionDescriptor
		);

		creationProcess.registerInitializationCallback(
				"PluralAttributeMapping(" + bootValueMapping.getRole() + ")#finishInitialization",
				() -> {
					pluralAttributeMapping.finishInitialization( bootProperty, bootValueMapping, creationProcess );
					return true;
				}
		);

		creationProcess.registerInitializationCallback(
				"PluralAttributeMapping(" + bootValueMapping.getRole() + ") - key descriptor",
				() -> {
					interpretPluralAttributeMappingKeyDescriptor(
							pluralAttributeMapping,
							bootValueMapping,
							collectionDescriptor,
							declaringType,
							dialect,
							creationProcess
					);
					return true;
				}
		);

		return pluralAttributeMapping;
	}

	private static void interpretPluralAttributeMappingKeyDescriptor(
			PluralAttributeMappingImpl attributeMapping,
			Collection bootValueMapping,
			CollectionPersister collectionDescriptor,
			ManagedMappingType declaringType,
			Dialect dialect,
			MappingModelCreationProcess creationProcess) {
		ModelPart attributeMappingSubPart = null;
		if ( !StringHelper.isEmpty( collectionDescriptor.getMappedByProperty() ) ) {
			attributeMappingSubPart = ( (ModelPartContainer) attributeMapping.getElementDescriptor().getPartMappingType() )
					.findSubPart( collectionDescriptor.getMappedByProperty(), null );
		}

		if ( attributeMappingSubPart instanceof ToOneAttributeMapping ) {
			final ToOneAttributeMapping referencedAttributeMapping = (ToOneAttributeMapping) attributeMappingSubPart;

			setReferencedAttributeForeignKeyDescriptor(
					attributeMapping,
					referencedAttributeMapping,
					referencedAttributeMapping.findContainingEntityMapping().getEntityPersister(),
					collectionDescriptor.getMappedByProperty(),
					dialect,
					creationProcess
			);
			return;
		}

		final KeyValue bootValueMappingKey = bootValueMapping.getKey();
		final Type keyType = bootValueMappingKey.getType();
		final ModelPart fkTarget;
		final String lhsPropertyName = collectionDescriptor.getCollectionType().getLHSPropertyName();
		final boolean isReferenceToPrimaryKey = lhsPropertyName == null;
		final ManagedMappingType keyDeclaringType;
		if ( collectionDescriptor.getElementType().isEntityType() ) {
			keyDeclaringType = ( (QueryableCollection) collectionDescriptor ).getElementPersister();
		}
		else {
			// This is not "really correct" but it is as good as it gets.
			// The key declaring type serves as declaring type for the inverse model part of a FK.
			// Most of the time, there is a proper managed type, but not for basic collections.
			// Since the declaring type is needed for certain operations, we use the one from the target side of the FK
			keyDeclaringType = declaringType;
		}
		if ( isReferenceToPrimaryKey ) {
			fkTarget = collectionDescriptor.getOwnerEntityPersister().getIdentifierMapping();
		}
		else {
			fkTarget = declaringType.findAttributeMapping( lhsPropertyName );
		}

		if ( keyType instanceof BasicType ) {
			assert bootValueMappingKey.getColumnSpan() == 1;
			assert fkTarget instanceof BasicValuedModelPart;
			final BasicValuedModelPart simpleFkTarget = (BasicValuedModelPart) fkTarget;
			final String tableExpression = getTableIdentifierExpression( bootValueMappingKey.getTable(), creationProcess );
			final SelectableMapping keySelectableMapping = SelectableMappingImpl.from(
					tableExpression,
					bootValueMappingKey.getSelectables().get(0),
					(JdbcMapping) keyType,
					creationProcess.getCreationContext().getTypeConfiguration(),
					dialect,
					creationProcess.getSqmFunctionRegistry()
			);
			attributeMapping.setForeignKeyDescriptor(
					new SimpleForeignKeyDescriptor(
							keyDeclaringType,
							simpleFkTarget,
							null,
							keySelectableMapping,
							simpleFkTarget,
							isReferenceToPrimaryKey,
							( (SimpleValue) bootValueMappingKey ).isConstrained()
					)
			);
		}
		else if ( fkTarget instanceof EmbeddableValuedModelPart ) {
			final EmbeddedForeignKeyDescriptor embeddedForeignKeyDescriptor =
					buildEmbeddableForeignKeyDescriptor(
							(EmbeddableValuedModelPart) fkTarget,
							bootValueMapping,
							keyDeclaringType,
							collectionDescriptor.getAttributeMapping(),
							false,
							dialect,
							creationProcess
					);

			attributeMapping.setForeignKeyDescriptor( embeddedForeignKeyDescriptor );
		}
		else {
			throw new NotYetImplementedFor6Exception(
					"Support for " + fkTarget.getClass() + " foreign keys not yet implemented: " + bootValueMapping.getRole()
			);
		}
	}

	/**
	 * Tries to {@link ToOneAttributeMapping#setForeignKeyDescriptor}
	 * to the given attribute {@code attributeMapping}.
	 *
	 * @param attributeMapping The attribute for which we try to set the foreign key
	 * @param bootProperty The property
	 * @param bootValueMapping The value mapping
	 * @param inversePropertyAccess Access to the inverse property
	 * @param dialect Current dialect
	 * @param creationProcess Current creation process
	 * @return true if the foreign key is actually set
	 */
	public static boolean interpretToOneKeyDescriptor(
			ToOneAttributeMapping attributeMapping,
			Property bootProperty,
			ToOne bootValueMapping,
			PropertyAccess inversePropertyAccess,
			Dialect dialect,
			MappingModelCreationProcess creationProcess) {
		if ( attributeMapping.getForeignKeyDescriptor() != null ) {
			// already built/known
			return true;
		}

		final String tableName = getTableIdentifierExpression( bootValueMapping.getTable(), creationProcess );

		attributeMapping.setIdentifyingColumnsTableExpression( tableName );

		final EntityPersister referencedEntityDescriptor = creationProcess
				.getEntityPersister( bootValueMapping.getReferencedEntityName() );

		String referencedPropertyName;
		boolean swapDirection = false;
		if ( bootValueMapping instanceof OneToOne ) {
			OneToOne oneToOne = (OneToOne) bootValueMapping;
			swapDirection = oneToOne.getForeignKeyType() == ForeignKeyDirection.TO_PARENT;
			referencedPropertyName = oneToOne.getMappedByProperty();
			if ( referencedPropertyName == null ) {
				referencedPropertyName = oneToOne.getReferencedPropertyName();
			}
		}
		else {
			referencedPropertyName = null;
		}

		if ( referencedPropertyName != null  ) {
			if ( referencedPropertyName.indexOf( "." ) > 0 ) {
				return interpretNestedToOneKeyDescriptor(
						referencedEntityDescriptor,
						referencedPropertyName,
						attributeMapping
				);
			}

			final ModelPart modelPart = referencedEntityDescriptor.findByPath( referencedPropertyName );
			if ( modelPart instanceof ToOneAttributeMapping ) {
				setReferencedAttributeForeignKeyDescriptor(
						attributeMapping,
						(ToOneAttributeMapping) modelPart,
						referencedEntityDescriptor,
						referencedPropertyName,
						dialect,
						creationProcess
				);
			}
			else if ( modelPart instanceof EmbeddableValuedModelPart ) {
				final EmbeddedForeignKeyDescriptor embeddedForeignKeyDescriptor = buildEmbeddableForeignKeyDescriptor(
						(EmbeddableValuedModelPart) modelPart,
						bootValueMapping,
						attributeMapping.getDeclaringType(),
						attributeMapping.findContainingEntityMapping(),
						true,
						dialect,
						creationProcess
				);
				attributeMapping.setForeignKeyDescriptor( embeddedForeignKeyDescriptor );
			}
			else if ( modelPart == null ) {
				throw new IllegalArgumentException( "Unable to find attribute " + bootProperty.getPersistentClass()
						.getEntityName() + " -> " + bootProperty.getName() );
			}
			else {
				throw new NotYetImplementedFor6Exception(
						"Support for foreign-keys based on `" + modelPart + "` not yet implemented: " +
								bootProperty.getPersistentClass().getEntityName() + " -> " + bootProperty.getName()
				);
			}
			return true;
		}

		final ModelPart fkTarget;
		if ( bootValueMapping.isReferenceToPrimaryKey() ) {
			fkTarget = referencedEntityDescriptor.getIdentifierMapping();
		}
		else {
			fkTarget = referencedEntityDescriptor.findByPath( bootValueMapping.getReferencedPropertyName() );
		}

		if ( fkTarget instanceof BasicValuedModelPart ) {
			final BasicValuedModelPart simpleFkTarget = (BasicValuedModelPart) fkTarget;
			final Iterator<Selectable> columnIterator = bootValueMapping.getColumnIterator();
			final Table table = bootValueMapping.getTable();
			final String tableExpression = getTableIdentifierExpression( table, creationProcess );
			final BasicValuedModelPart declaringKeyPart;
			final PropertyAccess declaringKeyPropertyAccess;
			if ( inversePropertyAccess == null ) {
				// So far, OneToOne mappings are only supported based on the owner's PK
				if ( bootValueMapping instanceof OneToOne ) {
					declaringKeyPart = simpleFkTarget;
					final EntityIdentifierMapping identifierMapping = attributeMapping.findContainingEntityMapping()
							.getIdentifierMapping();
					declaringKeyPropertyAccess = ( (PropertyBasedMapping) identifierMapping ).getPropertyAccess();
				}
				else {
					declaringKeyPart = simpleFkTarget;
//					declaringKeyPropertyAccess = ( (PropertyBasedMapping) declaringKeyPart ).getPropertyAccess();
					declaringKeyPropertyAccess = new ChainedPropertyAccessImpl(
							attributeMapping.getPropertyAccess(),
							( (PropertyBasedMapping) declaringKeyPart ).getPropertyAccess()
					);
				}
			}
			else {
				declaringKeyPart = simpleFkTarget;
				declaringKeyPropertyAccess = new ChainedPropertyAccessImpl(
						inversePropertyAccess,
						( (PropertyBasedMapping) simpleFkTarget ).getPropertyAccess()
				);
			}
			final SelectableMapping keySelectableMapping;
			if ( columnIterator.hasNext() ) {
				keySelectableMapping = SelectableMappingImpl.from(
						tableExpression,
						columnIterator.next(),
						simpleFkTarget.getJdbcMapping(),
						creationProcess.getCreationContext().getTypeConfiguration(),
						dialect,
						creationProcess.getSqmFunctionRegistry()
				);
			}
			else {
				// case of ToOne with @PrimaryKeyJoinColumn
				keySelectableMapping = SelectableMappingImpl.from(
						tableExpression,
						table.getPrimaryKey().getColumn( 0 ),
						simpleFkTarget.getJdbcMapping(),
						creationProcess.getCreationContext().getTypeConfiguration(),
						dialect,
						creationProcess.getSqmFunctionRegistry()
				);
			}

			final ForeignKeyDescriptor foreignKeyDescriptor = new SimpleForeignKeyDescriptor(
					attributeMapping.getDeclaringType(),
					declaringKeyPart,
					declaringKeyPropertyAccess,
					keySelectableMapping,
					simpleFkTarget,
					bootValueMapping.isReferenceToPrimaryKey(),
					bootValueMapping.isConstrained(),
					swapDirection
			);
			attributeMapping.setForeignKeyDescriptor( foreignKeyDescriptor );
		}
		else if ( fkTarget instanceof EmbeddableValuedModelPart ) {
			final EmbeddedForeignKeyDescriptor embeddedForeignKeyDescriptor = buildEmbeddableForeignKeyDescriptor(
					(EmbeddableValuedModelPart) fkTarget,
					bootValueMapping,
					attributeMapping.getDeclaringType(),
					attributeMapping.findContainingEntityMapping(),
					swapDirection,
					dialect,
					creationProcess
			);
			attributeMapping.setForeignKeyDescriptor( embeddedForeignKeyDescriptor );
		}
		else {
			throw new NotYetImplementedFor6Exception(
					"Support for " + fkTarget.getClass() + " foreign-keys not yet implemented: " +
							bootProperty.getPersistentClass().getEntityName() + " -> " + bootProperty.getName()
			);
		}

		return true;
	}

	/**
	 * Tries to {@link ToOneAttributeMapping#setForeignKeyDescriptor}
	 * to the given attribute {@code attributeMapping},
	 * using the same value from the inverse property defined by the {@code mapped-by}.
	 *
	 * @param referencedEntityDescriptor The entity which contains the inverse property
	 * @param referencedPropertyName The inverse property name path
	 * @param attributeMapping The attribute for which we try to set the foreign key
	 * @return true if the foreign key is actually set
	 */
	private static boolean interpretNestedToOneKeyDescriptor(
			EntityPersister referencedEntityDescriptor,
			String referencedPropertyName,
			ToOneAttributeMapping attributeMapping) {
		String[] propertyPath = StringHelper.split( ".", referencedPropertyName );
		EmbeddableValuedModelPart lastEmbeddableModelPart = null;

		for ( int i = 0; i < propertyPath.length; i++ ) {
			String path = propertyPath[i];
			ModelPart modelPart;

			if ( i == 0 ) {
				modelPart = referencedEntityDescriptor.findSubPart( path );
			}
			else {
				modelPart = lastEmbeddableModelPart.findSubPart( path, null );
			}

			if ( modelPart == null ) {
				return false;
			}
			if ( modelPart instanceof ToOneAttributeMapping ) {
				ToOneAttributeMapping referencedAttributeMapping = (ToOneAttributeMapping) modelPart;
				ForeignKeyDescriptor foreignKeyDescriptor = referencedAttributeMapping.getForeignKeyDescriptor();
				if ( foreignKeyDescriptor == null ) {
					return false;
				}

				attributeMapping.setForeignKeyDescriptor( foreignKeyDescriptor );
				return true;
			}
			if ( modelPart instanceof EmbeddableValuedModelPart ) {
				lastEmbeddableModelPart = (EmbeddableValuedModelPart) modelPart;
			}
			else {
				return false;
			}
		}

		return false;
	}

	public static EmbeddedForeignKeyDescriptor buildEmbeddableForeignKeyDescriptor(
			EmbeddableValuedModelPart embeddableValuedModelPart,
			Value bootValueMapping,
			ManagedMappingType keyDeclaringType,
			TableGroupProducer keyDeclaringTableGroupProducer,
			boolean inverse,
			Dialect dialect,
			MappingModelCreationProcess creationProcess) {
		final boolean hasConstraint;
		final SelectableMappings keySelectableMappings;
		final String keyTableExpression;
		if ( bootValueMapping instanceof Collection ) {
			final Collection collectionBootValueMapping = (Collection) bootValueMapping;
			hasConstraint = ( (SimpleValue) collectionBootValueMapping.getKey() ).isConstrained();
			keyTableExpression = getTableIdentifierExpression(
					collectionBootValueMapping.getCollectionTable(),
					creationProcess
			);
			keySelectableMappings = SelectableMappingsImpl.from(
					keyTableExpression,
					collectionBootValueMapping.getKey(),
					getPropertyOrder( bootValueMapping, creationProcess ),
					creationProcess.getCreationContext().getSessionFactory(),
					creationProcess.getCreationContext().getTypeConfiguration(),
					dialect,
					creationProcess.getSqmFunctionRegistry()
			);
		}
		else {
			if ( bootValueMapping instanceof OneToMany ) {
				// We assume there is a constraint if the mapping is not nullable
				hasConstraint = !bootValueMapping.isNullable();
			}
			else {
				hasConstraint = ( (SimpleValue) bootValueMapping ).isConstrained();
			}
			keyTableExpression = getTableIdentifierExpression(
					bootValueMapping.getTable(),
					creationProcess
			);
			keySelectableMappings = SelectableMappingsImpl.from(
					keyTableExpression,
					bootValueMapping,
					getPropertyOrder( bootValueMapping, creationProcess ),
					creationProcess.getCreationContext().getSessionFactory(),
					creationProcess.getCreationContext().getTypeConfiguration(),
					dialect,
					creationProcess.getSqmFunctionRegistry()
			);
		}
		if ( inverse ) {
			return new EmbeddedForeignKeyDescriptor(
					embeddableValuedModelPart,
					createInverseModelPart(
							embeddableValuedModelPart,
							keyDeclaringType,
							keyDeclaringTableGroupProducer,
							keySelectableMappings,
							creationProcess
					),
					embeddableValuedModelPart.getContainingTableExpression(),
					embeddableValuedModelPart.getEmbeddableTypeDescriptor(),
					keyTableExpression,
					keySelectableMappings,
					hasConstraint,
					creationProcess
			);
		}
		else {
			return new EmbeddedForeignKeyDescriptor(
					createInverseModelPart(
							embeddableValuedModelPart,
							keyDeclaringType,
							keyDeclaringTableGroupProducer,
							keySelectableMappings,
							creationProcess
					),
					embeddableValuedModelPart,
					keyTableExpression,
					keySelectableMappings,
					embeddableValuedModelPart.getContainingTableExpression(),
					embeddableValuedModelPart.getEmbeddableTypeDescriptor(),
					hasConstraint,
					creationProcess
			);
		}
	}

	private static int[] getPropertyOrder(Value bootValueMapping, MappingModelCreationProcess creationProcess) {
		final ComponentType componentType;
		final boolean sorted;
		if ( bootValueMapping instanceof Collection ) {
			final Collection collectionBootValueMapping = (Collection) bootValueMapping;
			componentType = (ComponentType) collectionBootValueMapping.getKey().getType();
			assert ( (SortableValue) collectionBootValueMapping.getKey() ).isSorted();
			sorted = ( (SortableValue) collectionBootValueMapping.getKey() ).isSorted();
		}
		else {
			final EntityType entityType = (EntityType) bootValueMapping.getType();
			final Type identifierOrUniqueKeyType = entityType.getIdentifierOrUniqueKeyType(
					creationProcess.getCreationContext().getSessionFactory()
			);
			if ( identifierOrUniqueKeyType instanceof ComponentType ) {
				componentType = (ComponentType) identifierOrUniqueKeyType;
				if ( bootValueMapping instanceof ToOne ) {
					assert ( (ToOne) bootValueMapping ).isSorted();
					sorted = ( (ToOne) bootValueMapping ).isSorted();
				}
				else {
					// Assume one-to-many is sorted, because it always uses the primary key value
					sorted = true;
				}
			}
			else {
				// This happens when we have a one-to-many with a mapped-by associations that has a basic FK
				return new int[] { 0 };
			}
		}
		// Consider the reordering if available
		if ( !sorted && componentType.getOriginalPropertyOrder() != null ) {
			return componentType.getOriginalPropertyOrder();
		}
		// A value that came from the annotation model is already sorted appropriately
		// so we use an "identity mapping"
		else {
			final int columnSpan = componentType.getColumnSpan( creationProcess.getCreationContext().getBootModel() );
			final int[] propertyReordering = new int[columnSpan];
			for ( int i = 0; i < columnSpan; i++ ) {
				propertyReordering[i] = i;
			}
			return propertyReordering;
		}
	}

	private static void setReferencedAttributeForeignKeyDescriptor(
			AbstractAttributeMapping attributeMapping,
			ToOneAttributeMapping referencedAttributeMapping,
			EntityPersister referencedEntityDescriptor,
			String referencedPropertyName,
			Dialect dialect,
			MappingModelCreationProcess creationProcess) {
		ForeignKeyDescriptor foreignKeyDescriptor = referencedAttributeMapping.getForeignKeyDescriptor();
		if ( foreignKeyDescriptor == null ) {
			PersistentClass entityBinding = creationProcess.getCreationContext()
					.getBootModel()
					.getEntityBinding(
							referencedEntityDescriptor.getEntityName() );
			Property property = entityBinding.getRecursiveProperty( referencedPropertyName );
			interpretToOneKeyDescriptor(
					referencedAttributeMapping,
					property,
					(ToOne) property.getValue(),
					referencedAttributeMapping.getPropertyAccess(),
					dialect,
					creationProcess
			);
			attributeMapping.setForeignKeyDescriptor( referencedAttributeMapping.getForeignKeyDescriptor() );
		}
		else {
			attributeMapping.setForeignKeyDescriptor( foreignKeyDescriptor );
		}
	}

	public static String getTableIdentifierExpression(Table table, MappingModelCreationProcess creationProcess) {
		return getTableIdentifierExpression( table, creationProcess.getCreationContext().getSessionFactory() );
	}

	public static String getTableIdentifierExpression(Table table, SessionFactoryImplementor sessionFactory) {
		if ( table.getSubselect() != null ) {
			return "( " + table.getSubselect() + " )";
		}

		return sessionFactory.getSqlStringGenerationContext().format( table.getQualifiedTableName() );
	}

	private static CollectionPart interpretMapKey(
			Collection bootValueMapping,
			CollectionPersister collectionDescriptor,
			String tableExpression,
			String sqlAliasStem,
			Dialect dialect,
			MappingModelCreationProcess creationProcess) {
		assert bootValueMapping instanceof IndexedCollection;
		final IndexedCollection indexedCollection = (IndexedCollection) bootValueMapping;
		final Value bootMapKeyDescriptor = indexedCollection.getIndex();

		if ( bootMapKeyDescriptor instanceof BasicValue ) {
			final BasicValue basicValue = (BasicValue) bootMapKeyDescriptor;
			final SelectableMapping selectableMapping = SelectableMappingImpl.from(
					tableExpression,
					basicValue.getSelectables().get(0),
					basicValue.resolve().getJdbcMapping(),
					creationProcess.getCreationContext().getTypeConfiguration(),
					dialect,
					creationProcess.getSqmFunctionRegistry()
			);
			return new BasicValuedCollectionPart(
					collectionDescriptor,
					CollectionPart.Nature.INDEX,
					selectableMapping
			);
		}

		if ( bootMapKeyDescriptor instanceof Component ) {
			final Component component = (Component) bootMapKeyDescriptor;
			final CompositeType compositeType = (CompositeType) component.getType();


			final EmbeddableMappingTypeImpl mappingType = EmbeddableMappingTypeImpl.from(
					component,
					compositeType,
					inflightDescriptor -> new EmbeddedCollectionPart(
							collectionDescriptor,
							CollectionPart.Nature.INDEX,
							inflightDescriptor,
							// parent-injection
							component.getParentProperty(),
							tableExpression,
							sqlAliasStem
					),
					creationProcess
			);

			return (CollectionPart) mappingType.getEmbeddedValueMapping();
		}

		if ( bootMapKeyDescriptor instanceof OneToMany || bootMapKeyDescriptor instanceof ToOne ) {
			final EntityType indexEntityType = (EntityType) collectionDescriptor.getIndexType();
			final EntityPersister associatedEntity = creationProcess.getEntityPersister( indexEntityType.getAssociatedEntityName() );

			final EntityCollectionPart indexDescriptor = new EntityCollectionPart(
					collectionDescriptor,
					CollectionPart.Nature.INDEX,
					bootMapKeyDescriptor,
					null,
					associatedEntity,
					creationProcess
			);

			creationProcess.registerInitializationCallback(
					"PluralAttributeMapping( " + bootValueMapping.getRole() + ") - index descriptor",
					() -> {
						indexDescriptor.finishInitialization(
								collectionDescriptor,
								bootValueMapping,
								indexEntityType.getRHSUniqueKeyPropertyName(),
								creationProcess
						);

						return true;
					}
			);

			return indexDescriptor;
		}

		throw new NotYetImplementedFor6Exception(
				"Support for plural attributes with index type [" + bootMapKeyDescriptor + "] not yet implemented"
		);
	}

	private static CollectionPart interpretElement(
			Collection bootDescriptor,
			String tableExpression,
			CollectionPersister collectionDescriptor,
			String sqlAliasStem,
			Dialect dialect,
			MappingModelCreationProcess creationProcess) {
		final Value element = bootDescriptor.getElement();

		if ( element instanceof BasicValue ) {
			final BasicValue basicElement = (BasicValue) element;
			final SelectableMapping selectableMapping = SelectableMappingImpl.from(
					tableExpression,
					basicElement.getSelectables().get(0),
					basicElement.resolve().getJdbcMapping(),
					creationProcess.getCreationContext().getTypeConfiguration(),
					dialect,
					creationProcess.getSqmFunctionRegistry()
			);
			return new BasicValuedCollectionPart(
					collectionDescriptor,
					CollectionPart.Nature.ELEMENT,
					selectableMapping
			);
		}

		if ( element instanceof Component ) {
			final Component component = (Component) element;
			final CompositeType compositeType = (CompositeType) collectionDescriptor.getElementType();


			final EmbeddableMappingTypeImpl mappingType = EmbeddableMappingTypeImpl.from(
					component,
					compositeType,
					embeddableMappingType -> new EmbeddedCollectionPart(
							collectionDescriptor,
							CollectionPart.Nature.ELEMENT,
							embeddableMappingType,
							// parent-injection
							component.getParentProperty(),
							tableExpression,
							sqlAliasStem
					),
					creationProcess
			);

			return (CollectionPart) mappingType.getEmbeddedValueMapping();
		}

		if ( element instanceof Any ) {
			final Any anyBootMapping = (Any) element;

			final SessionFactoryImplementor sessionFactory = creationProcess.getCreationContext().getSessionFactory();
			final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();
			final JavaTypeRegistry jtdRegistry = typeConfiguration.getJavaTypeRegistry();
			final JavaType<Object> baseJtd = jtdRegistry.getDescriptor(Object.class);

			return new DiscriminatedCollectionPart(
					CollectionPart.Nature.ELEMENT,
					collectionDescriptor,
					baseJtd,
					anyBootMapping,
					anyBootMapping.getType(),
					creationProcess
			);
		}

		if ( element instanceof OneToMany || element instanceof ToOne ) {
			final EntityType elementEntityType = (EntityType) collectionDescriptor.getElementType();
			final EntityPersister associatedEntity = creationProcess.getEntityPersister( elementEntityType.getAssociatedEntityName() );

			final NotFoundAction notFoundAction;
			if ( element instanceof ManyToOne ) {
				notFoundAction = ( (ManyToOne) element ).getNotFoundAction();
			}
			else if ( element instanceof OneToMany ) {
				notFoundAction = ( (OneToMany) element ).getNotFoundAction();
			}
			else {
				throw new IllegalArgumentException( "Just seeing if this happens" );
			}

			final EntityCollectionPart elementDescriptor = new EntityCollectionPart(
					collectionDescriptor,
					CollectionPart.Nature.ELEMENT,
					bootDescriptor.getElement(),
					notFoundAction,
					associatedEntity,
					creationProcess
			);

			creationProcess.registerInitializationCallback(
					"PluralAttributeMapping( " + elementDescriptor.getNavigableRole() + ") - index descriptor",
					() -> {
						elementDescriptor.finishInitialization(
								collectionDescriptor,
								bootDescriptor,
								elementEntityType.getRHSUniqueKeyPropertyName(),
								creationProcess
						);

						return true;
					}
			);

			return elementDescriptor;
		}

		throw new NotYetImplementedFor6Exception(
				"Support for plural attributes with element type [" + element + "] not yet implemented"
		);
	}

	public static EmbeddedAttributeMapping createInverseModelPart(
			EmbeddableValuedModelPart modelPart,
			ManagedMappingType keyDeclaringType,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			MappingModelCreationProcess creationProcess) {
		final EmbeddableMappingType embeddableTypeDescriptor = modelPart.getEmbeddableTypeDescriptor();
		if ( modelPart instanceof NonAggregatedIdentifierMapping ) {
			return new InverseNonAggregatedIdentifierMapping(
					keyDeclaringType,
					declaringTableGroupProducer,
					selectableMappings,
					(NonAggregatedIdentifierMapping) modelPart,
					embeddableTypeDescriptor,
					creationProcess
			);
		}
		else if ( modelPart instanceof VirtualModelPart ) {
			return new VirtualEmbeddedAttributeMapping(
					keyDeclaringType,
					declaringTableGroupProducer,
					selectableMappings,
					modelPart,
					embeddableTypeDescriptor,
					creationProcess
			);
		}
		else {
			return new EmbeddedAttributeMapping(
					keyDeclaringType,
					declaringTableGroupProducer,
					selectableMappings,
					modelPart,
					embeddableTypeDescriptor,
					creationProcess
			);
		}
	}

	private static class CollectionMappingTypeImpl implements CollectionMappingType {
		private final JavaType<?> collectionJtd;
		private final CollectionSemantics<?,?> semantics;

		public CollectionMappingTypeImpl(
				JavaType<?> collectionJtd,
				CollectionSemantics<?,?> semantics) {
			this.collectionJtd = collectionJtd;
			this.semantics = semantics;
		}

		@Override
		public CollectionSemantics<?,?> getCollectionSemantics() {
			return semantics;
		}

		@Override
		public JavaType<?> getMappedJavaType() {
			return collectionJtd;
		}
	}

	public static ToOneAttributeMapping buildSingularAssociationAttributeMapping(
			String attrName,
			NavigableRole navigableRole,
			int stateArrayPosition,
			Property bootProperty,
			ManagedMappingType declaringType,
			EntityPersister declaringEntityPersister,
			EntityType attrType,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess) {
		if ( bootProperty.getValue() instanceof ToOne ) {
			final ToOne value = (ToOne) bootProperty.getValue();
			final EntityPersister entityPersister = creationProcess.getEntityPersister( value.getReferencedEntityName() );
			final AttributeMetadataAccess attributeMetadataAccess = getAttributeMetadataAccess(
					bootProperty,
					attrType,
					propertyAccess,
					cascadeStyle,
					creationProcess
			);
			SessionFactoryImplementor sessionFactory = creationProcess.getCreationContext().getSessionFactory();

			final AssociationType type = (AssociationType) bootProperty.getType();
			final FetchStyle fetchStyle = FetchOptionsHelper
					.determineFetchStyleByMetadata(
							bootProperty.getValue().getFetchMode(),
							type,
							sessionFactory
					);

			final FetchTiming fetchTiming;
			final String role = declaringType.getNavigableRole().toString() + "." + bootProperty.getName();
			final boolean lazy = value.isLazy();
			if ( lazy && entityPersister.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading() ) {
				if ( value.isUnwrapProxy() ) {
					fetchTiming = FetchOptionsHelper.determineFetchTiming( fetchStyle, type, lazy, role, sessionFactory );
				}
				else if ( value instanceof ManyToOne && value.isNullable() && ( (ManyToOne) value ).isIgnoreNotFound() ) {
					fetchTiming = FetchTiming.IMMEDIATE;
				}
				else {
					fetchTiming = FetchOptionsHelper.determineFetchTiming( fetchStyle, type, lazy, role, sessionFactory );
				}
			}
			else if ( !lazy
					|| value instanceof OneToOne && value.isNullable()
					|| value instanceof ManyToOne && value.isNullable() && ( (ManyToOne) value ).isIgnoreNotFound() ) {
				fetchTiming = FetchTiming.IMMEDIATE;
			}
			else {
				fetchTiming = FetchOptionsHelper.determineFetchTiming( fetchStyle, type, lazy, role, sessionFactory );
			}

			final ToOneAttributeMapping attributeMapping = new ToOneAttributeMapping(
					attrName,
					navigableRole,
					stateArrayPosition,
					(ToOne) bootProperty.getValue(),
					attributeMetadataAccess,
					fetchTiming,
					fetchStyle,
					entityPersister,
					declaringType,
					declaringEntityPersister,
					propertyAccess
			);

			creationProcess.registerForeignKeyPostInitCallbacks(
					"To-one key - " + navigableRole,
					() -> {
						final Dialect dialect = creationProcess.getCreationContext()
								.getSessionFactory()
								.getJdbcServices()
								.getDialect();

						return MappingModelCreationHelper.interpretToOneKeyDescriptor(
								attributeMapping,
								bootProperty,
								(ToOne) bootProperty.getValue(),
								null,
								dialect,
								creationProcess
						);
					}
			);
			return attributeMapping;
		}
		else {
			throw new NotYetImplementedFor6Exception( "AnyType support has not yet been implemented" );
		}
	}
}
