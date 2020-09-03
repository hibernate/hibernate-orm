/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.metamodel.mapping.internal.DiscriminatedAssociationAttributeMapping;
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
public class EmbeddableMappingType implements ManagedMappingType {

	public static EmbeddableMappingType from(
			Component bootDescriptor,
			CompositeType compositeType,
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
				"EmbeddableMappingType#finishInitialization",
				() -> mappingType.finishInitialization(
						bootDescriptor,
						compositeType,
						creationProcess
				)
		);

		return mappingType;
	}

	public static EmbeddableMappingType from(
			Component bootDescriptor,
			CompositeType compositeType,
			NavigableRole embeddedRole,
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
				"EmbeddableMappingType(" + embeddedRole + ")#finishInitialization",
				() -> mappingType.finishInitialization(
						bootDescriptor,
						compositeType,
						creationProcess
				)
		);

		return mappingType;
	}

	private final JavaTypeDescriptor<?> embeddableJtd;
	private final EmbeddableRepresentationStrategy representationStrategy;

	private final SessionFactoryImplementor sessionFactory;

//	private final Map<String,AttributeMapping> attributeMappings = new TreeMap<>();
	private final Map<String,AttributeMapping> attributeMappings = new LinkedHashMap<>();

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
			MappingModelCreationProcess creationProcess) {
		final SessionFactoryImplementor sessionFactory = creationProcess.getCreationContext().getSessionFactory();
		final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();

		final String containingTableExpression = valueMapping.getContainingTableExpression();
		final List<String> mappedColumnExpressions = valueMapping.getMappedColumnExpressions();

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

				final String mappedColumnExpression = mappedColumnExpressions.get( columnPosition++ );

				attributeMappings.put(
						bootPropertyDescriptor.getName(),
						MappingModelCreationHelper.buildBasicAttributeMapping(
								bootPropertyDescriptor.getName(),
								valueMapping.getNavigableRole().append( bootPropertyDescriptor.getName() ),
								attributeIndex,
								bootPropertyDescriptor,
								this,
								(BasicType<?>) subtype,
								containingTableExpression,
								mappedColumnExpression,
								false,
								selectable.getCustomReadExpression(),
								selectable.getCustomWriteExpression(),
								representationStrategy.resolvePropertyAccess( bootPropertyDescriptor ),
								compositeType.getCascadeStyle( attributeIndex ),
								creationProcess
						)
				);
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

				attributeMappings.put(
						bootPropertyDescriptor.getName(),
						new DiscriminatedAssociationAttributeMapping(
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
						)
				);
			}
			else if ( subtype instanceof CompositeType ) {
				final CompositeType subCompositeType = (CompositeType) subtype;
				final int columnSpan = subCompositeType.getColumnSpan( sessionFactory );

				final List<String> customReadExpressions = new ArrayList<>( columnSpan );
				final List<String> customWriteExpressions = new ArrayList<>( columnSpan );

				final Iterator<Selectable> columnIterator = bootDescriptor.getColumnIterator();
				while ( columnIterator.hasNext() ) {
					final Selectable selectable = columnIterator.next();
					customReadExpressions.add( selectable.getCustomReadExpression() );
					customWriteExpressions.add( selectable.getCustomWriteExpression() );
				}

				attributeMappings.put(
						bootPropertyDescriptor.getName(),
						MappingModelCreationHelper.buildEmbeddedAttributeMapping(
								bootPropertyDescriptor.getName(),
								attributeIndex,
								bootPropertyDescriptor,
								this,
								subCompositeType,
								containingTableExpression,
								ArrayHelper.toStringArray( mappedColumnExpressions.subList( columnPosition, columnPosition + columnSpan ) ),
//								ArrayHelper.toStringArray( customReadExpressions.subList( columnPosition, columnPosition + columnSpan ) ),
//								ArrayHelper.toStringArray( customWriteExpressions.subList( columnPosition, columnPosition + columnSpan ) ),
								ArrayHelper.toStringArray( customReadExpressions ),
								ArrayHelper.toStringArray( customWriteExpressions ),
								representationStrategy.resolvePropertyAccess( bootPropertyDescriptor ),
								compositeType.getCascadeStyle( attributeIndex ),
								creationProcess
						)
				);

				columnPosition += columnSpan;
			}
			else {
				final EntityPersister entityPersister = creationProcess
						.getEntityPersister( bootDescriptor.getOwner().getEntityName() );
				if ( subtype instanceof CollectionType ) {
					attributeMappings.put(
							bootPropertyDescriptor.getName(),
							MappingModelCreationHelper.buildPluralAttributeMapping(
									bootPropertyDescriptor.getName(),
									attributeIndex,
									bootPropertyDescriptor,
									entityPersister,
									representationStrategy.resolvePropertyAccess( bootPropertyDescriptor ),
									compositeType.getCascadeStyle( attributeIndex),
									compositeType.getFetchMode( attributeIndex ),
									creationProcess
							)
					);
				}
				else if ( subtype instanceof EntityType ) {
					final ToOneAttributeMapping toOneAttributeMapping = MappingModelCreationHelper.buildSingularAssociationAttributeMapping(
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
					attributeMappings.put( bootPropertyDescriptor.getName(), toOneAttributeMapping );
					// todo (6.0) : not sure it is always correct
					columnPosition++;
				}
			}

			attributeIndex++;
		}

		return true;
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
		return attributeMappings.size();
	}

	@Override
	public void visitFetchables(
			Consumer<Fetchable> fetchableConsumer,
			EntityMappingType treatTargetType) {
		visitAttributeMappings( attributeMapping -> fetchableConsumer.accept( (Fetchable) attributeMapping ) );
	}

	private int cachedJdbcTypeCount = -1;

	@Override
	public int getJdbcTypeCount(TypeConfiguration typeConfiguration) {
		if ( cachedJdbcTypeCount == -1 ) {
			int count = 0;

			for ( AttributeMapping attributeMapping : getAttributeMappings() ) {
				count += attributeMapping.getJdbcTypeCount( typeConfiguration );
			}

			this.cachedJdbcTypeCount = count;
		}

		return cachedJdbcTypeCount;
	}

	private List<JdbcMapping> cachedJdbcMappings;

	@Override
	public List<JdbcMapping> getJdbcMappings(TypeConfiguration typeConfiguration) {
		if ( cachedJdbcMappings == null ) {
			final List<JdbcMapping> result = new ArrayList<>();
			visitJdbcTypes(
					result::add,
					Clause.IRRELEVANT,
					typeConfiguration
			);
			this.cachedJdbcMappings = Collections.unmodifiableList( result );
		}

		return cachedJdbcMappings;
	}

	@Override
	public void visitJdbcTypes(
			Consumer<JdbcMapping> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		attributeMappings.forEach(
				(s, attributeMapping) -> {
					if ( attributeMapping instanceof PluralAttributeMapping ) {
						return;
					}
					if ( attributeMapping instanceof ToOneAttributeMapping ) {
						( (ToOneAttributeMapping) attributeMapping ).getKeyTargetMatchPart().visitJdbcTypes(
								action,
								clause,
								typeConfiguration
						);
					}
					else {
						attributeMapping.visitJdbcTypes( action, clause, typeConfiguration );
					}
				}
		);
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		final Collection<AttributeMapping> attributeMappings = getAttributeMappings();

		Object[] result = new Object[attributeMappings.size()];
		int i = 0;
		final Iterator<AttributeMapping> iterator = attributeMappings.iterator();
		while ( iterator.hasNext() ) {
			AttributeMapping mapping = iterator.next();
			Object o = mapping.getPropertyAccess().getGetter().get( value );
			result[i] = mapping.disassemble( o, session );
			i++;
		}
		return result;
	}

	@Override
	public void visitJdbcValues(
			Object value,
			Clause clause,
			JdbcValuesConsumer consumer,
			SharedSessionContractImplementor session) {
		attributeMappings.forEach(
				(s, attributeMapping) -> {
					Object o = attributeMapping.getPropertyAccess().getGetter().get( value );
					attributeMapping.visitJdbcValues( o, clause, consumer, session );
				}
		);
	}

	@Override
	public void visitDisassembledJdbcValues(
			Object value,
			Clause clause,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		final Collection<AttributeMapping> attributeMappings = getAttributeMappings();
		final Iterator<AttributeMapping> iterator = attributeMappings.iterator();
		final Object[] values = (Object[]) value;
		int i = 0;
		while ( iterator.hasNext() ) {
			AttributeMapping mapping = iterator.next();
			mapping.visitDisassembledJdbcValues( values[i], clause, valuesConsumer, session );
			i++;
		}
	}

	public void visitColumns(ColumnConsumer consumer) {
		attributeMappings.values().forEach(
				attributeMapping -> attributeMapping.visitColumns( consumer )
		);
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
		return attributeMappings.get( name );
	}

	@Override
	public Collection<AttributeMapping> getAttributeMappings() {
		return attributeMappings.values();
	}

	@Override
	public void visitAttributeMappings(Consumer<AttributeMapping> action) {
		attributeMappings.values().forEach( action );
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		return attributeMappings.get( name );
	}

	@Override
	public void visitSubParts(
			Consumer<ModelPart> consumer,
			EntityMappingType treatTargetType) {
		visitAttributeMappings( consumer::accept );
	}

	public void setPropertyValues(Object compositeInstance, Object[] resolvedValues) {
		// todo (6.0) : reflection optimizer...

		visitAttributeMappings(
				new Consumer<AttributeMapping>() {
					private int i = 0;

					@Override
					public void accept(AttributeMapping attributeMapping) {
						attributeMapping.getAttributeMetadataAccess()
								.resolveAttributeMetadata( null )
								.getPropertyAccess()
								.getSetter()
								.set( compositeInstance, resolvedValues[i++], sessionFactory );
					}
				}
		);
	}

	public boolean isCreateEmptyCompositesEnabled() {
		return createEmptyCompositesEnabled;
	}
}
