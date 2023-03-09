/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.io.Serializable;
import java.util.Locale;
import java.util.function.Function;

import org.hibernate.MappingException;
import org.hibernate.SharedSessionContract;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.internal.MutableAttributeMappingList;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableResultImpl;
import org.hibernate.type.AnyType;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.spi.CompositeTypeImplementor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Describes a "normal" embeddable.
 *
 * @apiNote At the moment, this class is also used to describe some non-"normal" things:
 *          mainly composite foreign keys.
 */
public class EmbeddableMappingTypeImpl extends AbstractEmbeddableMapping implements SelectableMappings {
	public static EmbeddableMappingTypeImpl from(
			Component bootDescriptor,
			CompositeType compositeType,
			boolean[] insertability,
			boolean[] updateability,
			Function<EmbeddableMappingType, EmbeddableValuedModelPart> embeddedPartBuilder,
			MappingModelCreationProcess creationProcess) {
		return from(
				bootDescriptor,
				compositeType,
				null,
				null,
				null,
				null,
				0,
				insertability,
				updateability,
				embeddedPartBuilder,
				creationProcess
		);
	}

	public static EmbeddableMappingTypeImpl from(
			Component bootDescriptor,
			CompositeType compositeType,
			String rootTableExpression,
			String[] rootTableKeyColumnNames,
			Property componentProperty,
			DependantValue dependantValue,
			int dependantColumnIndex,
			boolean[] insertability,
			boolean[] updateability,
			Function<EmbeddableMappingType,EmbeddableValuedModelPart> embeddedPartBuilder,
			MappingModelCreationProcess creationProcess) {
		final RuntimeModelCreationContext creationContext = creationProcess.getCreationContext();

		final EmbeddableMappingTypeImpl mappingType = new EmbeddableMappingTypeImpl(
				bootDescriptor,
				componentProperty,
				embeddedPartBuilder,
				creationContext
		);

		if ( compositeType instanceof CompositeTypeImplementor ) {
			( (CompositeTypeImplementor) compositeType ).injectMappingModelPart( mappingType.getEmbeddedValueMapping(), creationProcess );
		}

		creationProcess.registerInitializationCallback(
				"EmbeddableMappingType(" + mappingType.getNavigableRole().getFullPath() + ")#finishInitialization",
				() ->
						mappingType.finishInitialization(
								bootDescriptor,
								compositeType,
								rootTableExpression,
								rootTableKeyColumnNames,
								dependantValue,
								dependantColumnIndex,
								insertability,
								updateability,
								creationProcess
						)
		);

		return mappingType;
	}

	private final JavaType<?> embeddableJtd;
	private final EmbeddableRepresentationStrategy representationStrategy;

	private final EmbeddableValuedModelPart valueMapping;

	private final boolean createEmptyCompositesEnabled;
	private final SelectableMapping aggregateMapping;
	private final boolean aggregateMappingRequiresColumnWriter;
	private final boolean preferSelectAggregateMapping;
	private final boolean preferBindAggregateMapping;

	private EmbeddableMappingTypeImpl(
			Component bootDescriptor,
			Property componentProperty,
			Function<EmbeddableMappingType, EmbeddableValuedModelPart> embeddedPartBuilder,
			RuntimeModelCreationContext creationContext) {
		super( new MutableAttributeMappingList( 5 ) );
		this.representationStrategy = creationContext
				.getBootstrapContext()
				.getRepresentationStrategySelector()
				.resolveStrategy( bootDescriptor, () -> this, creationContext );

		this.embeddableJtd = representationStrategy.getMappedJavaType();
		this.valueMapping = embeddedPartBuilder.apply( this );

		this.createEmptyCompositesEnabled = ConfigurationHelper.getBoolean(
				Environment.CREATE_EMPTY_COMPOSITES_ENABLED,
				creationContext.getServiceRegistry().getService( ConfigurationService.class ).getSettings(),
				false
		);
		final AggregateColumn aggregateColumn = bootDescriptor.getAggregateColumn();
		if ( aggregateColumn != null ) {
			final Dialect dialect = creationContext.getDialect();
			final boolean insertable;
			final boolean updatable;
			if ( componentProperty == null ) {
				insertable = true;
				updatable = true;
			}
			else {
				insertable = componentProperty.isInsertable();
				updatable = componentProperty.isUpdateable();
			}
			this.aggregateMapping = SelectableMappingImpl.from(
					bootDescriptor.getOwner().getTable().getName(),
					aggregateColumn,
					bootDescriptor.getParentAggregateColumn() != null
							? bootDescriptor.getParentAggregateColumn().getSelectablePath()
							: null,
					resolveJdbcMapping( bootDescriptor, creationContext ),
					creationContext.getTypeConfiguration(),
					insertable,
					updatable,
					false,
					dialect,
					null
			);
			final AggregateSupport aggregateSupport = dialect.getAggregateSupport();
			final int sqlTypeCode = aggregateColumn.getSqlTypeCode();
			this.aggregateMappingRequiresColumnWriter = aggregateSupport
					.requiresAggregateCustomWriteExpressionRenderer( sqlTypeCode );
			this.preferSelectAggregateMapping = aggregateSupport.preferSelectAggregateMapping( sqlTypeCode );
			this.preferBindAggregateMapping = aggregateSupport.preferBindAggregateMapping( sqlTypeCode );
		}
		else {
			this.aggregateMapping = null;
			this.aggregateMappingRequiresColumnWriter = false;
			this.preferSelectAggregateMapping = false;
			this.preferBindAggregateMapping = false;
		}
	}

	private JdbcMapping resolveJdbcMapping(Component bootDescriptor, RuntimeModelCreationContext creationContext) {
		// The following is a bit "hacky" because ideally, this should happen in InferredBasicValueResolver#from,
		// but since we don't have access to the EmbeddableMappingType there yet, we do it here.
		// A possible alternative design would be to change AggregateJdbcType#resolveAggregateDescriptor
		// to accept a CompositeType instead of EmbeddableMappingType, and I even tried that,
		// but it doesn't work out unfortunately, because the type would have to be created too early,
		// when the values of the component properties aren't fully initialized yet.
		// Both designs would do this as part of the "finishInitialization" phase,
		// so there is IMO no real win to do it differently
		final TypeConfiguration typeConfiguration = creationContext.getTypeConfiguration();
		final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		final Column aggregateColumn = bootDescriptor.getAggregateColumn();
		final BasicType<?> basicType = basicTypeRegistry.resolve(
				getMappedJavaType(),
				typeConfiguration.getJdbcTypeRegistry().resolveAggregateDescriptor(
						aggregateColumn.getSqlTypeCode(),
						aggregateColumn.getSqlTypeCode() == SqlTypes.STRUCT
								? aggregateColumn.getSqlType()
								: null,
						this,
						creationContext
				)
		);
		// Register the resolved type under its struct name and java class name
		if ( bootDescriptor.getStructName() != null ) {
			basicTypeRegistry.register( basicType, bootDescriptor.getStructName() );
			basicTypeRegistry.register( basicType, getMappedJavaType().getJavaTypeClass().getName() );
		}
		return basicType;
	}

	public EmbeddableMappingTypeImpl(
			EmbeddedAttributeMapping valueMapping,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			EmbeddableMappingType inverseMappingType,
			MappingModelCreationProcess creationProcess) {
		super( new MutableAttributeMappingList( 5 ) );

		this.embeddableJtd = inverseMappingType.getJavaType();
		this.representationStrategy = inverseMappingType.getRepresentationStrategy();
		this.valueMapping = valueMapping;
		this.createEmptyCompositesEnabled = inverseMappingType.isCreateEmptyCompositesEnabled();
		this.aggregateMapping = null;
		this.aggregateMappingRequiresColumnWriter = false;
		this.preferSelectAggregateMapping = false;
		this.preferBindAggregateMapping = false;
		this.selectableMappings = selectableMappings;
		creationProcess.registerInitializationCallback(
				"EmbeddableMappingType(" + inverseMappingType.getNavigableRole().getFullPath() + ".{inverse})#finishInitialization",
				() -> inverseInitializeCallback(
						declaringTableGroupProducer,
						selectableMappings,
						inverseMappingType,
						creationProcess,
						valueMapping.getDeclaringType(),
						attributeMappings
				)
		);
	}

	public EmbeddableMappingType createInverseMappingType(
			EmbeddedAttributeMapping valueMapping,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			MappingModelCreationProcess creationProcess) {
		return new EmbeddableMappingTypeImpl(
				valueMapping,
				declaringTableGroupProducer,
				selectableMappings,
				this,
				creationProcess
		);
	}

	private boolean finishInitialization(
			Component bootDescriptor,
			CompositeType compositeType,
			String rootTableExpression,
			String[] rootTableKeyColumnNames,
			DependantValue dependantValue,
			int dependantColumnIndex,
			boolean[] insertability,
			boolean[] updateability,
			MappingModelCreationProcess creationProcess) {
// for some reason I cannot get this to work, though only a single test fails - `CompositeElementTest`
//		return finishInitialization(
//				getNavigableRole(),
//				bootDescriptor,
//				compositeType,
//				rootTableExpression,
//				rootTableKeyColumnNames,
//				this,
//				representationStrategy,
//				(name, type) -> {},
//				(column, jdbcEnvironment) -> getTableIdentifierExpression(
//						column.getValue().getTable(),
//						jdbcEnvironment
//				),
//				this::addAttribute,
//				() -> {
//					// We need the attribute mapping types to finish initialization first before we can build the column mappings
//					creationProcess.registerInitializationCallback(
//							"EmbeddableMappingType(" + getEmbeddedValueMapping().getNavigableRole().getFullPath() + ")#initColumnMappings",
//							this::initColumnMappings
//					);
//				},
//				creationProcess
//		);
// todo (6.0) - get this ^^ to work, or drop the comment

		final TypeConfiguration typeConfiguration = creationProcess.getCreationContext().getTypeConfiguration();
		final JdbcServices jdbcServices = creationProcess.getCreationContext().getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final Dialect dialect = jdbcEnvironment.getDialect();

		final String baseTableExpression = valueMapping.getContainingTableExpression();
		final Type[] subtypes = compositeType.getSubtypes();

		int attributeIndex = 0;
		int columnPosition = 0;

		// Reset the attribute mappings that were added in previous attempts
		attributeMappings.clear();

		for ( Property bootPropertyDescriptor : bootDescriptor.getProperties() ) {
			final AttributeMapping attributeMapping;

			final Type subtype = subtypes[attributeIndex];
			final Value value = bootPropertyDescriptor.getValue();
			if ( subtype instanceof BasicType ) {
				final BasicValue basicValue = (BasicValue) value;
				final Selectable selectable = dependantValue != null ?
						dependantValue.getColumns().get( dependantColumnIndex + columnPosition ) :
						basicValue.getColumn();
				final String containingTableExpression;
				final String columnExpression;
				if ( rootTableKeyColumnNames == null ) {
					if ( selectable.isFormula() ) {
						columnExpression = selectable.getTemplate(
								dialect,
								creationProcess.getCreationContext().getTypeConfiguration(),
								creationProcess.getSqmFunctionRegistry()
						);
					}
					else {
						columnExpression = selectable.getText( dialect );
					}
					if ( selectable instanceof Column ) {
						final Column column = (Column) selectable;
						containingTableExpression = MappingModelCreationHelper.getTableIdentifierExpression(
								column.getValue().getTable(),
								creationProcess
						);
					}
					else {
						containingTableExpression = baseTableExpression;
					}
				}
				else {
					containingTableExpression = rootTableExpression;
					columnExpression = rootTableKeyColumnNames[columnPosition];
				}
				final SelectablePath selectablePath;
				final String columnDefinition;
				final Long length;
				final Integer precision;
				final Integer scale;
				final boolean nullable;
				if ( selectable instanceof Column ) {
					final Column column = (Column) selectable;
					columnDefinition = column.getSqlType();
					length = column.getLength();
					precision = column.getPrecision();
					scale = column.getScale();
					nullable = column.isNullable();
					selectablePath = basicValue.createSelectablePath( column.getQuotedName( dialect ) );
				}
				else {
					columnDefinition = null;
					length = null;
					precision = null;
					scale = null;
					nullable = true;
					selectablePath = basicValue.createSelectablePath( bootPropertyDescriptor.getName() );
				}
				attributeMapping = MappingModelCreationHelper.buildBasicAttributeMapping(
						bootPropertyDescriptor.getName(),
						valueMapping.getNavigableRole().append( bootPropertyDescriptor.getName() ),
						attributeIndex,
						attributeIndex,
						bootPropertyDescriptor,
						this,
						(BasicType<?>) subtype,
						containingTableExpression,
						columnExpression,
						selectablePath,
						selectable.isFormula(),
						selectable.getCustomReadExpression(),
						selectable.getWriteExpr( ( (BasicType<?>) subtype ).getJdbcMapping(), dialect ),
						columnDefinition,
						length,
						precision,
						scale,
						nullable,
						insertability[columnPosition],
						updateability[columnPosition],
						representationStrategy.resolvePropertyAccess( bootPropertyDescriptor ),
						compositeType.getCascadeStyle( attributeIndex ),
						creationProcess
				);

				columnPosition++;
			}
			else if ( subtype instanceof AnyType ) {
				final Any bootValueMapping = (Any) value;
				final AnyType anyType = (AnyType) subtype;

				final PropertyAccess propertyAccess = representationStrategy.resolvePropertyAccess( bootPropertyDescriptor );
				final boolean nullable = bootValueMapping.isNullable();
				final boolean insertable = insertability[columnPosition];
				final boolean updateable = updateability[columnPosition];
				final boolean includeInOptimisticLocking = bootPropertyDescriptor.isOptimisticLocked();
				final CascadeStyle cascadeStyle = compositeType.getCascadeStyle( attributeIndex );

				SimpleAttributeMetadata attributeMetadataAccess = new SimpleAttributeMetadata(
						propertyAccess,
						getMutabilityPlan( updateable ),
						nullable,
						insertable,
						updateable,
						includeInOptimisticLocking,
						true,
						cascadeStyle
				);

				attributeMapping = new DiscriminatedAssociationAttributeMapping(
						valueMapping.getNavigableRole().append( bootPropertyDescriptor.getName() ),
						typeConfiguration.getJavaTypeRegistry().getDescriptor( Object.class ),
						this,
						attributeIndex,
						attributeIndex,
						attributeMetadataAccess,
						bootPropertyDescriptor.isLazy() ? FetchTiming.DELAYED : FetchTiming.IMMEDIATE,
						propertyAccess,
						bootPropertyDescriptor,
						anyType,
						bootValueMapping,
						creationProcess
				);
			}
			else if ( subtype instanceof CompositeType ) {
				final CompositeType subCompositeType = (CompositeType) subtype;
				final int columnSpan = subCompositeType.getColumnSpan( creationProcess.getCreationContext().getMetadata() );
				final String subTableExpression;
				final String[] subRootTableKeyColumnNames;
				if ( rootTableKeyColumnNames == null ) {
					subTableExpression = baseTableExpression;
					subRootTableKeyColumnNames = null;
				}
				else {
					subTableExpression = rootTableExpression;
					subRootTableKeyColumnNames = new String[columnSpan];
					System.arraycopy( rootTableKeyColumnNames, columnPosition, subRootTableKeyColumnNames, 0, columnSpan );
				}

				attributeMapping = MappingModelCreationHelper.buildEmbeddedAttributeMapping(
						bootPropertyDescriptor.getName(),
						attributeIndex,
						attributeIndex,
						bootPropertyDescriptor,
						dependantValue,
						dependantColumnIndex + columnPosition,
						this,
						subCompositeType,
						subTableExpression,
						subRootTableKeyColumnNames,
						representationStrategy.resolvePropertyAccess( bootPropertyDescriptor ),
						compositeType.getCascadeStyle( attributeIndex ),
						creationProcess
				);

				columnPosition += columnSpan;
			}
			else if ( subtype instanceof CollectionType ) {
				final EntityPersister entityPersister = creationProcess.getEntityPersister( bootDescriptor.getOwner().getEntityName() );

				attributeMapping = MappingModelCreationHelper.buildPluralAttributeMapping(
						bootPropertyDescriptor.getName(),
						attributeIndex,
						attributeIndex,
						bootPropertyDescriptor,
						entityPersister,
						representationStrategy.resolvePropertyAccess( bootPropertyDescriptor ),
						compositeType.getCascadeStyle( attributeIndex),
						compositeType.getFetchMode( attributeIndex ),
						creationProcess
				);
			}
			else if ( subtype instanceof EntityType ) {
				final EntityPersister entityPersister = creationProcess.getEntityPersister( bootDescriptor.getOwner().getEntityName() );

				attributeMapping = MappingModelCreationHelper.buildSingularAssociationAttributeMapping(
						bootPropertyDescriptor.getName(),
						valueMapping.getNavigableRole().append( bootPropertyDescriptor.getName() ),
						attributeIndex,
						attributeIndex,
						bootPropertyDescriptor,
						entityPersister,
						entityPersister,
						(EntityType) subtype,
						getRepresentationStrategy().resolvePropertyAccess( bootPropertyDescriptor ),
						compositeType.getCascadeStyle( attributeIndex ),
						creationProcess
				);
				columnPosition += bootPropertyDescriptor.getColumnSpan();
			}
			else {
				throw new MappingException(
						String.format(
								Locale.ROOT,
								"Unable to determine attribute nature : %s#%s",
								bootDescriptor.getOwner().getEntityName(),
								bootPropertyDescriptor.getName()
						)
				);
			}

			addAttribute( attributeMapping );

			attributeIndex++;
		}

		// We need the attribute mapping types to finish initialization first before we can build the column mappings
		creationProcess.registerInitializationCallback(
				"EmbeddableMappingType(" + getEmbeddedValueMapping().getNavigableRole().getFullPath() + ")#initColumnMappings",
				this::initColumnMappings
		);

		return true;
	}

	private static MutabilityPlan<?> getMutabilityPlan(boolean updateable) {
		if ( updateable ) {
			return new MutabilityPlan<>() {
				@Override
				public boolean isMutable() {
					return true;
				}

				@Override
				public Object deepCopy(Object value) {
					return value;
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
			return ImmutableMutabilityPlan.INSTANCE;
		}
	}



	public EmbeddableValuedModelPart getEmbeddedValueMapping() {
		return valueMapping;
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return embeddableJtd;
	}

	public EmbeddableRepresentationStrategy getRepresentationStrategy() {
		return representationStrategy;
	}

	@Override
	public String getPartName() {
		return getEmbeddedValueMapping().getPartName();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return valueMapping.getNavigableRole();
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new EmbeddableResultImpl<>(
				navigablePath,
				valueMapping,
				resultVariable,
				creationState
		);
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		final int size = attributeMappings.size();
		int span = 0;
		if ( domainValue instanceof Object[] ) {
			final Object[] values = (Object[]) domainValue;
			assert values.length == size;

			for ( int i = 0; i < size; i++ ) {
				final AttributeMapping attributeMapping = attributeMappings.get( i );
				final Object attributeValue = values[ i ];
				span += attributeMapping.breakDownJdbcValues(
						attributeValue,
						offset + span,
						x,
						y,
						valueConsumer,
						session
				);
			}
		}
		else {
			for ( int i = 0; i < size; i++ ) {
				final AttributeMapping attributeMapping = attributeMappings.get( i );
				final Object attributeValue = domainValue == null
						? null
						: attributeMapping.getPropertyAccess().getGetter().get( domainValue );
				span += attributeMapping.breakDownJdbcValues(
						attributeValue,
						offset + span,
						x,
						y,
						valueConsumer,
						session
				);
			}
		}
		return span;
	}

	@Override
	public <X, Y> int decompose(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		if ( shouldBindAggregateMapping() ) {
			valueConsumer.consume( offset, x, y, domainValue, aggregateMapping );
			return 1;
		}
		int span = 0;
		if ( domainValue instanceof Object[] ) {
			final Object[] values = (Object[]) domainValue;
			assert values.length == attributeMappings.size();

			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final AttributeMapping attributeMapping = attributeMappings.get( i );
				final Object attributeValue = values[ i ];
				span += attributeMapping.decompose( attributeValue, offset + span, x, y, valueConsumer, session );
			}
		}
		else {
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final AttributeMapping attributeMapping = attributeMappings.get( i );
				if ( !(attributeMapping instanceof PluralAttributeMapping )) {
					final Object attributeValue = domainValue == null
							? null
							: attributeMapping.getPropertyAccess().getGetter().get( domainValue );
					span += attributeMapping.decompose( attributeValue, offset + span, x, y, valueConsumer, session );
				}
			}
		}
		return span;
	}

	@Override
	public void forEachInsertable(int offset, SelectableConsumer consumer) {
		if ( shouldMutateAggregateMapping() ) {
			if ( aggregateMapping.isInsertable() ) {
				consumer.accept( offset, aggregateMapping );
			}
		}
		else {
			final int jdbcTypeCount = selectableMappings.getJdbcTypeCount();
			for ( int i = 0; i < jdbcTypeCount; i++ ) {
				final SelectableMapping selectable = selectableMappings.getSelectable( i );
				if ( selectable.isInsertable() ) {
					consumer.accept( offset + i, selectable );
				}
			}
		}
	}

	@Override
	public void forEachUpdatable(int offset, SelectableConsumer consumer) {
		if ( shouldMutateAggregateMapping() ) {
			if ( aggregateMapping.isUpdateable() ) {
				consumer.accept( offset, aggregateMapping );
			}
		}
		else {
			final int jdbcTypeCount = selectableMappings.getJdbcTypeCount();
			for ( int i = 0; i < jdbcTypeCount; i++ ) {
				final SelectableMapping selectable = selectableMappings.getSelectable( i );
				if ( selectable.isUpdateable() ) {
					consumer.accept( offset + i, selectable );
				}
			}
		}
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return valueMapping.findContainingEntityMapping();
	}


	@Override
	public boolean isCreateEmptyCompositesEnabled() {
		return createEmptyCompositesEnabled;
	}

	@Override
	public SelectableMapping getAggregateMapping() {
		return aggregateMapping;
	}

	@Override
	public boolean requiresAggregateColumnWriter() {
		return aggregateMappingRequiresColumnWriter;
	}

	@Override
	public boolean shouldSelectAggregateMapping() {
		return preferSelectAggregateMapping;
	}

	@Override
	public boolean shouldBindAggregateMapping() {
		return preferBindAggregateMapping;
	}
}
