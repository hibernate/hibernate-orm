/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import org.hibernate.MappingException;
import org.hibernate.SharedSessionContract;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableResultImpl;
import org.hibernate.type.AnyType;
import org.hibernate.type.BasicType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.JdbcTypeNameMapper;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.spi.CompositeTypeImplementor;

import static java.lang.System.arraycopy;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.buildBasicAttributeMapping;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.buildEmbeddedAttributeMapping;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.buildPluralAttributeMapping;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.buildSingularAssociationAttributeMapping;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.getTableIdentifierExpression;
import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.JSON_ARRAY;
import static org.hibernate.type.SqlTypes.SQLXML;
import static org.hibernate.type.SqlTypes.STRUCT;
import static org.hibernate.type.SqlTypes.STRUCT_ARRAY;
import static org.hibernate.type.SqlTypes.STRUCT_TABLE;
import static org.hibernate.type.SqlTypes.XML_ARRAY;

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

		if ( compositeType instanceof CompositeTypeImplementor compositeTypeImplementor ) {
			compositeTypeImplementor.injectMappingModelPart( mappingType.getEmbeddedValueMapping(), creationProcess );
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
	private final EmbeddableDiscriminatorMapping discriminatorMapping;
	private final Map<String, ConcreteEmbeddableTypeImpl> concreteEmbeddableBySubclass;
	private final Map<Object, ConcreteEmbeddableTypeImpl> concreteEmbeddableByDiscriminator;

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
		this.discriminatorMapping = generateDiscriminatorMapping( bootDescriptor, creationContext );
		if ( bootDescriptor.isPolymorphic() ) {
			this.concreteEmbeddableByDiscriminator = new HashMap<>();
			this.concreteEmbeddableBySubclass = new HashMap<>();

			int subclassId = 0;
			// Sort the entries by embeddable class name to have a somewhat stable subclass id
			final Set<Map.Entry<Object, String>> entries = new TreeSet<>( Map.Entry.comparingByValue() );
			entries.addAll( bootDescriptor.getDiscriminatorValues().entrySet() );
			for ( final var discriminatorEntry : entries ) {
				final var concreteEmbeddableType = new ConcreteEmbeddableTypeImpl(
						representationStrategy.getInstantiatorForDiscriminator( discriminatorEntry.getKey() ),
						discriminatorEntry.getKey(),
						subclassId++
				);
				concreteEmbeddableByDiscriminator.put( discriminatorEntry.getKey(), concreteEmbeddableType );
				concreteEmbeddableBySubclass.put( discriminatorEntry.getValue(), concreteEmbeddableType );
			}
		}
		else {
			this.concreteEmbeddableByDiscriminator = null;
			this.concreteEmbeddableBySubclass = null;
		}

		final var aggregateColumn = bootDescriptor.getAggregateColumn();
		if ( aggregateColumn != null ) {
			final var dialect = creationContext.getDialect();
			final boolean insertable;
			final boolean updatable;
			if ( componentProperty == null ) {
				insertable = true;
				updatable = true;
			}
			else {
				insertable = componentProperty.isInsertable();
				updatable = componentProperty.isUpdatable();
			}
			this.aggregateMapping = SelectableMappingImpl.from(
					bootDescriptor.getOwner().getTable()
							.getQualifiedName( creationContext.getSqlStringGenerationContext() ),
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
					null,
					creationContext
			);
			final int defaultSqlTypeCode =
					aggregateMapping.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode();
			final var aggregateSupport = dialect.getAggregateSupport();
			final int sqlTypeCode =
					defaultSqlTypeCode == ARRAY
							? aggregateColumn.getTypeCode()
							: defaultSqlTypeCode;
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
		final var typeConfiguration = creationContext.getTypeConfiguration();
		final var basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		final var aggregateColumn = bootDescriptor.getAggregateColumn();
		final var basicValue = (BasicValue) aggregateColumn.getValue();
		final var resolution = basicValue.getResolution();
		final int aggregateColumnSqlTypeCode = resolution.getJdbcType().getDefaultSqlTypeCode();
		final int aggregateSqlTypeCode;
		boolean isArray = false;
		String structTypeName = null;
		switch ( aggregateColumnSqlTypeCode ) {
			case STRUCT:
				aggregateSqlTypeCode = STRUCT;
				structTypeName = aggregateColumn.getSqlType( creationContext.getMetadata() );
				break;
			case ARRAY:
			case STRUCT_ARRAY:
			case STRUCT_TABLE:
				isArray = true;
				aggregateSqlTypeCode = STRUCT;
				structTypeName = bootDescriptor.getStructName().render();
				if ( structTypeName == null ) {
					final String arrayTypeName = aggregateColumn.getSqlType( creationContext.getMetadata() );
					if ( arrayTypeName.endsWith( " array" ) ) {
						structTypeName = arrayTypeName.substring( 0, arrayTypeName.length() - " array".length() );
					}
				}
				break;
			case JSON_ARRAY:
				isArray = true;
				aggregateSqlTypeCode = JSON;
				break;
			case XML_ARRAY:
				isArray = true;
				aggregateSqlTypeCode = SQLXML;
				break;
			default:
				aggregateSqlTypeCode = aggregateColumnSqlTypeCode;
				break;
		}
		final var jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();
		final var aggregateJdbcType = jdbcTypeRegistry.resolveAggregateDescriptor(
				aggregateSqlTypeCode,
				structTypeName,
				this,
				creationContext
		);
		final var basicType = basicTypeRegistry.resolve( getMappedJavaType(), aggregateJdbcType );
		// Register the resolved type under its struct name and java class name
		final var structName = bootDescriptor.getStructName();
		if ( structName != null ) {
			basicTypeRegistry.register( basicType, structName.render() );
			basicTypeRegistry.register( basicType, getMappedJavaType().getJavaTypeClass().getName() );
		}
		final BasicType<?> resolvedJdbcMapping;
		if ( isArray ) {
			final var arrayConstructor = jdbcTypeRegistry.getConstructor( aggregateColumnSqlTypeCode );
			if ( arrayConstructor == null ) {
				throw new IllegalArgumentException( "No JdbcTypeConstructor registered for SqlTypes." + JdbcTypeNameMapper.getTypeName( aggregateColumnSqlTypeCode ) );
			}
			//noinspection rawtypes,unchecked
			final BasicType<?> arrayType =
					( (BasicPluralJavaType) resolution.getDomainJavaType() )
							.resolveType(
									typeConfiguration,
									creationContext.getDialect(),
									basicType,
									aggregateColumn,
									typeConfiguration.getCurrentBaseSqlTypeIndicators()
							);
			basicTypeRegistry.register( arrayType );
			resolvedJdbcMapping = arrayType;
		}
		else {
			resolvedJdbcMapping = basicType;
		}
		resolution.updateResolution( resolvedJdbcMapping );
		return resolvedJdbcMapping;
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
		this.discriminatorMapping = null;
		this.concreteEmbeddableBySubclass = null;
		this.concreteEmbeddableByDiscriminator = null;
		this.aggregateMapping = null;
		this.aggregateMappingRequiresColumnWriter = false;
		this.preferSelectAggregateMapping = false;
		this.preferBindAggregateMapping = false;
		this.selectableMappings = selectableMappings;
		creationProcess.registerInitializationCallback(
				"EmbeddableMappingType("
						+ inverseMappingType.getNavigableRole().getFullPath()
						+ ".{inverse})#finishInitialization",
				() -> inverseInitializeCallback(
						declaringTableGroupProducer,
						selectableMappings,
						inverseMappingType,
						creationProcess,
						this,
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

		final var creationContext = creationProcess.getCreationContext();
		final var typeConfiguration = creationContext.getTypeConfiguration();
		final var dialect =
				creationContext.getJdbcServices()
						.getJdbcEnvironment().getDialect();

		final String baseTableExpression = valueMapping.getContainingTableExpression();
		final var subtypes = compositeType.getSubtypes();

		int attributeIndex = 0;
		int columnPosition = 0;

		// Reset the attribute mappings that were added in previous attempts
		attributeMappings.clear();

		for ( final var bootPropertyDescriptor : bootDescriptor.getProperties() ) {
			final AttributeMapping attributeMapping;

			final Type subtype = subtypes[attributeIndex];
			final Value value = bootPropertyDescriptor.getValue();
			if ( subtype instanceof BasicType ) {
				final var basicValue = (BasicValue) value;
				final var selectable =
						dependantValue != null
								? dependantValue.getColumns().get( dependantColumnIndex + columnPosition )
								: basicValue.getColumn();
				final String containingTableExpression;
				final String columnExpression;
				if ( rootTableKeyColumnNames == null ) {
					columnExpression =
							selectable.isFormula()
									? selectable.getTemplate( dialect, typeConfiguration )
									: selectable.getText( dialect );
					if ( selectable instanceof Column column ) {
						containingTableExpression =
								getTableIdentifierExpression( column.getValue().getTable(), creationProcess );
					}
					else {
						containingTableExpression = baseTableExpression;
					}
				}
				else {
					containingTableExpression = rootTableExpression;
					columnExpression = rootTableKeyColumnNames[columnPosition];
				}
				final var role =
						valueMapping.getNavigableRole()
								.append( bootPropertyDescriptor.getName() );
				final SelectablePath selectablePath;
				final String columnDefinition;
				final String sqlTypeName;
				final Long length;
				final Integer arrayLength;
				final Integer precision;
				final Integer scale;
				final Integer temporalPrecision;
				final boolean isLob;
				final boolean nullable;
				if ( selectable instanceof Column column ) {
					final var columnSize =
							column.getColumnSize( dialect, creationProcess.getCreationContext().getMetadata() );
					columnDefinition = column.getColumnDefinition();
					sqlTypeName = column.getSqlType();
					length = columnSize.getLength();
					arrayLength = columnSize.getArrayLength();
					precision = columnSize.getPrecision();
					scale = columnSize.getScale();
					temporalPrecision = column.getTemporalPrecision();
					isLob = column.isSqlTypeLob( creationContext.getMetadata() );
					nullable = bootPropertyDescriptor.isOptional() && column.isNullable() ;
					selectablePath = basicValue.createSelectablePath( column.getQuotedName( dialect ) );
					MappingModelCreationHelper.resolveAggregateColumnBasicType( creationProcess, role, column );
				}
				else {
					columnDefinition = null;
					sqlTypeName = null;
					length = null;
					arrayLength = null;
					precision = null;
					scale = null;
					temporalPrecision = null;
					isLob = false;
					nullable = bootPropertyDescriptor.isOptional();
					selectablePath = new SelectablePath( determineEmbeddablePrefix() + bootPropertyDescriptor.getName() );
				}
				attributeMapping = buildBasicAttributeMapping(
						bootPropertyDescriptor.getName(),
						role,
						attributeIndex,
						attributeIndex,
						bootPropertyDescriptor,
						this,
						basicValue.getResolution().getLegacyResolvedBasicType(),
						containingTableExpression,
						columnExpression,
						selectablePath,
						selectable.isFormula(),
						selectable.getCustomReadExpression(),
						selectable.getWriteExpr(
								basicValue.getResolution().getJdbcMapping(),
								dialect,
								creationContext.getBootModel()
						),
						columnDefinition,
						sqlTypeName,
						length,
						arrayLength,
						precision,
						scale,
						temporalPrecision,
						isLob,
						nullable,
						insertability[columnPosition],
						updateability[columnPosition],
						representationStrategy.resolvePropertyAccess( bootPropertyDescriptor ),
						compositeType.getCascadeStyle( attributeIndex ),
						creationProcess
				);

				columnPosition++;
			}
			else if ( subtype instanceof AnyType anyType ) {
				final var bootValueMapping = (Any) value;
				final var propertyAccess =
						representationStrategy.resolvePropertyAccess( bootPropertyDescriptor );
				final var attributeMetadataAccess = new SimpleAttributeMetadata(
						propertyAccess,
						getMutabilityPlan( updateability[columnPosition] ),
						bootValueMapping.isNullable(),
						insertability[columnPosition],
						updateability[columnPosition],
						bootPropertyDescriptor.isOptimisticLocked(),
						true,
						compositeType.getCascadeStyle( attributeIndex )
				);

				attributeMapping = new DiscriminatedAssociationAttributeMapping(
						valueMapping.getNavigableRole()
								.append( bootPropertyDescriptor.getName() ),
						typeConfiguration.getJavaTypeRegistry()
								.resolveDescriptor( Object.class ),
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
			else if ( subtype instanceof CompositeType subCompositeType ) {
				final int columnSpan = subCompositeType.getColumnSpan( creationContext.getMetadata() );
				final String subTableExpression;
				final String[] subRootTableKeyColumnNames;
				if ( rootTableKeyColumnNames == null ) {
					subTableExpression = baseTableExpression;
					subRootTableKeyColumnNames = null;
				}
				else {
					subTableExpression = rootTableExpression;
					subRootTableKeyColumnNames = new String[columnSpan];
					arraycopy( rootTableKeyColumnNames, columnPosition, subRootTableKeyColumnNames, 0, columnSpan );
				}

				attributeMapping = buildEmbeddedAttributeMapping(
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
				attributeMapping = buildPluralAttributeMapping(
						bootPropertyDescriptor.getName(),
						attributeIndex,
						attributeIndex,
						bootPropertyDescriptor,
						this,
						representationStrategy.resolvePropertyAccess( bootPropertyDescriptor ),
						compositeType.getCascadeStyle( attributeIndex),
						compositeType.getFetchMode( attributeIndex ),
						creationProcess
				);
			}
			else if ( subtype instanceof EntityType subentityType ) {
				attributeMapping = buildSingularAssociationAttributeMapping(
						bootPropertyDescriptor.getName(),
						valueMapping.getNavigableRole().append( bootPropertyDescriptor.getName() ),
						attributeIndex,
						attributeIndex,
						bootPropertyDescriptor,
						this,
						creationProcess.getEntityPersister( bootDescriptor.getOwner().getEntityName() ),
						subentityType,
						representationStrategy.resolvePropertyAccess( bootPropertyDescriptor ),
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

			if ( isPolymorphic() ) {
				final String declaringClass = bootDescriptor.getPropertyDeclaringClass( bootPropertyDescriptor );
				for ( var entry : concreteEmbeddableBySubclass.entrySet() ) {
					if ( isDefinedInClassOrSuperclass( bootDescriptor, declaringClass, entry.getKey() ) ) {
						entry.getValue().declaredAttributes.set( attributeMapping.getStateArrayPosition() );
					}
				}
			}

			addAttribute( attributeMapping );

			attributeIndex++;
		}

		// We need the attribute mapping types to finish initialization first before we can build the column mappings
		creationProcess.registerInitializationCallback(
				"EmbeddableMappingType(" + valueMapping.getNavigableRole().getFullPath() + ")#initColumnMappings",
				this::initColumnMappings
		);

		return true;
	}

	private boolean isDefinedInClassOrSuperclass(Component bootDescriptor, String declaringClass, String subclass) {
		while ( subclass != null ) {
			if ( declaringClass.equals( subclass ) ) {
				return true;
			}
			subclass = bootDescriptor.getSuperclass( subclass );
		}
		return false;
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
			return ImmutableMutabilityPlan.instance();
		}
	}

	private EmbeddableDiscriminatorMapping generateDiscriminatorMapping(
			Component bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		final var discriminator = bootDescriptor.getDiscriminator();
		if ( discriminator == null ) {
			return null;
		}

		final var selectable = discriminator.getSelectables().get( 0 );
		final String discriminatorColumnExpression;
		final String columnDefinition;
		final String sqlTypeName;
		final String name;
		final Long length;
		final Integer arrayLength;
		final Integer precision;
		final Integer scale;
		final boolean isFormula = discriminator.hasFormula();
		if ( isFormula ) {
			final var formula = (Formula) selectable;
			discriminatorColumnExpression = name = formula.getTemplate(
					creationContext.getDialect(),
					creationContext.getTypeConfiguration()
			);
			columnDefinition = null;
			sqlTypeName = null;
			length = null;
			arrayLength = null;
			precision = null;
			scale = null;
		}
		else {
			final var column = discriminator.getColumns().get( 0 );
			assert column != null : "Embeddable discriminators require a column";
			final var columnSize =
					column.getColumnSize( creationContext.getDialect(), creationContext.getMetadata() );
			discriminatorColumnExpression = column.getReadExpr( creationContext.getDialect() );
			columnDefinition = column.getColumnDefinition();
			sqlTypeName = column.getSqlType();
			name = column.getName();
			length = columnSize.getLength();
			arrayLength = columnSize.getArrayLength();
			precision = columnSize.getPrecision();
			scale = columnSize.getScale();
		}

		return new ExplicitColumnDiscriminatorMappingImpl(
				this,
				name,
				bootDescriptor.getTable()
						.getQualifiedName( creationContext.getSqlStringGenerationContext() ),
				discriminatorColumnExpression,
				isFormula,
				!isFormula,
				!isFormula,
				columnDefinition,
				sqlTypeName,
				selectable.getCustomReadExpression(),
				length,
				arrayLength,
				precision,
				scale,
				bootDescriptor.getDiscriminatorType()
		);
	}

	public EmbeddableValuedModelPart getEmbeddedValueMapping() {
		return valueMapping;
	}

	@Override
	public EmbeddableDiscriminatorMapping getDiscriminatorMapping() {
		return discriminatorMapping;
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

	private static final class ConcreteEmbeddableTypeImpl implements ConcreteEmbeddableType {
		private final EmbeddableInstantiator instantiator;
		private final Object discriminatorValue;
		private final int subclassId;
		private final BitSet declaredAttributes;

		public ConcreteEmbeddableTypeImpl(EmbeddableInstantiator instantiator, Object discriminatorValue, int subclassId) {
			this.instantiator = instantiator;
			this.discriminatorValue = discriminatorValue;
			this.subclassId = subclassId;
			this.declaredAttributes = new BitSet();
		}

		@Override
		public EmbeddableInstantiator getInstantiator() {
			return instantiator;
		}

		@Override
		public Object getDiscriminatorValue() {
			return discriminatorValue;
		}

		@Override
		public int getSubclassId() {
			return subclassId;
		}

		@Override
		public boolean declaresAttribute(AttributeMapping attributeMapping) {
			return declaredAttributes.get( attributeMapping.getStateArrayPosition() );
		}

		@Override
		public boolean declaresAttribute(int attributeIndex) {
			return declaredAttributes.get( attributeIndex );
		}
	}

	@Override
	public ConcreteEmbeddableType findSubtypeByDiscriminator(Object discriminatorValue) {
		return concreteEmbeddableByDiscriminator == null ? this : concreteEmbeddableByDiscriminator.get( discriminatorValue );
	}

	@Override
	public ConcreteEmbeddableType findSubtypeBySubclass(String subclassName) {
		return concreteEmbeddableBySubclass == null ? this : concreteEmbeddableBySubclass.get( subclassName );
	}

	@Override
	public Collection<ConcreteEmbeddableType> getConcreteEmbeddableTypes() {
		//noinspection unchecked
		return concreteEmbeddableBySubclass == null
				? Collections.singleton( this )
				: (Collection<ConcreteEmbeddableType>) (Collection<?>) concreteEmbeddableBySubclass.values();
	}

	@Override
	protected Object[] getAttributeValues(Object compositeInstance) {
		if ( !isPolymorphic() ) {
			return super.getAttributeValues( compositeInstance );
		}
		else {
			final int numberOfAttributes = getNumberOfAttributeMappings();
			final var results = new Object[numberOfAttributes + 1];
			final var concreteEmbeddableType = findSubtypeBySubclass( compositeInstance.getClass().getName() );
			int i = 0;
			for ( ; i < numberOfAttributes; i++ ) {
				results[i] =
						concreteEmbeddableType.declaresAttribute( i )
								? getValue( compositeInstance, i )
								: null;
			}
			results[i] = compositeInstance.getClass();
			return results;
		}
	}

	@Override
	protected void setAttributeValues(Object component, Object[] values) {
		if ( !isPolymorphic() ) {
			super.setAttributeValues( component, values );
		}
		else {
			final String compositeClassName = component.getClass().getName();
			final var concreteEmbeddableType = findSubtypeBySubclass( compositeClassName );
			for ( int i = 0; i < getNumberOfAttributeMappings(); i++ ) {
				final AttributeMapping attributeMapping = getAttributeMapping( i );
				if ( concreteEmbeddableType.declaresAttribute( attributeMapping ) ) {
					setValue( component, i, values[i] );
				}
				else if ( values[i] != null ) {
					throw new IllegalArgumentException( String.format(
							"Unexpected non-null value for embeddable subtype '%s'",
							compositeClassName
					) );
				}
			}
		}
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		return EntityDiscriminatorMapping.matchesRoleName( name )
				? discriminatorMapping
				: super.findSubPart( name, treatTargetType );

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
		if ( domainValue instanceof Object[] values ) {
			assert values.length == size + ( isPolymorphic() ? 1 : 0 );
			int i = 0;
			for ( ; i < size; i++ ) {
				final var attributeMapping = attributeMappings.get( i );
				if ( !attributeMapping.isPluralAttributeMapping() ) {
					final Object attributeValue = values[i];
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
			if ( isPolymorphic() ) {
				span += discriminatorMapping.breakDownJdbcValues( values[i], offset + span, x, y, valueConsumer, session );
			}
		}
		else {
			final var concreteEmbeddableType =
					domainValue == null
							? null
							: findSubtypeBySubclass( domainValue.getClass().getName() );
			for ( int i = 0; i < size; i++ ) {
				final var attributeMapping = attributeMappings.get( i );
				if ( !attributeMapping.isPluralAttributeMapping() ) {
					final Object attributeValue =
							concreteEmbeddableType == null
								|| !concreteEmbeddableType.declaresAttribute( attributeMapping )
									? null
									: getValue( domainValue, i );
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
			if ( isPolymorphic() ) {
				final Object d = concreteEmbeddableType == null ? null : concreteEmbeddableType.getDiscriminatorValue();
				span += discriminatorMapping.breakDownJdbcValues( d, offset + span, x, y, valueConsumer, session );
			}
		}
		return span;
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		int span = 0;
		if ( value == null ) {
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final var attributeMapping = attributeMappings.get( i );
				if ( !(attributeMapping instanceof PluralAttributeMapping) ) {
					span += attributeMapping.forEachJdbcValue( null, span + offset, x, y, valuesConsumer, session );
				}
			}
			if ( isPolymorphic() ) {
				span += discriminatorMapping.forEachJdbcValue( null, offset + span, x, y, valuesConsumer, session );
			}
		}
		else {
			final var concreteEmbeddableType = findSubtypeBySubclass( value.getClass().getName() );
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final var attributeMapping = attributeMappings.get( i );
				if ( !(attributeMapping instanceof PluralAttributeMapping) ) {
					final Object attributeValue =
							concreteEmbeddableType == null
								|| !concreteEmbeddableType.declaresAttribute( attributeMapping )
									? null
									: getValue( value, i );
					span += attributeMapping.forEachJdbcValue( attributeValue, span + offset, x, y, valuesConsumer, session );
				}
			}
			if ( isPolymorphic() ) {
				final Object d = concreteEmbeddableType == null ? null : concreteEmbeddableType.getDiscriminatorValue();
				span += discriminatorMapping.forEachJdbcValue( d, offset + span, x, y, valuesConsumer, session );
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
		final int size = attributeMappings.size();
		int span = 0;
		if ( domainValue instanceof Object[] values ) {
			assert values.length == size + ( isPolymorphic() ? 1 : 0 );
			int i = 0;
			for ( ; i < size; i++ ) {
				final var attributeMapping = attributeMappings.get( i );
				final Object attributeValue = values[ i ];
				span += attributeMapping.decompose( attributeValue, offset + span, x, y, valueConsumer, session );
			}
			if ( isPolymorphic() ) {
				span += discriminatorMapping.decompose( values[i], offset + span, x, y, valueConsumer, session );
			}
		}
		else {
			final var concreteEmbeddableType =
					domainValue == null
							? null
							: findSubtypeBySubclass( domainValue.getClass().getName() );
			for ( int i = 0; i < size; i++ ) {
				final var attributeMapping = attributeMappings.get( i );
				if ( !attributeMapping.isPluralAttributeMapping() ) {
					final Object attributeValue =
							concreteEmbeddableType == null
								|| !concreteEmbeddableType.declaresAttribute( attributeMapping )
									? null
									: attributeMapping.getPropertyAccess().getGetter().get( domainValue );
					span += attributeMapping.decompose( attributeValue, offset + span, x, y, valueConsumer, session );
				}
			}
			if ( isPolymorphic() ) {
				final Object d = concreteEmbeddableType == null ? null : concreteEmbeddableType.getDiscriminatorValue();
				span += discriminatorMapping.decompose( d, offset + span, x, y, valueConsumer, session );
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
				final var selectable = selectableMappings.getSelectable( i );
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
