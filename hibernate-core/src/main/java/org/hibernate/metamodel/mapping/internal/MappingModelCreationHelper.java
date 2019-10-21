/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.Consumer;

import org.hibernate.LockMode;
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
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.CollectionIdentifierDescriptor;
import org.hibernate.metamodel.mapping.CollectionMappingType;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.ColumnConsumer;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadata;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.spi.DomainMetamodel;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.SQLLoadableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.walking.internal.FetchStrategyHelper;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAliasStemHelper;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.internal.domain.basic.BasicFetch;
import org.hibernate.sql.results.internal.domain.basic.BasicResult;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.type.BasicType;
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
	// EntityIdentifier

	public static BasicEntityIdentifierMapping buildSimpleIdentifierMapping(
			EntityPersister entityPersister,
			String rootTable,
			String pkColumnName,
			BasicType idType,
			MappingModelCreationProcess creationProcess) {
		assert entityPersister.hasIdentifierProperty();
		assert entityPersister.getIdentifierPropertyName() != null;

		final PersistentClass bootEntityDescriptor = creationProcess.getCreationContext()
				.getBootModel()
				.getEntityBinding( entityPersister.getEntityName() );

		final PropertyAccess propertyAccess = entityPersister.getRepresentationStrategy()
				.resolvePropertyAccess( bootEntityDescriptor.getIdentifierProperty() );

		return new BasicEntityIdentifierMapping() {
			@Override
			public PropertyAccess getPropertyAccess() {
				return propertyAccess;
			}

			@Override
			public MappingType getMappedTypeDescriptor() {
				return ( (BasicType) entityPersister.getIdentifierType() ).getMappedTypeDescriptor();
			}

			@Override
			public int getJdbcTypeCount(TypeConfiguration typeConfiguration) {
				return 1;
			}

			@Override
			public void visitColumns(ColumnConsumer consumer) {
				consumer.accept( getMappedColumnExpression(), getContainingTableExpression(), getJdbcMapping() );
			}

			@Override
			public void visitJdbcTypes(
					Consumer<JdbcMapping> action,
					Clause clause,
					TypeConfiguration typeConfiguration) {
				action.accept( idType );
			}

			@Override
			public void visitJdbcValues(
					Object value,
					Clause clause,
					JdbcValuesConsumer valuesConsumer,
					SharedSessionContractImplementor session) {
				valuesConsumer.consume( value, idType );
			}

			@Override
			public JavaTypeDescriptor getJavaTypeDescriptor() {
				return getMappedTypeDescriptor().getMappedJavaTypeDescriptor();
			}

			@Override
			public <T> DomainResult<T> createDomainResult(
					NavigablePath navigablePath,
					TableGroup tableGroup,
					String resultVariable,
					DomainResultCreationState creationState) {
				final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();
				final TableReference rootTableReference = tableGroup.resolveTableReference( rootTable );

				final Expression expression = expressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey( rootTableReference, pkColumnName ),
						sqlAstProcessingState -> new ColumnReference(
								rootTableReference.getIdentificationVariable(),
								pkColumnName,
								( (BasicValuedMapping) entityPersister.getIdentifierType() ).getJdbcMapping(),
								creationProcess.getCreationContext().getSessionFactory()
						)
				);

				final SqlSelection sqlSelection = expressionResolver.resolveSqlSelection(
						expression,
						idType.getExpressableJavaTypeDescriptor(),
						creationProcess.getCreationContext().getSessionFactory().getTypeConfiguration()
				);

				//noinspection unchecked
				return new BasicResult(
						sqlSelection.getValuesArrayPosition(),
						resultVariable,
						entityPersister.getIdentifierMapping().getMappedTypeDescriptor().getMappedJavaTypeDescriptor()
				);
			}

			@Override
			public void applySqlSelections(
					NavigablePath navigablePath,
					TableGroup tableGroup,
					DomainResultCreationState creationState) {
				final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();
				final TableReference rootTableReference = tableGroup.resolveTableReference( rootTable );

				final Expression expression = expressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey( rootTableReference, pkColumnName ),
						sqlAstProcessingState -> new ColumnReference(
								rootTable,
								pkColumnName,
								( (BasicValuedModelPart) entityPersister.getIdentifierType() ).getJdbcMapping(),
								creationProcess.getCreationContext().getSessionFactory()
						)
				);

				// the act of resolving the expression -> selection applies it
				expressionResolver.resolveSqlSelection(
						expression,
						idType.getExpressableJavaTypeDescriptor(),
						creationProcess.getCreationContext().getSessionFactory().getTypeConfiguration()
				);
			}

			@Override
			public String getContainingTableExpression() {
				return rootTable;
			}

			@Override
			public BasicValueConverter getConverter() {
				return null;
			}

			@Override
			public String getMappedColumnExpression() {
				return pkColumnName;
			}

			@Override
			public JdbcMapping getJdbcMapping() {
				return idType;
			}

			@Override
			public String getFetchableName() {
				return entityPersister.getIdentifierPropertyName();
			}

			@Override
			public FetchStrategy getMappedFetchStrategy() {
				return FetchStrategy.IMMEDIATE_JOIN;
			}

			@Override
			public Fetch generateFetch(
					FetchParent fetchParent,
					NavigablePath fetchablePath,
					FetchTiming fetchTiming,
					boolean selected,
					LockMode lockMode,
					String resultVariable,
					DomainResultCreationState creationState) {
				return new BasicFetch<>(
						0,
						fetchParent,
						fetchablePath,
						this,
						false,
						null,
						FetchTiming.IMMEDIATE,
						creationState
				);
			}
		};

	}

	public static EntityIdentifierMapping buildEncapsulatedCompositeIdentifierMapping(
			EntityPersister entityPersister,
			String rootTableName,
			String[] rootTableKeyColumnNames,
			CompositeType cidType,
			MappingModelCreationProcess creationProcess) {
		final PersistentClass bootEntityDescriptor = creationProcess.getCreationContext()
				.getBootModel()
				.getEntityBinding( entityPersister.getEntityName() );

		final PropertyAccess propertyAccess = entityPersister.getRepresentationStrategy()
				.resolvePropertyAccess( bootEntityDescriptor.getIdentifierProperty() );

		return new EntityIdentifierMapping() {
			@Override
			public PropertyAccess getPropertyAccess() {
				return propertyAccess;
			}

			@Override
			public MappingType getMappedTypeDescriptor() {
				return ( (BasicValuedModelPart) entityPersister.getIdentifierType() ).getMappedTypeDescriptor();
			}

			@Override
			public JavaTypeDescriptor getJavaTypeDescriptor() {
				return getMappedTypeDescriptor().getMappedJavaTypeDescriptor();
			}

			@Override
			public <T> DomainResult<T> createDomainResult(
					NavigablePath navigablePath,
					TableGroup tableGroup,
					String resultVariable,
					DomainResultCreationState creationState) {
				return ( (ModelPart) entityPersister.getIdentifierType() ).createDomainResult(
						navigablePath,
						tableGroup,
						resultVariable,
						creationState
				);
			}

			@Override
			public void applySqlSelections(
					NavigablePath navigablePath,
					TableGroup tableGroup,
					DomainResultCreationState creationState) {
				( (ModelPart) entityPersister.getIdentifierType() ).applySqlSelections(
						navigablePath,
						tableGroup,
						creationState
				);
			}
		};
	}

	public static EntityIdentifierMapping buildNonEncapsulatedCompositeIdentifierMapping(
			EntityPersister entityPersister,
			String rootTableName,
			String[] rootTableKeyColumnNames,
			CompositeType cidType,
			MappingModelCreationProcess creationProcess) {
		final PersistentClass bootEntityDescriptor = creationProcess.getCreationContext()
				.getBootModel()
				.getEntityBinding( entityPersister.getEntityName() );

		final PropertyAccess propertyAccess = entityPersister.getRepresentationStrategy()
				.resolvePropertyAccess( bootEntityDescriptor.getIdentifierProperty() );

		return new EntityIdentifierMapping() {

			@Override
			public PropertyAccess getPropertyAccess() {
				return propertyAccess;
			}

			@Override
			public MappingType getMappedTypeDescriptor() {
				return entityPersister;
			}

			@Override
			public JavaTypeDescriptor getJavaTypeDescriptor() {
				return getMappedTypeDescriptor().getMappedJavaTypeDescriptor();
			}

			@Override
			public <T> DomainResult<T> createDomainResult(
					NavigablePath navigablePath,
					TableGroup tableGroup,
					String resultVariable,
					DomainResultCreationState creationState) {
				return ( (ModelPart) entityPersister.getIdentifierType() ).createDomainResult(
						navigablePath,
						tableGroup,
						resultVariable,
						creationState
				);
			}

			@Override
			public void applySqlSelections(
					NavigablePath navigablePath,
					TableGroup tableGroup,
					DomainResultCreationState creationState) {
				( (ModelPart) entityPersister.getIdentifierType() ).applySqlSelections(
						navigablePath,
						tableGroup,
						creationState
				);
			}
		};
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Non-identifier attributes

	public static BasicValuedSingularAttributeMapping buildBasicAttributeMapping(
			String attrName,
			int stateArrayPosition,
			Property bootProperty,
			ManagedMappingType declaringType,
			BasicType attrType,
			String tableExpression,
			String attrColumnName,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess) {
		final BasicValue.Resolution<?> resolution = ( (BasicValue) bootProperty.getValue() ).resolve();
		final BasicValueConverter valueConverter = resolution.getValueConverter();

		final StateArrayContributorMetadataAccess attributeMetadataAccess = entityMappingType -> new StateArrayContributorMetadata() {
			private final MutabilityPlan mutabilityPlan = resolution.getMutabilityPlan();
			private final boolean nullable = bootProperty.getValue().isNullable();
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
			// we want to "decompose" the "type" into its various pieces as expected by the mapping
			assert valueConverter.getRelationalJavaDescriptor() == resolution.getRelationalJavaDescriptor();

			final BasicType<?> mappingBasicType = creationProcess.getCreationContext()
					.getDomainModel()
					.getTypeConfiguration()
					.getBasicTypeRegistry()
					.resolve( valueConverter.getRelationalJavaDescriptor(), resolution.getRelationalSqlTypeDescriptor() );


			return new BasicValuedSingularAttributeMapping(
					attrName,
					stateArrayPosition,
					attributeMetadataAccess,
					fetchStrategy,
					tableExpression,
					attrColumnName,
					valueConverter,
					mappingBasicType,
					mappingBasicType.getJdbcMapping(),
					declaringType,
					propertyAccess
			);
		}
		else {
			return new BasicValuedSingularAttributeMapping(
					attrName,
					stateArrayPosition,
					attributeMetadataAccess,
					fetchStrategy,
					tableExpression,
					attrColumnName,
					null,
					attrType,
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
			String[] attrColumnNames,
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

		final EmbeddableMappingType embeddableMappingType = EmbeddableMappingType.from(
				(Component) bootProperty.getValue(),
				attrType,
				attributeMappingType -> new EmbeddedAttributeMapping(
						attrName,
						stateArrayPosition,
						tableExpression,
						attrColumnNames,
						attributeMetadataAccess,
						FetchStrategy.IMMEDIATE_JOIN,
						attributeMappingType,
						declaringType,
						propertyAccess
				),
				creationProcess
		);

		return (EmbeddedAttributeMapping) embeddableMappingType.getEmbeddedValueMapping();
	}

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

	public static PluralAttributeMapping buildPluralAttributeMapping(
			String attrName,
			int stateArrayPosition,
			Property bootProperty,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess) {

		final Collection bootValueMapping = (Collection) bootProperty.getValue();

		final RuntimeModelCreationContext creationContext = creationProcess.getCreationContext();
		final SessionFactoryImplementor sessionFactory = creationContext.getSessionFactory();
		final Dialect dialect = sessionFactory.getJdbcServices().getJdbcEnvironment().getDialect();
		final DomainMetamodel domainModel = creationContext.getDomainModel();

		final CollectionPersister collectionDescriptor = domainModel.findCollectionDescriptor( bootValueMapping.getRole() );
		assert collectionDescriptor != null;

		final String tableExpression = ( (Joinable) collectionDescriptor ).getTableName();

		final String sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( bootProperty.getName() );

		final CollectionMappingType<?> collectionMappingType;
		final JavaTypeDescriptorRegistry jtdRegistry = creationContext.getJavaTypeDescriptorRegistry();

		final ForeignKeyDescriptor keyDescriptor = interpretKeyDescriptor(
				bootProperty,
				bootValueMapping,
				collectionDescriptor,
				declaringType,
				dialect,
				creationProcess
		);

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
				indexDescriptor = new BasicValuedCollectionPart(
						collectionDescriptor,
						CollectionPart.Nature.INDEX,
						creationContext.getTypeConfiguration().getBasicTypeForJavaType( Integer.class ),
						// no converter
						null,
						tableExpression,
						index.getColumnIterator().next().getText( dialect )
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
						identifierColumnName,
						(BasicType) loadableCollection.getIdentifierType()
				);

				break;
			}
			case LIST: {
				final BasicValue index = (BasicValue) ( (IndexedCollection) bootValueMapping ).getIndex();

				indexDescriptor = new BasicValuedCollectionPart(
						collectionDescriptor,
						CollectionPart.Nature.INDEX,
						creationContext.getTypeConfiguration().getBasicTypeForJavaType( Integer.class ),
						// no converter
						null,
						tableExpression,
						index.getColumnIterator().next().getText( dialect )
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
				( (OuterJoinLoadable) declaringType ).getFetchMode( stateArrayPosition ),
				collectionDescriptor.getCollectionType(),
				sessionFactory
		);

		return new PluralAttributeMappingImpl(
				attrName,
				bootValueMapping,
				propertyAccess,
				entityMappingType -> contributorMetadata,
				collectionMappingType,
				stateArrayPosition,
				keyDescriptor,
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
	}

	private static ForeignKeyDescriptor interpretKeyDescriptor(
			Property bootProperty,
			Collection bootValueMapping,
			CollectionPersister collectionDescriptor,
			ManagedMappingType declaringType,
			Dialect dialect,
			MappingModelCreationProcess creationProcess) {
		final Type keyType = bootValueMapping.getKey().getType();

		final ModelPart fkTarget;
		final String lhsPropertyName = collectionDescriptor.getCollectionType().getLHSPropertyName();
		if ( lhsPropertyName == null ) {
			fkTarget = collectionDescriptor.getOwnerEntityPersister().getIdentifierMapping();
		}
		else {
			fkTarget = declaringType.findAttributeMapping( lhsPropertyName );
		}

		if ( keyType instanceof BasicType ) {
			assert bootValueMapping.getKey().getColumnSpan() == 1;
			assert fkTarget instanceof BasicValuedModelPart;
			final BasicValuedModelPart simpleFkTarget = (BasicValuedModelPart) fkTarget;

			return new SimpleForeignKeyDescriptor(
					bootValueMapping.getKey().getTable().getName(),
					bootValueMapping.getKey().getColumnIterator().next().getText( dialect ),
					simpleFkTarget.getContainingTableExpression(),
					simpleFkTarget.getMappedColumnExpression(),
					(BasicType) keyType
			);
		}

		throw new NotYetImplementedFor6Exception(
				"Support for composite foreign-keys not yet implemented: " + bootValueMapping.getRole()
		);
	}

	private static ForeignKeyDescriptor interpretKeyDescriptor(
			Property bootProperty,
			ToOne bootValueMapping,
			EntityPersister referencedEntityDescriptor,
			Dialect dialect,
			MappingModelCreationProcess creationProcess) {

		final ModelPart fkTarget;
		if ( bootValueMapping.isReferenceToPrimaryKey() ) {
			fkTarget = referencedEntityDescriptor.getIdentifierMapping();
		}
		else {
			fkTarget = referencedEntityDescriptor.findSubPart( bootValueMapping.getReferencedPropertyName() );
		}

		final JdbcServices jdbcServices = creationProcess.getCreationContext().getSessionFactory().getJdbcServices();

		if ( fkTarget instanceof BasicValuedModelPart ) {
			final BasicValuedModelPart simpleFkTarget = (BasicValuedModelPart) fkTarget;

			return new SimpleForeignKeyDescriptor(
					creationProcess.getCreationContext()
							.getBootstrapContext()
							.getMetadataBuildingOptions()
							.getPhysicalNamingStrategy().toPhysicalTableName(
									bootValueMapping.getTable().getNameIdentifier(),
									jdbcServices.getJdbcEnvironment()
					).getText(),
					bootValueMapping.getColumnIterator().next().getText( dialect ),
					simpleFkTarget.getContainingTableExpression(),
					simpleFkTarget.getMappedColumnExpression(),
					simpleFkTarget.getJdbcMapping()
			);
		}

		throw new NotYetImplementedFor6Exception(
				"Support for composite foreign-keys not yet implemented: " +
						bootProperty.getPersistentClass().getEntityName() + " -> " + bootProperty.getName()
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
			return new BasicValuedCollectionPart(
					collectionDescriptor,
					CollectionPart.Nature.INDEX,
					basicValue.resolve().getResolvedBasicType(),
					basicValue.resolve().getValueConverter(),
					tableExpression,
					basicValue.getColumnIterator().next().getText( dialect )
			);
		}

		if ( bootMapKeyDescriptor instanceof Component ) {
			final Component component = (Component) bootMapKeyDescriptor;
			final CompositeType compositeType = (CompositeType) component.getType();

			final List<String> columnExpressions = CollectionHelper.arrayList( component.getColumnSpan() );
			final Iterator<Selectable> columnIterator = component.getColumnIterator();
			while ( columnIterator.hasNext() ) {
				columnExpressions.add( columnIterator.next().getText( dialect ) );
			}

			final EmbeddableMappingType mappingType = EmbeddableMappingType.from(
					component,
					compositeType,
					inflightDescriptor -> new EmbeddedCollectionPart(
							collectionDescriptor,
							CollectionPart.Nature.INDEX,
							inflightDescriptor,
							// parent-injection
							null,
							tableExpression,
							columnExpressions,
							sqlAliasStem
					),
					creationProcess
			);

			return (CollectionPart) mappingType.getEmbeddedValueMapping();
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
			return new BasicValuedCollectionPart(
					collectionDescriptor,
					CollectionPart.Nature.ELEMENT,
					basicElement.resolve().getResolvedBasicType(),
					basicElement.resolve().getValueConverter(),
					tableExpression,
					basicElement.getColumnIterator().next().getText( dialect )
			);
		}

		if ( element instanceof Component ) {
			final Component component = (Component) element;
			final CompositeType compositeType = (CompositeType) collectionDescriptor.getElementType();

			final List<String> columnExpressions = CollectionHelper.arrayList( component.getColumnSpan() );
			final Iterator<Selectable> columnIterator = component.getColumnIterator();
			while ( columnIterator.hasNext() ) {
				columnExpressions.add( columnIterator.next().getText( dialect ) );
			}

			final EmbeddableMappingType mappingType = EmbeddableMappingType.from(
					component,
					compositeType,
					embeddableMappingType -> new EmbeddedCollectionPart(
							collectionDescriptor,
							CollectionPart.Nature.ELEMENT,
							embeddableMappingType,
							// parent-injection
							null,
							tableExpression,
							columnExpressions,
							sqlAliasStem
					),
					creationProcess
			);

			return (CollectionPart) mappingType.getEmbeddedValueMapping();
		}

		if ( element instanceof OneToMany || element instanceof ToOne ) {
			final EntityPersister associatedEntity;

			if ( element instanceof OneToMany ) {
				associatedEntity = creationProcess.getEntityPersister(
						( (OneToMany) element ).getReferencedEntityName()
				);
			}
			else {
				// many-to-many
				associatedEntity = creationProcess.getEntityPersister(
						( (ToOne) element ).getReferencedEntityName()
				);
			}

			return new EntityCollectionPart( CollectionPart.Nature.ELEMENT, associatedEntity );
		}

		throw new NotYetImplementedFor6Exception(
				"Support for plural attributes with element type [" + element + "] not yet implemented"
		);
	}

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


	public static SingularAssociationAttributeMapping buildSingularAssociationAttributeMapping(
			String attrName,
			int stateArrayPosition,
			Property bootProperty,
			ManagedMappingType declaringType,
			EntityType attrType,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess) {
		ToOne value = (ToOne) bootProperty.getValue();
		final EntityPersister entityPersister = creationProcess.getEntityPersister(
				value.getReferencedEntityName() );

		final StateArrayContributorMetadataAccess stateArrayContributorMetadataAccess = getStateArrayContributorMetadataAccess(
				bootProperty,
				attrType,
				propertyAccess,
				cascadeStyle,
				creationProcess
		);
		final Dialect dialect = creationProcess.getCreationContext()
				.getSessionFactory()
				.getJdbcServices()
				.getJdbcEnvironment()
				.getDialect();

		final ForeignKeyDescriptor foreignKeyDescriptor = interpretKeyDescriptor(
				bootProperty,
				value,
				entityPersister,
				dialect,
				creationProcess
		);
		// todo (6.0) : determine the correct FetchStrategy
		return new SingularAssociationAttributeMapping(
				attrName,
				stateArrayPosition,
				foreignKeyDescriptor,
				stateArrayContributorMetadataAccess,
				FetchStrategy.IMMEDIATE_JOIN,
				entityPersister,
				declaringType,
				propertyAccess
		);
	}

}
