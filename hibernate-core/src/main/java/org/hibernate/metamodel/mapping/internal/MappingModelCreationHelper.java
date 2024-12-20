/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.SharedSessionContract;
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
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
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
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.PropertyBasedMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.VirtualModelPart;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.internal.ChainedPropertyAccessImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasStemHelper;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
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

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.metamodel.mapping.MappingModelCreationLogging.MAPPING_MODEL_CREATION_MESSAGE_LOGGER;
import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

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

		final Component component = (Component) bootProperty.getValue();
		final EmbeddableMappingTypeImpl embeddableMappingType = EmbeddableMappingTypeImpl.from(
				component,
				cidType,
				rootTableName,
				rootTableKeyColumnNames,
				bootProperty,
				null,
				0,
				component.getColumnInsertability(),
				component.getColumnUpdateability(),
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
			int fetchableIndex,
			Property bootProperty,
			ManagedMappingType declaringType,
			BasicType attrType,
			String tableExpression,
			String attrColumnName,
			SelectablePath selectablePath,
			boolean isAttrFormula,
			String readExpr,
			String writeExpr,
			String columnDefinition,
			Long length,
			Integer precision,
			Integer scale,
			Integer temporalPrecision,
			boolean isLob,
			boolean nullable,
			boolean insertable,
			boolean updateable,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess) {
		final SimpleValue value = (SimpleValue) bootProperty.getValue();
		final BasicValue.Resolution<?> resolution = ( (Resolvable) value ).resolve();
		final SimpleAttributeMetadata attributeMetadata = new SimpleAttributeMetadata( propertyAccess, resolution.getMutabilityPlan(), bootProperty, value );

		final FetchTiming fetchTiming;
		final FetchStyle fetchStyle;
		final boolean partitioned;
		if ( declaringType instanceof EmbeddableMappingType ) {
			if ( bootProperty.isLazy() ) {
				MAPPING_MODEL_CREATION_MESSAGE_LOGGER.debugf(
						"Attribute was declared lazy, but is part of an embeddable - `%s#%s` - LAZY will be ignored",
						declaringType.getNavigableRole().getFullPath(),
						bootProperty.getName()
				);
			}
			fetchTiming = FetchTiming.IMMEDIATE;
			fetchStyle = FetchStyle.JOIN;
			partitioned = value.isPartitionKey() && !( (EmbeddableMappingType) declaringType ).getEmbeddedValueMapping().isVirtual();
		}
		else {
			fetchTiming = bootProperty.isLazy() ? FetchTiming.DELAYED : FetchTiming.IMMEDIATE;
			fetchStyle = bootProperty.isLazy() ? FetchStyle.SELECT : FetchStyle.JOIN;
			partitioned = value.isPartitionKey();
		}

		return new BasicAttributeMapping(
				attrName,
				navigableRole,
				stateArrayPosition,
				fetchableIndex,
				attributeMetadata,
				fetchTiming,
				fetchStyle,
				tableExpression,
				attrColumnName,
				selectablePath,
				isAttrFormula,
				readExpr,
				writeExpr,
				columnDefinition,
				length,
				precision,
				scale,
				temporalPrecision,
				isLob,
				nullable,
				insertable,
				updateable,
				partitioned,
				attrType,
				declaringType,
				propertyAccess
		);
	}

	public static EmbeddedAttributeMapping buildEmbeddedAttributeMapping(
			String attrName,
			int stateArrayPosition,
			int fetchableIndex,
			Property bootProperty,
			ManagedMappingType declaringType,
			CompositeType attrType,
			String tableExpression,
			String[] rootTableKeyColumnNames,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess) {
		return buildEmbeddedAttributeMapping(
				attrName,
				stateArrayPosition,
				fetchableIndex,
				bootProperty,
				null,
				0,
				declaringType,
				attrType,
				tableExpression,
				rootTableKeyColumnNames,
				propertyAccess,
				cascadeStyle,
				creationProcess
		);
	}

	public static EmbeddedAttributeMapping buildEmbeddedAttributeMapping(
			String attrName,
			int stateArrayPosition,
			int fetchableIndex,
			Property bootProperty,
			DependantValue dependantValue,
			int dependantColumnIndex,
			ManagedMappingType declaringType,
			CompositeType attrType,
			String tableExpression,
			String[] rootTableKeyColumnNames,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess) {
		final AttributeMetadata attributeMetadataAccess = getAttributeMetadata(
				bootProperty,
				attrType,
				propertyAccess,
				cascadeStyle,
				creationProcess
		);

		Value componentValue = bootProperty.getValue();
		if ( componentValue instanceof DependantValue && dependantValue != null ) {
			componentValue = dependantValue.getWrappedValue();
		}

		final Component component = (Component) componentValue;
		final EmbeddableMappingTypeImpl embeddableMappingType = EmbeddableMappingTypeImpl.from(
				component,
				attrType,
				tableExpression,
				rootTableKeyColumnNames,
				bootProperty,
				dependantValue,
				dependantColumnIndex,
				component.getColumnInsertability(),
				component.getColumnUpdateability(),
				attributeMappingType -> {
					if ( component.isEmbedded() ) {
						return new VirtualEmbeddedAttributeMapping(
								attrName,
								declaringType.getNavigableRole().append( attrName ),
								stateArrayPosition,
								fetchableIndex,
								tableExpression,
								attributeMetadataAccess,
								component.getParentProperty(),
								FetchTiming.IMMEDIATE,
								FetchStyle.JOIN,
								attributeMappingType,
								declaringType,
								propertyAccess
						);
					}
					else {
						return new EmbeddedAttributeMapping(
								attrName,
								declaringType.getNavigableRole().append( attrName ),
								stateArrayPosition,
								fetchableIndex,
								tableExpression,
								attributeMetadataAccess,
								component.getParentProperty(),
								FetchTiming.IMMEDIATE,
								FetchStyle.JOIN,
								attributeMappingType,
								declaringType,
								propertyAccess
						);
					}
				},
				creationProcess
		);

		return (EmbeddedAttributeMapping) embeddableMappingType.getEmbeddedValueMapping();
	}

	@SuppressWarnings("rawtypes")
	protected static AttributeMetadata getAttributeMetadata(
			Property bootProperty,
			Type attrType,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess) {
		final MutabilityPlan mutabilityPlan = getMutabilityPlan( bootProperty, attrType, creationProcess );
		return new SimpleAttributeMetadata(
				propertyAccess,
				mutabilityPlan,
				bootProperty.getValue().isNullable(),
				bootProperty.isInsertable(),
				bootProperty.isUpdateable(),
				bootProperty.isOptimisticLocked(),
				bootProperty.isSelectable(),
				cascadeStyle
		);
	}

	private static MutabilityPlan getMutabilityPlan(
			Property bootProperty,
			Type attrType,
			MappingModelCreationProcess creationProcess) {
		if ( bootProperty.isUpdateable() ) {
			return new MutabilityPlan() {

				final SessionFactoryImplementor sessionFactory =
						creationProcess.getCreationContext().getSessionFactory();

				@Override
				public boolean isMutable() {
					return true;
				}

				@Override
				public Object deepCopy(Object value) {
					if ( value == null ) {
						return null;
					}

					return attrType.deepCopy( value, sessionFactory );
				}

				@Override
				public Serializable disassemble(Object value, SharedSessionContract session) {
					throw new UnsupportedOperationException();
				}

				@Override
				public Object assemble(Serializable cached, SharedSessionContract session) {
					throw new UnsupportedOperationException();
				}
			};
		}
		else {
			return ImmutableMutabilityPlan.instance();
		}
	}

	public static AttributeMetadata getAttributeMetadata(PropertyAccess propertyAccess) {
		return new SimpleAttributeMetadata( propertyAccess, ImmutableMutabilityPlan.instance(), false, true, false, false, true, null);// todo (6.0) : not sure if CascadeStyle=null is correct
	}

	public static PluralAttributeMapping buildPluralAttributeMapping(
			String attrName,
			int stateArrayPosition,
			int fetchableIndex,
			Property bootProperty,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			FetchMode fetchMode,
			MappingModelCreationProcess creationProcess) {
		return buildPluralAttributeMapping(
				attrName,
				stateArrayPosition,
				fetchableIndex,
				bootProperty,
				declaringType,
				propertyAccess,
				cascadeStyle,
				fetchMode,
				creationProcess,
				Function.identity()
		);
	}

	/**
	 * For Hibernate Reactive
	 */
	@SuppressWarnings("rawtypes")
	public static PluralAttributeMapping buildPluralAttributeMapping(
			String attrName,
			int stateArrayPosition,
			int fetchableIndex,
			Property bootProperty,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			FetchMode fetchMode,
			MappingModelCreationProcess creationProcess,
			Function<PluralAttributeMappingImpl, PluralAttributeMappingImpl> mappingConverter) {

		final Collection bootValueMapping = (Collection) bootProperty.getValue();

		final RuntimeModelCreationContext creationContext = creationProcess.getCreationContext();
		final Dialect dialect = creationContext.getDialect();
		final MappingMetamodel domainModel = creationContext.getDomainModel();

		final CollectionPersister collectionDescriptor = domainModel.findCollectionDescriptor( bootValueMapping.getRole() );
		assert collectionDescriptor != null;

		final String tableExpression = collectionDescriptor.getTableName();

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
						index.isColumnInsertable( 0 ),
						index.isColumnUpdateable( 0 ),
						false,
						dialect,
						creationProcess.getSqmFunctionRegistry(),
						creationProcess.getCreationContext()
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

				final String identifierColumnName = collectionDescriptor.getIdentifierColumnName();
				assert identifierColumnName != null;

				identifierDescriptor = new CollectionIdentifierDescriptorImpl(
						collectionDescriptor,
						tableExpression,
						identifierColumnName,
						(BasicType) collectionDescriptor.getIdentifierType()
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
						index.isColumnInsertable( 0 ),
						index.isColumnUpdateable( 0 ),
						false,
						dialect,
						creationProcess.getSqmFunctionRegistry(),
						creationProcess.getCreationContext()
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

		SimpleAttributeMetadata attributeMetadata = new SimpleAttributeMetadata(
				propertyAccess,
				ImmutableMutabilityPlan.instance(),
				bootProperty.isOptional(),
				bootProperty.isInsertable(),
				bootProperty.isUpdateable(),
				bootProperty.isOptimisticLocked(),
				bootProperty.isSelectable(),
				cascadeStyle
		);

		final SessionFactoryImplementor sessionFactory = creationContext.getSessionFactory();
		final FetchStyle style = FetchOptionsHelper.determineFetchStyleByMetadata(
				fetchMode,
				collectionDescriptor.getCollectionType(),
				sessionFactory
		);

		final FetchTiming timing = FetchOptionsHelper.determineFetchTiming(
				style,
				collectionDescriptor.getCollectionType(),
				collectionDescriptor.isLazy(),
				sessionFactory
		);

		final PluralAttributeMappingImpl pluralAttributeMapping = mappingConverter.apply( new PluralAttributeMappingImpl(
				attrName,
				bootValueMapping,
				propertyAccess,
				attributeMetadata,
				collectionMappingType,
				stateArrayPosition,
				fetchableIndex,
				elementDescriptor,
				indexDescriptor,
				identifierDescriptor,
				timing,
				style,
				cascadeStyle,
				declaringType,
				collectionDescriptor,
				creationProcess
		) );

		creationProcess.registerInitializationCallback(
				"PluralAttributeMapping(" + bootValueMapping.getRole() + ")#finishInitialization",
				() -> {
					pluralAttributeMapping.finishInitialization(
							bootProperty,
							bootValueMapping,
							creationProcess
					);
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
		final ModelPart fkTargetPart;
		final String lhsPropertyName = collectionDescriptor.getCollectionType().getLHSPropertyName();
		final boolean isReferenceToPrimaryKey = lhsPropertyName == null;
		final ManagedMappingType keyDeclaringType;
		final String collectionTableName = collectionDescriptor.getTableName();

		if ( collectionDescriptor.getElementType() instanceof EntityType ) {
			keyDeclaringType = collectionDescriptor.getElementPersister();
		}
		else {
			// This is not "really correct" but it is as good as it gets.
			// The key declaring type serves as declaring type for the inverse model part of a FK.
			// Most of the time, there is a proper managed type, but not for basic collections.
			// Since the declaring type is needed for certain operations, we use the one from the target side of the FK
			keyDeclaringType = declaringType;
		}

		if ( isReferenceToPrimaryKey ) {
			fkTargetPart = collectionDescriptor.getOwnerEntityPersister().getIdentifierMappingForJoin();
//			fkTargetPart = collectionDescriptor.getOwnerEntityPersister().getIdentifierMapping();
		}
		else {
			fkTargetPart = declaringType.findContainingEntityMapping().findSubPart( lhsPropertyName );
		}

		if ( keyType instanceof BasicType ) {
			assert bootValueMappingKey.getColumnSpan() == 1;

			final BasicValuedModelPart simpleFkTargetPart = castNonNull( fkTargetPart.asBasicValuedModelPart() );

			final String keyTableExpression = collectionTableName;//getTableIdentifierExpression( bootValueMappingKey.getTable(), creationProcess );
			final SelectableMapping keySelectableMapping = SelectableMappingImpl.from(
					keyTableExpression,
					bootValueMappingKey.getSelectables().get(0),
					(JdbcMapping) keyType,
					creationProcess.getCreationContext().getTypeConfiguration(),
					bootValueMappingKey.isColumnInsertable( 0 ),
					bootValueMappingKey.isColumnUpdateable( 0 ),
					false,
					dialect,
					creationProcess.getSqmFunctionRegistry(),
					creationProcess.getCreationContext()
			);

			final SimpleForeignKeyDescriptor keyDescriptor = new SimpleForeignKeyDescriptor(
					keyDeclaringType,
					keySelectableMapping,
					simpleFkTargetPart,
					isReferenceToPrimaryKey,
					( (SimpleValue) bootValueMappingKey ).isConstrained()
			);
			attributeMapping.setForeignKeyDescriptor( keyDescriptor );
			creationProcess.registerForeignKey( collectionDescriptor.getAttributeMapping(), keyDescriptor );
		}
		else if ( fkTargetPart instanceof EmbeddableValuedModelPart ) {
			final EmbeddedForeignKeyDescriptor keyDescriptor = buildEmbeddableForeignKeyDescriptor(
					(EmbeddableValuedModelPart) fkTargetPart,
					bootValueMapping,
					keyDeclaringType,
					collectionDescriptor.getAttributeMapping(),
					collectionTableName,
					false,
					bootValueMappingKey.getColumnInsertability(),
					bootValueMappingKey.getColumnUpdateability(),
					dialect,
					creationProcess
			);

			attributeMapping.setForeignKeyDescriptor( keyDescriptor );
			creationProcess.registerForeignKey( collectionDescriptor.getAttributeMapping(), keyDescriptor );
		}
		else {
			throw new UnsupportedOperationException(
					"Support for " + fkTargetPart.getClass() + " foreign keys not yet implemented: " + bootValueMapping.getRole()
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
			if ( referencedPropertyName.indexOf( '.' ) > 0 ) {
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
						bootValueMapping.getColumnInsertability(),
						bootValueMapping.getColumnUpdateability(),
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
				throw new UnsupportedOperationException(
						"Support for foreign-keys based on `" + modelPart + "` not yet implemented: " +
								bootProperty.getPersistentClass().getEntityName() + " -> " + bootProperty.getName()
				);
			}
			return true;
		}

		final ModelPart fkTarget;
		if ( bootValueMapping.isReferenceToPrimaryKey() ) {
			fkTarget = referencedEntityDescriptor.getIdentifierMappingForJoin();
//			fkTarget = referencedEntityDescriptor.getIdentifierMapping();
		}
		else {
			fkTarget = referencedEntityDescriptor.findByPath( bootValueMapping.getReferencedPropertyName() );
		}

		final BasicValuedModelPart simpleFkTarget = fkTarget.asBasicValuedModelPart();
		if ( simpleFkTarget != null ) {
			final Iterator<Selectable> columnIterator = bootValueMapping.getSelectables().iterator();
			final Table table = bootValueMapping.getTable();
			final String tableExpression = getTableIdentifierExpression( table, creationProcess );
			final PropertyAccess declaringKeyPropertyAccess;
			if ( inversePropertyAccess == null ) {
				// So far, OneToOne mappings are only supported based on the owner's PK
				if ( bootValueMapping instanceof OneToOne ) {
					final EntityIdentifierMapping identifierMapping = attributeMapping.findContainingEntityMapping()
							.getIdentifierMapping();
					declaringKeyPropertyAccess = ( (PropertyBasedMapping) identifierMapping ).getPropertyAccess();
				}
				else {
					declaringKeyPropertyAccess = new ChainedPropertyAccessImpl(
							attributeMapping.getPropertyAccess(),
							( (PropertyBasedMapping) simpleFkTarget ).getPropertyAccess()
					);
				}
			}
			else {
				declaringKeyPropertyAccess = new ChainedPropertyAccessImpl(
						inversePropertyAccess,
						( (PropertyBasedMapping) simpleFkTarget ).getPropertyAccess()
				);
			}
			final SelectableMapping keySelectableMapping;
			int i = 0;
			final Value value = bootProperty.getValue();
			if ( columnIterator.hasNext() ) {
				keySelectableMapping = SelectableMappingImpl.from(
						tableExpression,
						columnIterator.next(),
						simpleFkTarget.getJdbcMapping(),
						creationProcess.getCreationContext().getTypeConfiguration(),
						value.isColumnInsertable( i ),
						value.isColumnUpdateable( i ),
						((SimpleValue) value).isPartitionKey(),
						dialect,
						creationProcess.getSqmFunctionRegistry(),
						creationProcess.getCreationContext()
				);
				i++;
			}
			else {
				// case of ToOne with @PrimaryKeyJoinColumn
				keySelectableMapping = SelectableMappingImpl.from(
						tableExpression,
						table.getPrimaryKey().getColumn( 0 ),
						simpleFkTarget.getJdbcMapping(),
						creationProcess.getCreationContext().getTypeConfiguration(),
						value.isColumnInsertable( 0 ),
						value.isColumnUpdateable( 0 ),
						((SimpleValue) value).isPartitionKey(),
						dialect,
						creationProcess.getSqmFunctionRegistry(),
						creationProcess.getCreationContext()
				);
			}

			final ForeignKeyDescriptor foreignKeyDescriptor = new SimpleForeignKeyDescriptor(
					attributeMapping.getDeclaringType(),
					keySelectableMapping,
					declaringKeyPropertyAccess,
					simpleFkTarget,
					bootValueMapping.isReferenceToPrimaryKey(),
					bootValueMapping.isConstrained(),
					swapDirection
			);
			attributeMapping.setForeignKeyDescriptor( foreignKeyDescriptor );
			creationProcess.registerForeignKey( attributeMapping, foreignKeyDescriptor );
		}
		else if ( fkTarget instanceof EmbeddableValuedModelPart ) {
			final Value value = bootProperty.getValue();
			final EmbeddedForeignKeyDescriptor embeddedForeignKeyDescriptor = buildEmbeddableForeignKeyDescriptor(
					(EmbeddableValuedModelPart) fkTarget,
					bootValueMapping,
					attributeMapping.getDeclaringType(),
					attributeMapping.findContainingEntityMapping(),
					swapDirection,
					value.getColumnInsertability(),
					value.getColumnUpdateability(),
					dialect,
					creationProcess
			);
			attributeMapping.setForeignKeyDescriptor( embeddedForeignKeyDescriptor );
			creationProcess.registerForeignKey( attributeMapping, embeddedForeignKeyDescriptor );
		}
		else {
			throw new UnsupportedOperationException(
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
			boolean[] insertable,
			boolean[] updateable,
			Dialect dialect,
			MappingModelCreationProcess creationProcess) {
		return buildEmbeddableForeignKeyDescriptor(
				embeddableValuedModelPart,
				bootValueMapping,
				keyDeclaringType,
				keyDeclaringTableGroupProducer,
				null,
				inverse,
				insertable,
				updateable,
				dialect,
				creationProcess
		);
	}

	private static EmbeddedForeignKeyDescriptor buildEmbeddableForeignKeyDescriptor(
			EmbeddableValuedModelPart embeddableValuedModelPart,
			Value bootValueMapping,
			ManagedMappingType keyDeclaringType,
			TableGroupProducer keyDeclaringTableGroupProducer,
			String keyTableExpression,
			boolean inverse,
			boolean[] insertable,
			boolean[] updateable,
			Dialect dialect,
			MappingModelCreationProcess creationProcess) {
		final boolean hasConstraint;
		final SelectableMappings keySelectableMappings;
		if ( bootValueMapping instanceof Collection ) {
			final Collection collectionBootValueMapping = (Collection) bootValueMapping;
			hasConstraint = ((SimpleValue) collectionBootValueMapping.getKey()).isConstrained();
			keyTableExpression = keyTableExpression != null ? keyTableExpression : getTableIdentifierExpression(
					collectionBootValueMapping.getCollectionTable(),
					creationProcess
			);
			keySelectableMappings = SelectableMappingsImpl.from(
					keyTableExpression,
					collectionBootValueMapping.getKey(),
					getPropertyOrder( bootValueMapping, creationProcess ),
					creationProcess.getCreationContext().getMetadata(),
					creationProcess.getCreationContext().getTypeConfiguration(),
					insertable,
					updateable,
					dialect,
					creationProcess.getSqmFunctionRegistry(),
					creationProcess.getCreationContext()
			);
		}
		else {
			if ( bootValueMapping instanceof OneToMany ) {
				// We assume there is a constraint if the mapping is not nullable
				hasConstraint = !bootValueMapping.isNullable();
			}
			else {
				hasConstraint = ((SimpleValue) bootValueMapping).isConstrained();
			}
			keyTableExpression = keyTableExpression != null ? keyTableExpression : getTableIdentifierExpression(
					bootValueMapping.getTable(),
					creationProcess
			);
			keySelectableMappings = SelectableMappingsImpl.from(
					keyTableExpression,
					bootValueMapping,
					getPropertyOrder( bootValueMapping, creationProcess ),
					creationProcess.getCreationContext().getMetadata(),
					creationProcess.getCreationContext().getTypeConfiguration(),
					insertable,
					updateable,
					dialect,
					creationProcess.getSqmFunctionRegistry(),
					creationProcess.getCreationContext()
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

	public static int[] getPropertyOrder(Value bootValueMapping, MappingModelCreationProcess creationProcess) {
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
					creationProcess.getCreationContext().getMetadata()
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
			foreignKeyDescriptor = referencedAttributeMapping.getForeignKeyDescriptor();
		}

		final EntityMappingType declaringEntityMapping = attributeMapping.findContainingEntityMapping();
		if ( foreignKeyDescriptor.getTargetPart() instanceof EntityIdentifierMapping
				&& foreignKeyDescriptor.getTargetPart() != declaringEntityMapping.getIdentifierMapping() ) {
			// If the many-to-one refers to the super type, but the one-to-many is defined in a subtype,
			// it would be wasteful to reuse the FK descriptor of the many-to-one,
			// because that refers to the PK column in the root table.
			// Joining such an association then requires that we join the root table
			attributeMapping.setForeignKeyDescriptor(
					foreignKeyDescriptor.withTargetPart( declaringEntityMapping.getIdentifierMapping() )
			);
		}
		else {
			attributeMapping.setForeignKeyDescriptor( foreignKeyDescriptor );
		}
	}

	public static String getTableIdentifierExpression(Table table, MappingModelCreationProcess creationProcess) {
		if ( table.getSubselect() != null ) {
			return "( " + table.getSubselect() + " )";
		}

		return creationProcess.getCreationContext().getSqlStringGenerationContext()
				.format( table.getQualifiedTableName() );
	}

	public static String getTableIdentifierExpression(Table table, SessionFactoryImplementor sessionFactory) {
		if ( table.getSubselect() != null ) {
			return "( " + table.getSubselect() + " )";
		}

		return sessionFactory.getSqlStringGenerationContext()
				.format( table.getQualifiedTableName() );
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
			final boolean insertable;
			final boolean updatable;
			if ( indexedCollection instanceof org.hibernate.mapping.Map
					&& ( (org.hibernate.mapping.Map) indexedCollection ).getMapKeyPropertyName() != null ) {
				// Replicate behavior of AbstractCollectionPersister#indexColumnIsSettable
				insertable = false;
				updatable = false;
			}
			else {
				insertable = updatable = basicValue.isColumnInsertable( 0 )
						|| basicValue.isColumnUpdateable( 0 );
			}
			final SelectableMapping selectableMapping = SelectableMappingImpl.from(
					tableExpression,
					basicValue.getSelectables().get( 0 ),
					basicValue.resolve().getJdbcMapping(),
					creationProcess.getCreationContext().getTypeConfiguration(),
					insertable,
					updatable,
					false,
					dialect,
					creationProcess.getSqmFunctionRegistry(),
					creationProcess.getCreationContext()
			);
			return new BasicValuedCollectionPart(
					collectionDescriptor,
					CollectionPart.Nature.INDEX,
					selectableMapping
			);
		}

		if ( bootMapKeyDescriptor instanceof Component ) {
			final Component component = (Component) bootMapKeyDescriptor;
			final CompositeType compositeType = component.getType();


			final EmbeddableMappingTypeImpl mappingType = EmbeddableMappingTypeImpl.from(
					component,
					compositeType,
					component.getColumnInsertability(),
					component.getColumnUpdateability(),
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

			final EntityCollectionPart indexDescriptor;
			if ( bootMapKeyDescriptor instanceof OneToMany ) {
				indexDescriptor = new OneToManyCollectionPart(
						CollectionPart.Nature.INDEX,
						bootValueMapping,
						collectionDescriptor,
						associatedEntity,
						creationProcess
				);
			}
			else {
				indexDescriptor = new ManyToManyCollectionPart(
						CollectionPart.Nature.INDEX,
						bootValueMapping,
						collectionDescriptor,
						associatedEntity,
						creationProcess
				);
			}

			creationProcess.registerInitializationCallback(
					"PluralAttributeMapping( " + bootValueMapping.getRole() + ") - index descriptor",
					() -> indexDescriptor.finishInitialization(
							collectionDescriptor,
							bootValueMapping,
							indexEntityType.getRHSUniqueKeyPropertyName(),
							creationProcess
					)
			);

			return indexDescriptor;
		}

		throw new UnsupportedOperationException(
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
					basicElement.isColumnInsertable( 0 ),
					basicElement.isColumnUpdateable( 0 ),
					basicElement.isPartitionKey(),
					true, // element collection does not support null elements
					dialect,
					creationProcess.getSqmFunctionRegistry(),
					creationProcess.getCreationContext()
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
					component.getColumnInsertability(),
					component.getColumnUpdateability(),
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

			final TypeConfiguration typeConfiguration = creationProcess.getCreationContext().getTypeConfiguration();
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

			final EntityCollectionPart elementDescriptor;
			if ( element instanceof OneToMany ) {
				elementDescriptor = new OneToManyCollectionPart(
						CollectionPart.Nature.ELEMENT,
						bootDescriptor,
						collectionDescriptor,
						associatedEntity,
						( (OneToMany) element ).getNotFoundAction(),
						creationProcess
				);
			}
			else {
				elementDescriptor = new ManyToManyCollectionPart(
						CollectionPart.Nature.ELEMENT,
						bootDescriptor,
						collectionDescriptor,
						associatedEntity,
						( (ManyToOne) element ).getNotFoundAction(),
						creationProcess
				);
			}

			creationProcess.registerInitializationCallback(
					"PluralAttributeMapping( " + elementDescriptor.getNavigableRole() + ") - element descriptor",
					() -> elementDescriptor.finishInitialization(
							collectionDescriptor,
							bootDescriptor,
							elementEntityType.getRHSUniqueKeyPropertyName(),
							creationProcess
					)
			);

			return elementDescriptor;
		}

		throw new UnsupportedOperationException(
				"Unexpected plural-attribute element type : " + element.getClass().getName()
		);
	}

	public static EmbeddedAttributeMapping createInverseModelPart(
			EmbeddableValuedModelPart modelPart,
			ManagedMappingType keyDeclaringType,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			MappingModelCreationProcess creationProcess) {
		final EmbeddableMappingType embeddableTypeDescriptor = modelPart.getEmbeddableTypeDescriptor();
		if ( modelPart instanceof org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping ) {
			return new InverseNonAggregatedIdentifierMapping(
					keyDeclaringType,
					declaringTableGroupProducer,
					selectableMappings,
					(org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping) modelPart,
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

	public static Expression buildColumnReferenceExpression(
			TableGroup tableGroup,
			ModelPart modelPart,
			SqlExpressionResolver sqlExpressionResolver,
			SessionFactoryImplementor sessionFactory) {
		final int jdbcTypeCount = modelPart.getJdbcTypeCount();

		if ( modelPart instanceof EmbeddableValuedModelPart ) {
			final List<ColumnReference> columnReferences = new ArrayList<>( jdbcTypeCount );
			modelPart.forEachSelectable(
					(columnIndex, selection) -> {
						final ColumnReference colRef;
						final String qualifier;
						if ( tableGroup == null ) {
							qualifier = selection.getContainingTableExpression();
						}
						else {
							qualifier = tableGroup.resolveTableReference( selection.getContainingTableExpression() ).getIdentificationVariable();
						}
						if ( sqlExpressionResolver == null ) {
							colRef = new ColumnReference( qualifier, selection );
						}
						else {
							colRef = (ColumnReference) sqlExpressionResolver.resolveSqlExpression(
									createColumnReferenceKey( qualifier, selection ),
									sqlAstProcessingState -> new ColumnReference( qualifier, selection )
							);
						}
						columnReferences.add( colRef );
					}
			);
			return new SqlTuple( columnReferences, modelPart );
		}
		else {
			final BasicValuedModelPart basicPart = castNonNull( modelPart.asBasicValuedModelPart() );
			final String qualifier;
			if ( tableGroup == null ) {
				qualifier = basicPart.getContainingTableExpression();
			}
			else {
				qualifier = tableGroup.resolveTableReference( basicPart.getContainingTableExpression() ).getIdentificationVariable();
			}
			if ( sqlExpressionResolver == null ) {
				return new ColumnReference( qualifier, basicPart );
			}
			else {
				return sqlExpressionResolver.resolveSqlExpression(
						createColumnReferenceKey( qualifier, basicPart ),
						sqlAstProcessingState -> new ColumnReference( qualifier, basicPart )
				);
			}
		}
	}

	public static BasicType<?> resolveAggregateColumnBasicType(
			MappingModelCreationProcess creationProcess,
			NavigableRole navigableRole,
			Column column) {
		if ( column instanceof AggregateColumn ) {
			final Component component = ( (AggregateColumn) column ).getComponent();
			final CompositeType compositeType = component.getType();
			final NavigableRole embeddableRole = navigableRole.append( CollectionPart.Nature.ELEMENT.getName() );
			final EmbeddableMappingTypeImpl mappingType = EmbeddableMappingTypeImpl.from(
					component,
					compositeType,
					component.getColumnInsertability(),
					component.getColumnUpdateability(),
					inflightDescriptor -> new EmbeddableValuedModelPart() {
						@Override
						public EmbeddableMappingType getEmbeddableTypeDescriptor() {
							return inflightDescriptor;
						}

						@Override
						public SqlTuple toSqlExpression(
								TableGroup tableGroup,
								Clause clause,
								SqmToSqlAstConverter walker,
								SqlAstCreationState sqlAstCreationState) {
							return null;
						}

						@Override
						public String getContainingTableExpression() {
							return "";
						}

						@Override
						public SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup) {
							return null;
						}

						@Override
						public boolean isSimpleJoinPredicate(Predicate predicate) {
							return predicate == null;
						}

						@Override
						public TableGroupJoin createTableGroupJoin(
								NavigablePath navigablePath,
								TableGroup lhs,
								@Nullable String explicitSourceAlias,
								@Nullable SqlAliasBase explicitSqlAliasBase,
								@Nullable SqlAstJoinType sqlAstJoinType,
								boolean fetched,
								boolean addsPredicate,
								SqlAstCreationState creationState) {
							return null;
						}

						@Override
						public TableGroup createRootTableGroupJoin(
								NavigablePath navigablePath,
								TableGroup lhs,
								@Nullable String explicitSourceAlias,
								@Nullable SqlAliasBase explicitSqlAliasBase,
								@Nullable SqlAstJoinType sqlAstJoinType,
								boolean fetched,
								@Nullable Consumer<Predicate> predicateConsumer,
								SqlAstCreationState creationState) {
							return null;
						}

						@Override
						public String getSqlAliasStem() {
							return "";
						}

						@Override
						public String getFetchableName() {
							return CollectionPart.Nature.ELEMENT.getName();
						}

						@Override
						public int getFetchableKey() {
							return 0;
						}

						@Override
						public FetchOptions getMappedFetchOptions() {
							return null;
						}

						@Override
						public Fetch generateFetch(
								FetchParent fetchParent,
								NavigablePath fetchablePath,
								FetchTiming fetchTiming,
								boolean selected,
								String resultVariable,
								DomainResultCreationState creationState) {
							return null;
						}

						@Override
						public NavigableRole getNavigableRole() {
							return embeddableRole;
						}

						@Override
						public String getPartName() {
							return CollectionPart.Nature.ELEMENT.getName();
						}

						@Override
						public MappingType getPartMappingType() {
							return inflightDescriptor;
						}

						@Override
						public <T> DomainResult<T> createDomainResult(
								NavigablePath navigablePath,
								TableGroup tableGroup,
								String resultVariable,
								DomainResultCreationState creationState) {
							return null;
						}

						@Override
						public void applySqlSelections(
								NavigablePath navigablePath,
								TableGroup tableGroup,
								DomainResultCreationState creationState) {

						}

						@Override
						public void applySqlSelections(
								NavigablePath navigablePath,
								TableGroup tableGroup,
								DomainResultCreationState creationState,
								BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {

						}

						@Override
						public EntityMappingType findContainingEntityMapping() {
							return null;
						}
					},
					creationProcess
			);
			return (BasicType<?>) mappingType.getAggregateMapping().getJdbcMapping();
		}
		return null;
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

	/**
	 * For Hibernate Reactive
	 */
	public static ToOneAttributeMapping buildSingularAssociationAttributeMapping(
			String attrName,
			NavigableRole navigableRole,
			int stateArrayPosition,
			int fetchableIndex,
			Property bootProperty,
			ManagedMappingType declaringType,
			EntityPersister declaringEntityPersister,
			EntityType attrType,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess) {
		return buildSingularAssociationAttributeMapping(
				attrName,
				navigableRole,
				stateArrayPosition,
				fetchableIndex,
				bootProperty,
				declaringType,
				declaringEntityPersister,
				attrType,
				propertyAccess,
				cascadeStyle,
				creationProcess,
				Function.identity()
		);
	}

	public static ToOneAttributeMapping buildSingularAssociationAttributeMapping(
			String attrName,
			NavigableRole navigableRole,
			int stateArrayPosition,
			int fetchableIndex,
			Property bootProperty,
			ManagedMappingType declaringType,
			EntityPersister declaringEntityPersister,
			EntityType attrType,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess,
			Function<ToOneAttributeMapping, ToOneAttributeMapping> mappingConverter) {
		if ( bootProperty.getValue() instanceof ToOne ) {
			final ToOne value = (ToOne) bootProperty.getValue();
			final EntityPersister entityPersister = creationProcess.getEntityPersister( value.getReferencedEntityName() );
			final AttributeMetadata attributeMetadata = getAttributeMetadata(
					bootProperty,
					attrType,
					propertyAccess,
					cascadeStyle,
					creationProcess
			);
			final SessionFactoryImplementor sessionFactory = creationProcess.getCreationContext().getSessionFactory();

			final AssociationType type = (AssociationType) bootProperty.getType();
			final FetchStyle fetchStyle = FetchOptionsHelper
					.determineFetchStyleByMetadata(
							bootProperty.getValue().getFetchMode(),
							type,
							sessionFactory
					);

			final FetchTiming fetchTiming;
			final boolean lazy = value.isLazy();
			if ( lazy && entityPersister.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading() ) {
				if ( value.isUnwrapProxy() ) {
					fetchTiming = FetchOptionsHelper.determineFetchTiming( fetchStyle, type, lazy, sessionFactory );
				}
				else if ( value instanceof ManyToOne && value.isNullable() && ( (ManyToOne) value ).isIgnoreNotFound() ) {
					fetchTiming = FetchTiming.IMMEDIATE;
				}
				else {
					fetchTiming = FetchOptionsHelper.determineFetchTiming( fetchStyle, type, lazy, sessionFactory );
				}
			}
			else if ( !lazy
					|| value instanceof OneToOne && value.isNullable()
					|| value instanceof ManyToOne && value.isNullable() && ( (ManyToOne) value ).isIgnoreNotFound() ) {
				fetchTiming = FetchTiming.IMMEDIATE;
				if ( lazy ) {
					if ( MAPPING_MODEL_CREATION_MESSAGE_LOGGER.isDebugEnabled() ) {
						MAPPING_MODEL_CREATION_MESSAGE_LOGGER.debugf(
								"Forcing FetchTiming.IMMEDIATE for to-one association : %s.%s",
								declaringType.getNavigableRole(),
								bootProperty.getName()
						);
					}
				}
			}
			else {
				fetchTiming = FetchOptionsHelper.determineFetchTiming( fetchStyle, type, lazy, sessionFactory );
			}

			final ToOneAttributeMapping attributeMapping = mappingConverter.apply( new ToOneAttributeMapping(
					attrName,
					navigableRole,
					stateArrayPosition,
					fetchableIndex,
					(ToOne) bootProperty.getValue(),
					attributeMetadata,
					fetchTiming,
					fetchStyle,
					entityPersister,
					declaringType,
					declaringEntityPersister,
					propertyAccess
			) );

			creationProcess.registerForeignKeyPostInitCallbacks(
					"To-one key - " + navigableRole,
					() -> MappingModelCreationHelper.interpretToOneKeyDescriptor(
							attributeMapping,
							bootProperty,
							(ToOne) bootProperty.getValue(),
							null,
							creationProcess.getCreationContext().getDialect(),
							creationProcess
					)
			);
			return attributeMapping;
		}
		else {
			throw new UnsupportedOperationException( "AnyType support has not yet been implemented" );
		}
	}
}
