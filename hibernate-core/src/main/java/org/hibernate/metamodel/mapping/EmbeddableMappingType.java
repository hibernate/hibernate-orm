/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.internal.BasicValuedSingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.SelectionMappingsImpl;
import org.hibernate.metamodel.mapping.internal.DiscriminatedAssociationAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableResultImpl;
import org.hibernate.type.AnyType;
import org.hibernate.type.BasicType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class EmbeddableMappingType implements ManagedMappingType, SelectionMappings {

	public static EmbeddableMappingType from(
			Component bootDescriptor,
			CompositeType compositeType,
			Function<EmbeddableMappingType,EmbeddableValuedModelPart> embeddedPartBuilder,
			MappingModelCreationProcess creationProcess) {
		return from( bootDescriptor, compositeType, null, null, embeddedPartBuilder, creationProcess );
	}

	public static EmbeddableMappingType from(
			Component bootDescriptor,
			CompositeType compositeType,
			String rootTableExpression,
			String[] rootTableKeyColumnNames,
			Function<EmbeddableMappingType,EmbeddableValuedModelPart> embeddedPartBuilder,
			MappingModelCreationProcess creationProcess) {
		final RuntimeModelCreationContext creationContext = creationProcess.getCreationContext();

		final EmbeddableRepresentationStrategy representationStrategy = creationContext.getBootstrapContext()
				.getRepresentationStrategySelector()
				.resolveStrategy( bootDescriptor, creationContext );

		final EmbeddableMappingType mappingType = new EmbeddableMappingType(
				bootDescriptor,
				representationStrategy,
				embeddedPartBuilder,
				creationContext.getSessionFactory()
		);

		creationProcess.registerInitializationCallback(
				"EmbeddableMappingType(" + bootDescriptor.getRoleName() + ")#finishInitialization",
				() -> mappingType.finishInitialization(
						bootDescriptor,
						compositeType,
						rootTableExpression,
						rootTableKeyColumnNames,
						creationProcess
				)
		);

		return mappingType;
	}

	private final JavaTypeDescriptor<?> embeddableJtd;
	private final EmbeddableRepresentationStrategy representationStrategy;

	private final SessionFactoryImplementor sessionFactory;

//	private final Map<String,AttributeMapping> attributeMappings = new TreeMap<>();
	private final List<AttributeMapping> attributes = new ArrayList<>();
	private final Map<String, AttributeMapping> attributeMap = new HashMap<>();
	private SelectionMappings selectionMappings;

	private final EmbeddableValuedModelPart valueMapping;
	private NavigableRole embeddedRole;

	private final boolean createEmptyCompositesEnabled;

	private EmbeddableMappingType(
			Component bootDescriptor,
			EmbeddableRepresentationStrategy representationStrategy,
			Function<EmbeddableMappingType, EmbeddableValuedModelPart> embeddedPartBuilder,
			SessionFactoryImplementor sessionFactory) {
		this.embeddableJtd = representationStrategy.getMappedJavaTypeDescriptor();
		this.representationStrategy = representationStrategy;
		this.sessionFactory = sessionFactory;

		this.valueMapping = embeddedPartBuilder.apply( this );

		final ConfigurationService cs = sessionFactory.getServiceRegistry()
				.getService(ConfigurationService.class);

		this.createEmptyCompositesEnabled = ConfigurationHelper.getBoolean(
				Environment.CREATE_EMPTY_COMPOSITES_ENABLED,
				cs.getSettings(),
				false
		);

	}

	private boolean finishInitialization(
			Component bootDescriptor,
			CompositeType compositeType,
			String rootTableExpression,
			String[] rootTableKeyColumnNames,
			MappingModelCreationProcess creationProcess) {
		final SessionFactoryImplementor sessionFactory = creationProcess.getCreationContext().getSessionFactory();
		final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();

		final String baseTableExpression = valueMapping.getContainingTableExpression();
		final Dialect dialect = creationProcess.getCreationContext().getSessionFactory().getJdbcServices().getDialect();
		final Type[] subtypes = compositeType.getSubtypes();

		int attributeIndex = 0;
		int columnPosition = 0;

		//noinspection unchecked
		final Iterator<Property> propertyIterator = bootDescriptor.getPropertyIterator();
		while ( propertyIterator.hasNext() ) {
			final Property bootPropertyDescriptor = propertyIterator.next();

			final Type subtype = subtypes[attributeIndex];
			if ( subtype instanceof BasicType ) {
				final BasicValue basicValue = (BasicValue) bootPropertyDescriptor.getValue();
				final Selectable selectable = basicValue.getColumn();
				final String containingTableExpression;
				final String columnExpression;
				if ( rootTableKeyColumnNames == null ) {
					if ( selectable.isFormula() ) {
						columnExpression = selectable.getTemplate( dialect, creationProcess.getSqmFunctionRegistry() );
					}
					else {
						columnExpression = selectable.getText( dialect );
					}
					if ( selectable instanceof Column ) {
						containingTableExpression = getTableIdentifierExpression(
								( (Column) selectable ).getValue().getTable(),
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

				BasicValuedSingularAttributeMapping attributeMapping = MappingModelCreationHelper.buildBasicAttributeMapping(
						bootPropertyDescriptor.getName(),
						valueMapping.getNavigableRole().append( bootPropertyDescriptor.getName() ),
						attributeIndex,
						bootPropertyDescriptor,
						this,
						(BasicType<?>) subtype,
						containingTableExpression,
						columnExpression,
						selectable.isFormula(),
						selectable.getCustomReadExpression(),
						selectable.getCustomWriteExpression(),
						representationStrategy.resolvePropertyAccess( bootPropertyDescriptor ),
						compositeType.getCascadeStyle( attributeIndex ),
						creationProcess
				);
				addAttribute( attributeMapping );
				columnPosition++;
			}
			else if ( subtype instanceof AnyType ) {
				final Any bootValueMapping = (Any) bootPropertyDescriptor.getValue();
				final AnyType anyType = (AnyType) subtype;

				final PropertyAccess propertyAccess = representationStrategy.resolvePropertyAccess( bootPropertyDescriptor );
				final boolean nullable = bootValueMapping.isNullable();
				final boolean insertable = bootPropertyDescriptor.isInsertable();
				final boolean updateable = bootPropertyDescriptor.isUpdateable();
				final boolean includeInOptimisticLocking = bootPropertyDescriptor.isOptimisticLocked();
				final CascadeStyle cascadeStyle = compositeType.getCascadeStyle( attributeIndex );
				final MutabilityPlan mutabilityPlan;

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

							return anyType.deepCopy( value, creationProcess.getCreationContext().getSessionFactory() );
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

				final StateArrayContributorMetadataAccess attributeMetadataAccess = entityMappingType -> new StateArrayContributorMetadata() {
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

				DiscriminatedAssociationAttributeMapping attributeMapping = new DiscriminatedAssociationAttributeMapping(
						valueMapping.getNavigableRole().append( bootPropertyDescriptor.getName() ),
						typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Object.class ),
						this,
						attributeIndex,
						attributeMetadataAccess,
						bootPropertyDescriptor.isLazy() ? FetchTiming.DELAYED : FetchTiming.IMMEDIATE,
						propertyAccess,
						bootPropertyDescriptor,
						anyType,
						bootValueMapping,
						creationProcess
				);
				addAttribute( attributeMapping );
			}
			else if ( subtype instanceof CompositeType ) {
				final CompositeType subCompositeType = (CompositeType) subtype;
				final int columnSpan = subCompositeType.getColumnSpan( sessionFactory );
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

				EmbeddedAttributeMapping attributeMapping = MappingModelCreationHelper.buildEmbeddedAttributeMapping(
						bootPropertyDescriptor.getName(),
						attributeIndex,
						bootPropertyDescriptor,
						this,
						subCompositeType,
						subTableExpression,
						subRootTableKeyColumnNames,
						representationStrategy.resolvePropertyAccess( bootPropertyDescriptor ),
						compositeType.getCascadeStyle( attributeIndex ),
						creationProcess
				);
				addAttribute( attributeMapping );
				columnPosition += columnSpan;
			}
			else {
				final EntityPersister entityPersister = creationProcess
						.getEntityPersister( bootDescriptor.getOwner().getEntityName() );
				if ( subtype instanceof CollectionType ) {
					PluralAttributeMapping attributeMapping = MappingModelCreationHelper.buildPluralAttributeMapping(
							bootPropertyDescriptor.getName(),
							attributeIndex,
							bootPropertyDescriptor,
							entityPersister,
							representationStrategy.resolvePropertyAccess( bootPropertyDescriptor ),
							compositeType.getCascadeStyle( attributeIndex ),
							compositeType.getFetchMode( attributeIndex ),
							creationProcess
					);
					addAttribute( attributeMapping );
				}
				else if ( subtype instanceof EntityType ) {
					final int columnSpan = subtype.getColumnSpan( sessionFactory );
					final ToOneAttributeMapping attributeMapping = MappingModelCreationHelper.buildSingularAssociationAttributeMapping(
							bootPropertyDescriptor.getName(),
							valueMapping.getNavigableRole().append( bootPropertyDescriptor.getName() ),
							attributeIndex,
							bootPropertyDescriptor,
							entityPersister,
							(EntityType) subtype,
							getRepresentationStrategy().resolvePropertyAccess( bootPropertyDescriptor ),
							compositeType.getCascadeStyle( attributeIndex ),
							creationProcess
					);
					addAttribute( attributeMapping );
					columnPosition += columnSpan;
				}
			}

			attributeIndex++;
		}

		// We need the attribute mapping types to finish initialization first before we can build the column mappings
		creationProcess.registerInitializationCallback(
				"EmbeddableMappingType(" + embeddedRole + ")#initColumnMappings",
				this::initColumnMappings
		);
		return true;
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

	private boolean initColumnMappings() {
		this.selectionMappings = SelectionMappingsImpl.from( this );
		return true;
	}

	private void addAttribute(AttributeMapping attributeMapping) {
		if ( attributeMap.put( attributeMapping.getAttributeName(), attributeMapping ) == null ) {
			attributes.add( attributeMapping );
		}
		else {
			for ( ListIterator<AttributeMapping> iterator = attributes.listIterator(); iterator.hasNext(); ) {
				final AttributeMapping existingMapping = iterator.next();
				if ( existingMapping.getAttributeName().equals( attributeMapping.getAttributeName() ) ) {
					iterator.set( attributeMapping );
					break;
				}
			}
		}
	}

	public EmbeddableValuedModelPart getEmbeddedValueMapping() {
		return valueMapping;
	}

	@Override
	public JavaTypeDescriptor getMappedJavaTypeDescriptor() {
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
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public int getNumberOfFetchables() {
		return attributeMap.size();
	}

	@Override
	public void visitFetchables(
			Consumer<Fetchable> fetchableConsumer,
			EntityMappingType treatTargetType) {
		visitAttributeMappings( attributeMapping -> fetchableConsumer.accept( (Fetchable) attributeMapping ) );
	}

	@Override
	public SelectionMapping getSelectionMapping(int columnIndex) {
		return selectionMappings.getSelectionMapping( columnIndex );
	}

	@Override
	public int getJdbcTypeCount() {
		return selectionMappings.getJdbcTypeCount();
	}

	@Override
	public List<JdbcMapping> getJdbcMappings() {
		return selectionMappings.getJdbcMappings();
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return selectionMappings.forEachSelection(
				offset,
				(selectionIndex, selectionMapping) -> {
					action.accept( selectionIndex, selectionMapping.getJdbcMapping() );
				}
		);
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		Object[] result = new Object[attributes.size()];
		for ( int i = 0; i < attributes.size(); i++ ) {
			AttributeMapping mapping = attributes.get( i );
			Object o = mapping.getPropertyAccess().getGetter().get( value );
			result[i] = mapping.disassemble( o, session );
		}
		return result;
	}

	@Override
	public int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer consumer,
			SharedSessionContractImplementor session) {
		int span = 0;
		for ( int i = 0; i < attributes.size(); i++ ) {
			final AttributeMapping attributeMapping = attributes.get( i );
			final Object o = attributeMapping.getPropertyAccess().getGetter().get( value );
			if ( attributeMapping instanceof ToOneAttributeMapping ) {
				final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) attributeMapping;
				span += toOneAttributeMapping.getForeignKeyDescriptor().forEachJdbcValue(
						toOneAttributeMapping.getAssociatedEntityMappingType()
								.getIdentifierMapping()
								.getIdentifier( o, session ),
						clause,
						span + offset,
						consumer,
						session
				);
			}
			else {
				span += attributeMapping.forEachJdbcValue( o, clause, span + offset, consumer, session );
			}
		}
		return span;
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		final Object[] values = (Object[]) value;
		int span = 0;
		for ( int i = 0; i < attributes.size(); i++ ) {
			final AttributeMapping mapping = attributes.get( i );
			span += mapping.forEachDisassembledJdbcValue( values[i], clause, span + offset, valuesConsumer, session );
		}
		return span;
	}

	@Override
	public int forEachSelection(SelectionConsumer consumer) {
		return selectionMappings.forEachSelection( 0, consumer );
	}

	@Override
	public int forEachSelection(int offset, SelectionConsumer consumer) {
		return selectionMappings.forEachSelection( offset, consumer );
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return valueMapping.findContainingEntityMapping();
	}

	@Override
	public int getNumberOfAttributeMappings() {
		return attributeMap.size();
	}

	@Override
	public AttributeMapping findAttributeMapping(String name) {
		return attributeMap.get( name );
	}

	@Override
	public List<AttributeMapping> getAttributeMappings() {
		return attributes;
	}

	@Override
	public void forEachAttributeMapping(IndexedConsumer<AttributeMapping> consumer) {
		for ( int i = 0; i < attributes.size(); i++ ) {
			consumer.accept( i, attributes.get( i ) );
		}
	}

	@Override
	public void visitAttributeMappings(Consumer<AttributeMapping> action) {
		attributes.forEach( action );
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		return attributeMap.get( name );
	}

	@Override
	public void visitSubParts(
			Consumer<ModelPart> consumer,
			EntityMappingType treatTargetType) {
		visitAttributeMappings( consumer::accept );
	}

	public void setPropertyValues(Object compositeInstance, Object[] resolvedValues) {
		// todo (6.0) : reflection optimizer...
		for ( int i = 0; i < attributes.size(); i++ ) {
			attributes.get( i )
					.getAttributeMetadataAccess()
					.resolveAttributeMetadata( null )
					.getPropertyAccess()
					.getSetter()
					.set( compositeInstance, resolvedValues[i], sessionFactory );
		}
	}

	public boolean isCreateEmptyCompositesEnabled() {
		return createEmptyCompositesEnabled;
	}
}
