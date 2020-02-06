/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

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
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.SingularAssociationAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableResultImpl;
import org.hibernate.type.BasicType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
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
				() -> mappingType.finishInitialization(
						bootDescriptor,
						compositeType,
						creationProcess
				)
		);

		return mappingType;
	}

	private final JavaTypeDescriptor embeddableJtd;
	private final EmbeddableRepresentationStrategy representationStrategy;

	private final SessionFactoryImplementor sessionFactory;

//	private final Map<String,AttributeMapping> attributeMappings = new TreeMap<>();
	private final Map<String,AttributeMapping> attributeMappings = new LinkedHashMap<>();

	private final EmbeddableValuedModelPart valueMapping;

	private final boolean createEmptyCompositesEnabled;

	private EmbeddableMappingType(
			@SuppressWarnings("unused") Component bootDescriptor,
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
				attributeMappings.put(
						bootPropertyDescriptor.getName(),
						MappingModelCreationHelper.buildBasicAttributeMapping(
								bootPropertyDescriptor.getName(),
								attributeIndex,
								bootPropertyDescriptor,
								this,
								(BasicType) subtype,
								containingTableExpression,
								mappedColumnExpressions.get( columnPosition++ ),
								representationStrategy.resolvePropertyAccess( bootPropertyDescriptor ),
								compositeType.getCascadeStyle( attributeIndex ),
								creationProcess
						)
				);
			}
			else if ( subtype instanceof CompositeType ) {
				final CompositeType subCompositeType = (CompositeType) subtype;
				final int columnSpan = subCompositeType.getColumnSpan( creationProcess.getCreationContext().getSessionFactory() );

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
					final SingularAssociationAttributeMapping singularAssociationAttributeMapping = MappingModelCreationHelper.buildSingularAssociationAttributeMapping(
							bootPropertyDescriptor.getName(),
							attributeIndex,
							bootPropertyDescriptor,
							entityPersister,
							(EntityType) subtype,
							getRepresentationStrategy().resolvePropertyAccess( bootPropertyDescriptor ),
							compositeType.getCascadeStyle( attributeIndex ),
							creationProcess
					);
					attributeMappings.put( bootPropertyDescriptor.getName(), singularAssociationAttributeMapping );
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
				(s, attributeMapping) -> attributeMapping.visitJdbcTypes( action, clause, typeConfiguration )
		);
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void visitDisassembledJdbcValues(
			Object value,
			Clause clause,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
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
