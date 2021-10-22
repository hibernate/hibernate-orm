/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.SharedSessionContract;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
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
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.mapping.internal.DiscriminatedAssociationAttributeMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.SelectableMappingsImpl;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlSelection;
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
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.spi.CompositeTypeImplementor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class EmbeddableMappingType implements ManagedMappingType, SelectableMappings {

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

		if ( compositeType instanceof CompositeTypeImplementor ) {
			( (CompositeTypeImplementor) compositeType ).injectMappingModelPart( mappingType.getEmbeddedValueMapping(), creationProcess );
		}

		creationProcess.registerInitializationCallback(
				"EmbeddableMappingType(" + mappingType.getNavigableRole().getFullPath() + ")#finishInitialization",
				() -> {
					try {
						final boolean finished = mappingType.finishInitialization(
								bootDescriptor,
								compositeType,
								rootTableExpression,
								rootTableKeyColumnNames,
								creationProcess
						);

						if ( finished ) {
							return finished;
						}
					}
					catch (Exception e) {
						MappingModelCreationLogger.LOGGER.debugf(
								e,
								"(DEBUG) Error finalizing EmbeddableMappingType(%s)",
								mappingType.embeddedRole
						);
					}

					MappingModelCreationLogger.LOGGER.debugf(
							"EmbeddableMappingType(%s) finalization was not able to complete successfully",
							mappingType.embeddedRole
					);
					return false;
				}
		);

		return mappingType;
	}

	private final JavaType<?> embeddableJtd;
	private final EmbeddableRepresentationStrategy representationStrategy;

	private final SessionFactoryImplementor sessionFactory;

	private final List<AttributeMapping> attributeMappings = new ArrayList<>();
	private SelectableMappings selectableMappings;

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

	private EmbeddableMappingType(
			EmbeddableValuedModelPart valueMapping,
			SelectableMappings selectableMappings,
			EmbeddableMappingType inverseMappingType,
			MappingModelCreationProcess creationProcess) {
		this.embeddableJtd = inverseMappingType.embeddableJtd;
		this.representationStrategy = inverseMappingType.representationStrategy;
		this.sessionFactory = inverseMappingType.sessionFactory;
		this.valueMapping = valueMapping;
		this.createEmptyCompositesEnabled = inverseMappingType.isCreateEmptyCompositesEnabled();
		this.selectableMappings = selectableMappings;
		creationProcess.registerInitializationCallback(
				"EmbeddableMappingType(" + inverseMappingType.getNavigableRole().getFullPath() + ".{inverse})#finishInitialization",
				() -> {
					if ( inverseMappingType.attributeMappings.isEmpty() ) {
						return false;
					}
					// Reset the attribute mappings that were added in previous attempts
					this.attributeMappings.clear();
					int currentIndex = 0;
					// We copy the attributes from the inverse mappings and replace the selection mappings
					for ( AttributeMapping attributeMapping : inverseMappingType.attributeMappings ) {
						if ( attributeMapping instanceof BasicAttributeMapping ) {
							final BasicAttributeMapping original = (BasicAttributeMapping) attributeMapping;
							final SelectableMapping selectableMapping = selectableMappings.getSelectable( currentIndex );
							attributeMapping = BasicAttributeMapping.withSelectableMapping(
									original,
									original.getPropertyAccess(),
									original.getValueGeneration(),
									selectableMapping
							);
							currentIndex++;
						}
						else if ( attributeMapping instanceof ToOneAttributeMapping ) {
							final ToOneAttributeMapping original = (ToOneAttributeMapping) attributeMapping;
							final ToOneAttributeMapping toOne = original.copy();
							final int offset = currentIndex;
							toOne.setIdentifyingColumnsTableExpression(
									selectableMappings.getSelectable( offset ).getContainingTableExpression()
							);
							toOne.setForeignKeyDescriptor(
									original.getForeignKeyDescriptor().withKeySelectionMapping(
											index -> selectableMappings.getSelectable( offset + index ),
											creationProcess
									)
							);

							attributeMapping = toOne;
							currentIndex += attributeMapping.getJdbcTypeCount();
						}
						else {
							throw new UnsupportedOperationException(
									"Only basic and to-one attributes are supported in composite fks" );
						}
						this.attributeMappings.add( attributeMapping );
					}
					return true;
				}
		);
	}

	public EmbeddableMappingType createInverseMappingType(
			EmbeddableValuedModelPart valueMapping,
			SelectableMappings selectableMappings,
			MappingModelCreationProcess creationProcess) {
		return new EmbeddableMappingType( valueMapping, selectableMappings, this, creationProcess );
	}

	private boolean finishInitialization(
			Component bootDescriptor,
			CompositeType compositeType,
			String rootTableExpression,
			String[] rootTableKeyColumnNames,
			MappingModelCreationProcess creationProcess) {
		final SessionFactoryImplementor sessionFactory = creationProcess.getCreationContext().getSessionFactory();
		final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final Dialect dialect = jdbcEnvironment.getDialect();

		final String baseTableExpression = valueMapping.getContainingTableExpression();
		final Type[] subtypes = compositeType.getSubtypes();

		int attributeIndex = 0;
		int columnPosition = 0;

		// Reset the attribute mappings that were added in previous attempts
		this.attributeMappings.clear();

		final Iterator<Property> propertyIterator = bootDescriptor.getPropertyIterator();
		while ( propertyIterator.hasNext() ) {
			final Property bootPropertyDescriptor = propertyIterator.next();
			final AttributeMapping attributeMapping;

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
								jdbcEnvironment
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

				attributeMapping = MappingModelCreationHelper.buildBasicAttributeMapping(
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
						public Serializable disassemble(Object value, SharedSessionContract session) {
							throw new NotYetImplementedFor6Exception( getClass() );
						}

						@Override
						public Object assemble(Serializable cached, SharedSessionContract session) {
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

				attributeMapping = new DiscriminatedAssociationAttributeMapping(
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

				attributeMapping = MappingModelCreationHelper.buildEmbeddedAttributeMapping(
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

				columnPosition += columnSpan;
			}
			else if ( subtype instanceof CollectionType ) {
				final EntityPersister entityPersister = creationProcess.getEntityPersister( bootDescriptor.getOwner().getEntityName() );

				attributeMapping = MappingModelCreationHelper.buildPluralAttributeMapping(
						bootPropertyDescriptor.getName(),
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
				"EmbeddableMappingType(" + embeddedRole + ")#initColumnMappings",
				this::initColumnMappings
		);
		return true;
	}

	private static String getTableIdentifierExpression(Table table, JdbcEnvironment jdbcEnvironment) {
		return jdbcEnvironment
				.getQualifiedObjectNameFormatter().format(
						table.getQualifiedTableName(),
						jdbcEnvironment.getDialect()
				);
	}

	private boolean initColumnMappings() {
		this.selectableMappings = SelectableMappingsImpl.from( this );
		return true;
	}

	private void addAttribute(AttributeMapping attributeMapping) {
		// check if we've already seen this attribute...
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final AttributeMapping previous = attributeMappings.get( i );
			if ( attributeMapping.getAttributeName().equals( previous.getAttributeName() ) ) {
				attributeMappings.set( i, attributeMapping );
				return;
			}
		}

		attributeMappings.add( attributeMapping );
	}

	public EmbeddableValuedModelPart getEmbeddedValueMapping() {
		return valueMapping;
	}

	@Override
	public JavaType getMappedJavaTypeDescriptor() {
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
		visitAttributeMappings(
				attributeMapping -> attributeMapping.applySqlSelections( navigablePath, tableGroup, creationState )
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		visitAttributeMappings(
				attributeMapping ->
						attributeMapping.applySqlSelections(
								navigablePath,
								tableGroup,
								creationState,
								selectionConsumer
						)
		);
	}

	@Override
	public int getNumberOfFetchables() {
		return attributeMappings.size();
	}

	@Override
	public void visitFetchables(
			Consumer<Fetchable> fetchableConsumer,
			EntityMappingType treatTargetType) {
		visitAttributeMappings( attributeMapping -> fetchableConsumer.accept( (Fetchable) attributeMapping ) );
	}

	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		return selectableMappings.getSelectable( columnIndex );
	}

	@Override
	public int getJdbcTypeCount() {
		return selectableMappings.getJdbcTypeCount();
	}

	@Override
	public List<JdbcMapping> getJdbcMappings() {
		return selectableMappings.getJdbcMappings();
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return selectableMappings.forEachSelectable(
				offset,
				(index, selectable) -> action.accept( index, selectable.getJdbcMapping() )
		);
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		if ( domainValue instanceof Object[] ) {
			final Object[] values = (Object[]) domainValue;
			assert values.length == attributeMappings.size();

			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final AttributeMapping attributeMapping = attributeMappings.get( i );
				final Object attributeValue = values[ i ];
				attributeMapping.breakDownJdbcValues( attributeValue, valueConsumer, session );
			}
		}
		else {
			attributeMappings.forEach(
					(attributeMapping) -> {
						final Object attributeValue = attributeMapping.getPropertyAccess().getGetter().get( domainValue );
						attributeMapping.breakDownJdbcValues( attributeValue, valueConsumer, session );
					}
			);
		}
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		final List<AttributeMapping> attributeMappings = getAttributeMappings();

		final Object[] result = new Object[ attributeMappings.size() ];
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final AttributeMapping attributeMapping = attributeMappings.get( i );
			Object o = attributeMapping.getPropertyAccess().getGetter().get( value );
			result[i] = attributeMapping.disassemble( o, session );
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

		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final AttributeMapping attributeMapping = attributeMappings.get( i );
			if ( attributeMapping instanceof PluralAttributeMapping ) {
				continue;
			}
			final Object o = attributeMapping.getPropertyAccess().getGetter().get( value );
			span += attributeMapping.forEachJdbcValue( o, clause, span + offset, consumer, session );
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
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final AttributeMapping mapping = attributeMappings.get( i );
			span += mapping.forEachDisassembledJdbcValue( values[i], clause, span + offset, valuesConsumer, session );
		}
		return span;
	}

	@Override
	public int forEachSelectable(SelectableConsumer consumer) {
		return selectableMappings.forEachSelectable( 0, consumer );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		return selectableMappings.forEachSelectable( offset, consumer );
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return valueMapping.findContainingEntityMapping();
	}

	@Override
	public int getNumberOfAttributeMappings() {
		return attributeMappings.size();
	}

	@Override
	public AttributeMapping findAttributeMapping(String name) {
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final AttributeMapping attr = attributeMappings.get( i );
			if ( name.equals( attr.getAttributeName() ) ) {
				return attr;
			}
		}
		return null;
	}

	@Override
	public List<AttributeMapping> getAttributeMappings() {
		return attributeMappings;
	}

	@Override
	public void forEachAttributeMapping(IndexedConsumer<AttributeMapping> consumer) {
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			consumer.accept( i, attributeMappings.get( i ) );
		}
	}

	@Override
	public void visitAttributeMappings(Consumer<? super AttributeMapping> action) {
		attributeMappings.forEach( action );
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		return findAttributeMapping( name );
	}

	@Override
	public void visitSubParts(
			Consumer<ModelPart> consumer,
			EntityMappingType treatTargetType) {
		visitAttributeMappings( consumer::accept );
	}

	public Object[] getPropertyValues(Object compositeInstance) {
		final Object[] results = new Object[attributeMappings.size()];
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final StateArrayContributorMapping attr = (StateArrayContributorMapping) attributeMappings.get( i );
			final Getter getter = attr.getAttributeMetadataAccess()
					.resolveAttributeMetadata( null )
					.getPropertyAccess()
					.getGetter();
			results[ attr.getStateArrayPosition() ] = getter.get( compositeInstance );
		}
		return results;
	}

	public void setPropertyValues(Object compositeInstance, Object[] resolvedValues) {
		// todo (6.0) : reflection optimizer...
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			attributeMappings.get( i )
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
