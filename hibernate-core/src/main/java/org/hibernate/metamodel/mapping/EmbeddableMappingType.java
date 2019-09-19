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
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.sql.SqlAstCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.internal.domain.composite.CompositeResult;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetchable;
import org.hibernate.type.BasicType;
import org.hibernate.type.CompositeType;
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
				creationContext.getSessionFactory()
		);

		mappingType.valueMapping = embeddedPartBuilder.apply( mappingType );

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

	private final SortedMap<String,AttributeMapping> attributeMappings = new TreeMap<>();

	/**
	 * This is logically final.  However given the chicken-and-egg situation between
	 * EmbeddableMappingType and EmbeddableValuedModelPart one side will need to be
	 * physically non-final
	 */
	private EmbeddableValuedModelPart valueMapping;

	public EmbeddableMappingType(
			Component bootDescriptor,
			EmbeddableRepresentationStrategy representationStrategy,
			SessionFactoryImplementor sessionFactory) {
		this.embeddableJtd = representationStrategy.getMappedJavaTypeDescriptor();
		this.representationStrategy = representationStrategy;
		this.sessionFactory = sessionFactory;
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

			if ( subtypes[ attributeIndex ] instanceof BasicType ) {
				attributeMappings.put(
						bootPropertyDescriptor.getName(),
						MappingModelCreationHelper.buildBasicAttributeMapping(
								bootPropertyDescriptor.getName(),
								attributeIndex,
								bootPropertyDescriptor,
								this,
								(BasicType) subtypes[ attributeIndex ],
								containingTableExpression,
								mappedColumnExpressions.get( columnPosition++ ),
								representationStrategy.resolvePropertyAccess( bootPropertyDescriptor ),
								compositeType.getCascadeStyle( attributeIndex ),
								creationProcess
						)
				);
			}
			else if ( subtypes[ attributeIndex ] instanceof CompositeType ) {
				final CompositeType subCompositeType = (CompositeType) subtypes[ attributeIndex ];
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
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new CompositeResult<>(
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
		visitAttributeMappings(
				attributeMapping -> {
					attributeMapping.visitJdbcTypes( action, clause, typeConfiguration );
				}
		);
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {

		return null;
	}

	@Override
	public void visitDisassembledJdbcValues(
			Object value, Clause clause, JdbcValuesConsumer valuesConsumer, SharedSessionContractImplementor session) {

	}

	@Override
	public void visitJdbcValues(
			Object value, Clause clause, JdbcValuesConsumer valuesConsumer, SharedSessionContractImplementor session) {

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
	public boolean isTypeOrSuperType(ManagedMappingType targetType) {
		return targetType == null || targetType == this;
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

	@Override
	public TableGroup prepareAsLhs(
			NavigablePath navigablePath,
			SqlAstCreationState creationState) {
		return getEmbeddedValueMapping().prepareAsLhs( navigablePath, creationState );
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
}
