/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.BiConsumer;

import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.collection.internal.StandardArraySemantics;
import org.hibernate.collection.internal.StandardBagSemantics;
import org.hibernate.collection.internal.StandardIdentifierBagSemantics;
import org.hibernate.collection.internal.StandardListSemantics;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Resolvable;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.CollectionIdentifierDescriptor;
import org.hibernate.metamodel.mapping.CollectionMappingType;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.SelectionMapping;
import org.hibernate.metamodel.mapping.SelectionMappings;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NonTransientException;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadata;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.SQLLoadableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.walking.internal.FetchStrategyHelper;
import org.hibernate.property.access.internal.PropertyAccessStrategyMapImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.ast.spi.SqlAliasStemHelper;
import org.hibernate.type.AnyType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.BasicType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.spi.TypeConfiguration;

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

		final StateArrayContributorMetadataAccess attributeMetadataAccess = getStateArrayContributorMetadataAccess(
				propertyAccess
		);

		final EmbeddableMappingType embeddableMappingType = EmbeddableMappingType.from(
				(Component) bootProperty.getValue(),
				cidType,
				rootTableName,
				rootTableKeyColumnNames,
				embeddable -> new EmbeddedIdentifierMappingImpl(
						entityPersister,
						attributeName,
						embeddable,
						attributeMetadataAccess,
						propertyAccess,
						rootTableName,
						creationProcess.getCreationContext().getSessionFactory()
				),
				creationProcess
		);


		return (EmbeddedIdentifierMappingImpl) embeddableMappingType.getEmbeddedValueMapping();
	}

	public static CompositeIdentifierMapping buildNonEncapsulatedCompositeIdentifierMapping(
			EntityPersister entityPersister,
			String rootTableName,
			String[] rootTableKeyColumnNames,
			CompositeType cidType,
			PersistentClass bootEntityDescriptor,
			BiConsumer<String,SingularAttributeMapping> idSubAttributeConsumer,
			MappingModelCreationProcess creationProcess) {
		final Component bootIdClassComponent = (Component) bootEntityDescriptor.getIdentifier();

		final EmbeddableMappingType embeddableMappingType = EmbeddableMappingType.from(
				bootIdClassComponent,
				cidType,
				rootTableName,
				rootTableKeyColumnNames,
				attributeMappingType -> {
					final SessionFactoryImplementor sessionFactory = creationProcess.getCreationContext().getSessionFactory();
					final PropertyAccess propertyAccess = PropertyAccessStrategyMapImpl.INSTANCE.buildPropertyAccess(
							null,
							EntityIdentifierMapping.ROLE_LOCAL_NAME
					);
					final StateArrayContributorMetadataAccess attributeMetadataAccess = getStateArrayContributorMetadataAccess(
							propertyAccess
					);
					Component bootComponentDescriptor = bootEntityDescriptor.getIdentifierMapper();
					final List<SingularAttributeMapping> idAttributeMappings;
					final Iterator<Property> bootIdSubPropertyItr;
					if ( bootComponentDescriptor == null ) {
						idAttributeMappings = new ArrayList<>( bootIdClassComponent.getPropertySpan() );
						bootIdSubPropertyItr = bootIdClassComponent.getPropertyIterator();

					}
					else {
						idAttributeMappings = new ArrayList<>( bootComponentDescriptor.getPropertySpan() );
						bootIdSubPropertyItr = bootComponentDescriptor.getPropertyIterator();
					}

					int columnsConsumedSoFar = 0;

					while ( bootIdSubPropertyItr.hasNext() ) {
						final Property bootIdSubProperty = bootIdSubPropertyItr.next();
						final Type idSubPropertyType = bootIdSubProperty.getType();

						if ( idSubPropertyType instanceof AnyType ) {
							throw new HibernateException(
									"AnyType property `" + bootEntityDescriptor.getEntityName() + "#" + bootIdSubProperty.getName() +
											"` cannot be used as part of entity identifier "
							);
						}

						if ( idSubPropertyType instanceof CollectionType ) {
							throw new HibernateException(
									"Plural property `" + bootEntityDescriptor.getEntityName() + "#" + bootIdSubProperty.getName() +
											"` cannot be used as part of entity identifier "
							);
						}

						final SingularAttributeMapping idSubAttribute;

						if ( idSubPropertyType instanceof BasicType ) {
							//noinspection rawtypes
							idSubAttribute = buildBasicAttributeMapping(
									bootIdSubProperty.getName(),
									entityPersister.getNavigableRole().append( bootIdSubProperty.getName() ),
									idAttributeMappings.size(),
									bootIdSubProperty,
									attributeMappingType,
									(BasicType) idSubPropertyType,
									rootTableName,
									rootTableKeyColumnNames[columnsConsumedSoFar],
									false,
									null,
									null,
									entityPersister.getRepresentationStrategy().resolvePropertyAccess( bootIdSubProperty ),
									CascadeStyles.ALL,
									creationProcess
							);
							columnsConsumedSoFar++;
						}
						else if ( idSubPropertyType instanceof CompositeType ) {
							// nested composite
							throw new NotYetImplementedFor6Exception();
						}
						else if ( idSubPropertyType instanceof EntityType ) {
							// key-many-to-one
							final EntityType keyManyToOnePropertyType = (EntityType) idSubPropertyType;

							idSubAttribute = buildSingularAssociationAttributeMapping(
									bootIdSubProperty.getName(),
									entityPersister.getNavigableRole().append( EntityIdentifierMapping.ROLE_LOCAL_NAME ),
									idAttributeMappings.size(),
									bootIdSubProperty,
									attributeMappingType,
									keyManyToOnePropertyType,
									entityPersister.getRepresentationStrategy().resolvePropertyAccess( bootIdSubProperty ),
									CascadeStyles.ALL,
									creationProcess
							);

							columnsConsumedSoFar += keyManyToOnePropertyType.getColumnSpan( sessionFactory );
						}
						else {
							throw new UnsupportedOperationException();
						}

						idAttributeMappings.add( idSubAttribute );
						if ( bootComponentDescriptor == null ) {
							idSubAttributeConsumer.accept( idSubAttribute.getAttributeName(), idSubAttribute );
						}
					}

					return new NonAggregatedIdentifierMappingImpl(
							attributeMappingType,
							entityPersister,
							idAttributeMappings,
							attributeMetadataAccess,
							rootTableName,
							bootIdClassComponent,
							bootComponentDescriptor,
							creationProcess
					);
				},
				creationProcess
		);

		return (CompositeIdentifierMapping) embeddableMappingType.getEmbeddedValueMapping();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Non-identifier attributes

	@SuppressWarnings("rawtypes")
	public static BasicValuedSingularAttributeMapping buildBasicAttributeMapping(
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
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess) {
		final Value value = bootProperty.getValue();
		final BasicValue.Resolution<?> resolution = ( (Resolvable) value ).resolve();

		final BasicValueConverter valueConverter = resolution.getValueConverter();

		final StateArrayContributorMetadataAccess attributeMetadataAccess = entityMappingType -> new StateArrayContributorMetadata() {
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

		final FetchStrategy fetchStrategy = bootProperty.isLazy()
				? new FetchStrategy( FetchTiming.DELAYED, FetchStyle.SELECT )
				: FetchStrategy.IMMEDIATE_JOIN;

		if ( valueConverter != null ) {

			if ( isAttrFormula ) {
				throw new MappingException( String.format(
						"Value converter should not be set for column [%s] annotated with @Formula [%s]",
						attrName,
						attrColumnName
				) );
			}

			// we want to "decompose" the "type" into its various pieces as expected by the mapping
			assert valueConverter.getRelationalJavaDescriptor() == resolution.getRelationalJavaDescriptor();

			final BasicType<?> mappingBasicType = creationProcess.getCreationContext()
					.getDomainModel()
					.getTypeConfiguration()
					.getBasicTypeRegistry()
					.resolve(
							valueConverter.getRelationalJavaDescriptor(),
							resolution.getRelationalSqlTypeDescriptor()
					);


			return new BasicValuedSingularAttributeMapping(
					attrName,
					navigableRole,
					stateArrayPosition,
					attributeMetadataAccess,
					fetchStrategy,
					tableExpression,
					attrColumnName,
					false,
					null,
					null,
					valueConverter,
					mappingBasicType.getJdbcMapping(),
					declaringType,
					propertyAccess
			);
		}
		else {
			return new BasicValuedSingularAttributeMapping(
					attrName,
					navigableRole,
					stateArrayPosition,
					attributeMetadataAccess,
					fetchStrategy,
					tableExpression,
					attrColumnName,
					isAttrFormula,
					readExpr,
					writeExpr,
					null,
					attrType,
					declaringType,
					propertyAccess
			);
		}
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
		final StateArrayContributorMetadataAccess attributeMetadataAccess = getStateArrayContributorMetadataAccess(
				bootProperty,
				attrType,
				propertyAccess,
				cascadeStyle,
				creationProcess
		);

		final Component component = (Component) bootProperty.getValue();
		final EmbeddableMappingType embeddableMappingType = EmbeddableMappingType.from(
				component,
				attrType,
				tableExpression,
				rootTableKeyColumnNames,
				attributeMappingType -> new EmbeddedAttributeMapping(
						attrName,
						declaringType.getNavigableRole().append( attrName ),
						stateArrayPosition,
						tableExpression,
						attributeMetadataAccess,
						component.getParentProperty(),
						FetchStrategy.IMMEDIATE_JOIN,
						attributeMappingType,
						declaringType,
						propertyAccess
				),
				creationProcess
		);

		return (EmbeddedAttributeMapping) embeddableMappingType.getEmbeddedValueMapping();
	}

	@SuppressWarnings("rawtypes")
	protected static StateArrayContributorMetadataAccess getStateArrayContributorMetadataAccess(
			Property bootProperty,
			Type attrType,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess) {
		return entityMappingType -> new StateArrayContributorMetadata() {
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
						public Serializable disassemble(Object value) {
							throw new NotYetImplementedFor6Exception( getClass() );
						}

						@Override
						public Object assemble(Serializable cached) {
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
	protected static StateArrayContributorMetadataAccess getStateArrayContributorMetadataAccess(
			PropertyAccess propertyAccess) {
		return entityMappingType -> new StateArrayContributorMetadata() {

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
		final Dialect dialect = sessionFactory.getJdbcServices().getJdbcEnvironment().getDialect();
		final MappingMetamodel domainModel = creationContext.getDomainModel();

		final CollectionPersister collectionDescriptor = domainModel.findCollectionDescriptor( bootValueMapping.getRole() );
		assert collectionDescriptor != null;

		final String tableExpression = ( (Joinable) collectionDescriptor ).getTableName();

		final String sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( bootProperty.getName() );

		final CollectionMappingType<?> collectionMappingType;
		final JavaTypeDescriptorRegistry jtdRegistry = creationContext.getJavaTypeDescriptorRegistry();

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

		final CollectionSemantics collectionSemantics = collectionDescriptor.getCollectionSemantics();
		switch ( collectionSemantics.getCollectionClassification() ) {
			case ARRAY: {
				collectionMappingType = new CollectionMappingTypeImpl(
						jtdRegistry.getDescriptor( Object[].class ),
						StandardArraySemantics.INSTANCE
				);

				final BasicValue index = (BasicValue) ( (IndexedCollection) bootValueMapping ).getIndex();
				final SelectionMapping selectionMapping = SelectionMappingImpl.from(
						tableExpression,
						index.getColumnIterator().next(),
						creationContext.getTypeConfiguration().getBasicTypeForJavaType( Integer.class ),
						dialect,
						creationProcess.getSqmFunctionRegistry()
				);
				indexDescriptor = new BasicValuedCollectionPart(
						collectionDescriptor,
						CollectionPart.Nature.INDEX,
						// no converter
						null,
						selectionMapping
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
			case IDBAG: {
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
				final SelectionMapping selectionMapping = SelectionMappingImpl.from(
						tableExpression,
						index.getColumnIterator().next(),
						creationContext.getTypeConfiguration().getBasicTypeForJavaType( Integer.class ),
						dialect,
						creationProcess.getSqmFunctionRegistry()
				);
				indexDescriptor = new BasicValuedCollectionPart(
						collectionDescriptor,
						CollectionPart.Nature.INDEX,
						// no converter
						null,
						selectionMapping
				);

				collectionMappingType = new CollectionMappingTypeImpl(
						jtdRegistry.getDescriptor( java.util.List.class ),
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

				indexDescriptor = interpretMapKey(
						bootValueMapping,
						collectionDescriptor,
						tableExpression,
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

		final StateArrayContributorMetadata contributorMetadata = new StateArrayContributorMetadata() {
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
		};

		final FetchStyle style = FetchStrategyHelper.determineFetchStyleByMetadata(
				fetchMode,
				collectionDescriptor.getCollectionType(),
				sessionFactory
		);

		final PluralAttributeMappingImpl pluralAttributeMapping = new PluralAttributeMappingImpl(
				attrName,
				bootValueMapping,
				propertyAccess,
				entityMappingType -> contributorMetadata,
				collectionMappingType,
				stateArrayPosition,
				elementDescriptor,
				indexDescriptor,
				identifierDescriptor,
				new FetchStrategy(
						FetchStrategyHelper.determineFetchTiming(
								style,
								collectionDescriptor.getCollectionType(),
								sessionFactory
						),
						style
				),
				cascadeStyle,
				declaringType,
				collectionDescriptor
		);

		creationProcess.registerInitializationCallback(
				"PluralAttributeMapping(" + bootValueMapping.getRole() + ")#finishInitialization",
				() -> {
					try {
						pluralAttributeMapping.finishInitialization( bootProperty, bootValueMapping, creationProcess );
						return true;
					}
					catch (NotYetImplementedFor6Exception nye) {
						throw nye;
					}
					catch (Exception e) {
						if ( e instanceof NonTransientException ) {
							throw e;
						}

						return false;
					}
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
			attributeMappingSubPart = attributeMapping.findSubPart( collectionDescriptor.getMappedByProperty(), null );
		}

		if ( attributeMappingSubPart instanceof ToOneAttributeMapping ) {
			final ToOneAttributeMapping referencedAttributeMapping = (ToOneAttributeMapping) attributeMappingSubPart;

			setRefererencedAttributeForeignKeyDescriptor(
					attributeMapping,
					referencedAttributeMapping,
					(EntityPersister) referencedAttributeMapping.getDeclaringType(),
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
		if ( lhsPropertyName == null ) {
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
			final SelectionMapping keySelectionMapping = SelectionMappingImpl.from(
					tableExpression,
					bootValueMappingKey.getColumnIterator().next(),
					(JdbcMapping) keyType,
					dialect,
					creationProcess.getSqmFunctionRegistry()
			);
			attributeMapping.setForeignKeyDescriptor(
					new SimpleForeignKeyDescriptor(
							keySelectionMapping,
							simpleFkTarget
					)
			);
		}
		else if ( fkTarget instanceof EmbeddableValuedModelPart ) {
			final EmbeddedForeignKeyDescriptor embeddedForeignKeyDescriptor =
					buildTargetingEmbeddableForeignKeyDescriptor(
							(EmbeddableValuedModelPart) fkTarget,
							bootValueMapping,
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

	public static void interpretSingularAssociationAttributeMappingKeyDescriptor(
			ToOneAttributeMapping attributeMapping,
			Property bootProperty,
			ToOne bootValueMapping,
			EntityMappingType declaringEntityDescriptor,
			Dialect dialect,
			MappingModelCreationProcess creationProcess) {
		if ( attributeMapping.getForeignKeyDescriptor() != null ) {
			return;
		}

		final String tableName = getTableIdentifierExpression( bootValueMapping.getTable(), creationProcess );
		attributeMapping.setIdentifyingColumnsTableExpression( tableName );

		final EntityPersister referencedEntityDescriptor = creationProcess
				.getEntityPersister( bootValueMapping.getReferencedEntityName() );

		String referencedPropertyName = bootValueMapping.getReferencedPropertyName();
		if ( referencedPropertyName == null && bootValueMapping instanceof OneToOne ) {
			referencedPropertyName = ( (OneToOne) bootValueMapping ).getMappedByProperty();
		}

		if ( referencedPropertyName != null  ) {
			final ModelPart modelPart = referencedEntityDescriptor.findSubPart( referencedPropertyName );
			if ( modelPart instanceof ToOneAttributeMapping ) {
				setRefererencedAttributeForeignKeyDescriptor(
						attributeMapping,
						(ToOneAttributeMapping) modelPart,
						referencedEntityDescriptor,
						referencedPropertyName,
						dialect,
						creationProcess
				);
			}
			else if ( modelPart instanceof EmbeddableValuedModelPart ) {
				final EmbeddedForeignKeyDescriptor embeddedForeignKeyDescriptor = buildForeignKeyReferringEmbeddableDescriptor(
						(EmbeddableValuedModelPart) modelPart,
						bootValueMapping,
						dialect,
						creationProcess
				);
				attributeMapping.setForeignKeyDescriptor( embeddedForeignKeyDescriptor );
			}
			else {
				throw new NotYetImplementedFor6Exception(
						"Support for composite foreign-keys not yet implemented: " +
								bootProperty.getPersistentClass().getEntityName() + " -> " + bootProperty.getName()
				);
			}
			return;
		}

		final ModelPart fkTarget;
		if ( bootValueMapping.isReferenceToPrimaryKey() ) {
			fkTarget = referencedEntityDescriptor.getIdentifierMapping();
		}
		else {
			fkTarget = declaringEntityDescriptor.getIdentifierMapping();
		}

		if ( fkTarget instanceof BasicValuedModelPart ) {
			final BasicValuedModelPart simpleFkTarget = (BasicValuedModelPart) fkTarget;
			final Iterator<Selectable> columnIterator = bootValueMapping.getColumnIterator();
			final Table table = bootValueMapping.getTable();
			final String tableExpression = getTableIdentifierExpression( table, creationProcess );
			final SelectionMapping keySelectionMapping;
			if ( columnIterator.hasNext() ) {
				keySelectionMapping = SelectionMappingImpl.from(
						tableExpression,
						columnIterator.next(),
						simpleFkTarget.getJdbcMapping(),
						dialect,
						creationProcess.getSqmFunctionRegistry()
				);
			}
			else {
				// case of ToOne with @PrimaryKeyJoinColumn
				keySelectionMapping = SelectionMappingImpl.from(
						tableExpression,
						table.getColumn( 0 ),
						simpleFkTarget.getJdbcMapping(),
						dialect,
						creationProcess.getSqmFunctionRegistry()
				);
			}

			final ForeignKeyDescriptor foreignKeyDescriptor = new SimpleForeignKeyDescriptor(
					keySelectionMapping,
					simpleFkTarget
			);
			attributeMapping.setForeignKeyDescriptor( foreignKeyDescriptor );
		}
		else if ( fkTarget instanceof EmbeddableValuedModelPart ) {
			final EmbeddedForeignKeyDescriptor embeddedForeignKeyDescriptor = buildTargetingEmbeddableForeignKeyDescriptor(
					(EmbeddableValuedModelPart) fkTarget,
					bootValueMapping,
					dialect,
					creationProcess
			);
			attributeMapping.setForeignKeyDescriptor( embeddedForeignKeyDescriptor );
		}
		else {
			throw new NotYetImplementedFor6Exception(
					"Support for" + fkTarget.getClass() + " foreign-keys not yet implemented: " +
							bootProperty.getPersistentClass().getEntityName() + " -> " + bootProperty.getName()
			);
		}
	}

	public static EmbeddedForeignKeyDescriptor buildForeignKeyReferringEmbeddableDescriptor(
			EmbeddableValuedModelPart embeddableValuedModelPart,
			Value bootValueMapping,
			Dialect dialect,
			MappingModelCreationProcess creationProcess) {
		final SelectionMappings targetSelectionMappings;
		final String targetTableExpression;
		if ( bootValueMapping instanceof Collection ) {
			final Collection collectionBootValueMapping = (Collection) bootValueMapping;
			targetTableExpression = getTableIdentifierExpression(
					collectionBootValueMapping.getCollectionTable(),
					creationProcess
			);
			targetSelectionMappings = SelectionMappingsImpl.from(
					targetTableExpression,
					collectionBootValueMapping.getKey(),
					creationProcess.getCreationContext().getSessionFactory(),
					dialect,
					creationProcess.getSqmFunctionRegistry()
			);
		}
		else {
			targetTableExpression = getTableIdentifierExpression(
					bootValueMapping.getTable(),
					creationProcess
			);
			targetSelectionMappings = SelectionMappingsImpl.from(
					targetTableExpression,
					bootValueMapping,
					creationProcess.getCreationContext().getSessionFactory(),
					dialect,
					creationProcess.getSqmFunctionRegistry()
			);
		}
		return new EmbeddedForeignKeyDescriptor(
				embeddableValuedModelPart,
				embeddableValuedModelPart.getContainingTableExpression(),
				embeddableValuedModelPart.getEmbeddableTypeDescriptor(),
				targetTableExpression,
				targetSelectionMappings,
				creationProcess
		);
	}


	public static EmbeddedForeignKeyDescriptor buildTargetingEmbeddableForeignKeyDescriptor(
			EmbeddableValuedModelPart embeddableValuedModelPart,
			Value bootValueMapping,
			Dialect dialect,
			MappingModelCreationProcess creationProcess) {
		final SelectionMappings keySelectionMappings;
		final String keyTableExpression;
		if ( bootValueMapping instanceof Collection ) {
			final Collection collectionBootValueMapping = (Collection) bootValueMapping;
			keyTableExpression = getTableIdentifierExpression(
					collectionBootValueMapping.getCollectionTable(),
					creationProcess
			);
			keySelectionMappings = SelectionMappingsImpl.from(
					keyTableExpression,
					collectionBootValueMapping.getKey(),
					creationProcess.getCreationContext().getSessionFactory(),
					dialect,
					creationProcess.getSqmFunctionRegistry()
			);
		}
		else {
			keyTableExpression = getTableIdentifierExpression(
					bootValueMapping.getTable(),
					creationProcess
			);
			keySelectionMappings = SelectionMappingsImpl.from(
					keyTableExpression,
					bootValueMapping,
					creationProcess.getCreationContext().getSessionFactory(),
					dialect,
					creationProcess.getSqmFunctionRegistry()
			);
		}
		return new EmbeddedForeignKeyDescriptor(
				embeddableValuedModelPart,
				keyTableExpression,
				keySelectionMappings,
				embeddableValuedModelPart.getContainingTableExpression(),
				embeddableValuedModelPart.getEmbeddableTypeDescriptor(),
				creationProcess
		);
	}

	private static void setRefererencedAttributeForeignKeyDescriptor(
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
			Property property = entityBinding.getProperty( referencedPropertyName );
			interpretSingularAssociationAttributeMappingKeyDescriptor(
					referencedAttributeMapping,
					property,
					(ToOne) property.getValue(),
					referencedEntityDescriptor,
					dialect,
					creationProcess
			);
			attributeMapping.setForeignKeyDescriptor( referencedAttributeMapping.getForeignKeyDescriptor() );
		}
		else {
			attributeMapping.setForeignKeyDescriptor( foreignKeyDescriptor );
		}
	}

	private static String getTableIdentifierExpression(Table table, MappingModelCreationProcess creationProcess) {
		final JdbcEnvironment jdbcEnvironment = creationProcess.getCreationContext()
				.getMetadata()
				.getDatabase()
				.getJdbcEnvironment();
		return jdbcEnvironment
				.getQualifiedObjectNameFormatter().format(
						table.getQualifiedTableName(),
						jdbcEnvironment.getDialect()
				);
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
			final SelectionMapping selectionMapping = SelectionMappingImpl.from(
					tableExpression,
					basicValue.getColumnIterator().next(),
					basicValue.resolve().getJdbcMapping(),
					dialect,
					creationProcess.getSqmFunctionRegistry()
			);
			return new BasicValuedCollectionPart(
					collectionDescriptor,
					CollectionPart.Nature.INDEX,
					basicValue.resolve().getValueConverter(),
					selectionMapping
			);
		}

		if ( bootMapKeyDescriptor instanceof Component ) {
			final Component component = (Component) bootMapKeyDescriptor;
			final CompositeType compositeType = (CompositeType) component.getType();


			final EmbeddableMappingType mappingType = EmbeddableMappingType.from(
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
					associatedEntity,
					creationProcess
			);

			creationProcess.registerInitializationCallback(
					"PluralAttributeMapping( " + bootValueMapping.getRole() + ") - index descriptor",
					() -> {
						try {
							indexDescriptor.finishInitialization(
									collectionDescriptor,
									bootValueMapping,
									indexEntityType.getRHSUniqueKeyPropertyName(),
									creationProcess
							);

							return true;
						}
						catch (NotYetImplementedFor6Exception nye) {
							throw nye;
						}
						catch (Exception wait) {
							return false;
						}
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
			final SelectionMapping selectionMapping = SelectionMappingImpl.from(
					tableExpression,
					basicElement.getColumnIterator().next(),
					basicElement.resolve().getJdbcMapping(),
					dialect,
					creationProcess.getSqmFunctionRegistry()
			);
			return new BasicValuedCollectionPart(
					collectionDescriptor,
					CollectionPart.Nature.ELEMENT,
					basicElement.resolve().getValueConverter(),
					selectionMapping
			);
		}

		if ( element instanceof Component ) {
			final Component component = (Component) element;
			final CompositeType compositeType = (CompositeType) collectionDescriptor.getElementType();


			final EmbeddableMappingType mappingType = EmbeddableMappingType.from(
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
			final JavaTypeDescriptorRegistry jtdRegistry = typeConfiguration.getJavaTypeDescriptorRegistry();
			final JavaTypeDescriptor<Object> baseJtd = jtdRegistry.getDescriptor(Object.class);

			return new DiscriminatedCollectionPart(
					CollectionPart.Nature.ELEMENT,
					collectionDescriptor.getNavigableRole(),
					baseJtd,
					anyBootMapping,
					anyBootMapping.getType(),
					creationProcess
			);
		}

		if ( element instanceof OneToMany || element instanceof ToOne ) {
			final EntityType elementEntityType = (EntityType) collectionDescriptor.getElementType();
			final EntityPersister associatedEntity = creationProcess.getEntityPersister( elementEntityType.getAssociatedEntityName() );

			final EntityCollectionPart elementDescriptor = new EntityCollectionPart(
					collectionDescriptor,
					CollectionPart.Nature.ELEMENT,
					bootDescriptor.getElement(),
					associatedEntity,
					creationProcess
			);

			creationProcess.registerInitializationCallback(
					"EntityCollectionPart( " + elementDescriptor.getNavigableRole() + ")#finishInitialization",
					() -> {
						try {
							elementDescriptor.finishInitialization(
									collectionDescriptor,
									bootDescriptor,
									elementEntityType.getRHSUniqueKeyPropertyName(),
									creationProcess
							);

							return true;
						}
						catch (NotYetImplementedFor6Exception nye) {
							throw nye;
						}
						catch (Exception wait) {
							return false;
						}
					}
			);

			return elementDescriptor;
		}

		throw new NotYetImplementedFor6Exception(
				"Support for plural attributes with element type [" + element + "] not yet implemented"
		);
	}

	@SuppressWarnings("rawtypes")
	private static class CollectionMappingTypeImpl implements CollectionMappingType {
		private final JavaTypeDescriptor collectionJtd;
		private final CollectionSemantics semantics;

		@SuppressWarnings("WeakerAccess")
		public CollectionMappingTypeImpl(
				JavaTypeDescriptor collectionJtd,
				CollectionSemantics semantics) {
			this.collectionJtd = collectionJtd;
			this.semantics = semantics;
		}

		@Override
		public CollectionSemantics getCollectionSemantics() {
			return semantics;
		}

		@Override
		public JavaTypeDescriptor getMappedJavaTypeDescriptor() {
			return collectionJtd;
		}
	}

	public static ToOneAttributeMapping buildSingularAssociationAttributeMapping(
			String attrName,
			NavigableRole navigableRole,
			int stateArrayPosition,
			Property bootProperty,
			ManagedMappingType declaringType,
			EntityType attrType,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess) {
		if ( bootProperty.getValue() instanceof ToOne ) {
			final ToOne value = (ToOne) bootProperty.getValue();
			final EntityPersister entityPersister = creationProcess.getEntityPersister( value.getReferencedEntityName() );
			final StateArrayContributorMetadataAccess stateArrayContributorMetadataAccess = getStateArrayContributorMetadataAccess(
					bootProperty,
					attrType,
					propertyAccess,
					cascadeStyle,
					creationProcess
			);
			SessionFactoryImplementor sessionFactory = creationProcess.getCreationContext().getSessionFactory();

			final AssociationType type = (AssociationType) bootProperty.getType();
			final FetchStyle fetchStyle = FetchStrategyHelper
					.determineFetchStyleByMetadata(
							bootProperty.getValue().getFetchMode(),
							type,
							sessionFactory
					);

			final FetchTiming fetchTiming;

			if ( fetchStyle == FetchStyle.JOIN
					|| ( value instanceof OneToOne && value.isNullable() )
					|| !( value ).isLazy() ) {
				fetchTiming = FetchTiming.IMMEDIATE;
			}
			else {
				fetchTiming = FetchStrategyHelper.determineFetchTiming( fetchStyle, type, sessionFactory );
			}

			final FetchStrategy fetchStrategy = new FetchStrategy( fetchTiming, fetchStyle );

			final ToOneAttributeMapping attributeMapping = new ToOneAttributeMapping(
					attrName,
					navigableRole,
					stateArrayPosition,
					(ToOne) bootProperty.getValue(),
					stateArrayContributorMetadataAccess,
					fetchStrategy,
					entityPersister,
					declaringType,
					propertyAccess
			);

			creationProcess.registerForeignKeyPostInitCallbacks(
					"To-one key - " + navigableRole,
					() -> {
						final Dialect dialect = creationProcess.getCreationContext()
								.getSessionFactory()
								.getJdbcServices()
								.getDialect();

						MappingModelCreationHelper.interpretSingularAssociationAttributeMappingKeyDescriptor(
								attributeMapping,
								bootProperty,
								(ToOne) bootProperty.getValue(),
								declaringType.findContainingEntityMapping(),
								dialect,
								creationProcess
						);
						return true;
					}
			);
			return attributeMapping;
		}
		else {
			throw new NotYetImplementedFor6Exception( "AnyType support has not yet been implemented" );
		}
	}
}
