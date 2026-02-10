/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;

import static org.hibernate.action.internal.EntityIdentityInsertAction.defaultedPrimitiveIds;
import static org.hibernate.generator.EventType.INSERT;

/**
 * For composite identifiers, defines a number of "nested" generations that
 * need to happen to "fill" the identifier property(s).
 * <p>
 * This generator is used implicitly for all composite identifier scenarios if an
 * explicit generator is not in place.  So it make sense to discuss the various
 * potential scenarios:<ul>
 * <li>
 * <i>"embedded" composite identifier</i> - this is possible only in HBM mappings
 * as {@code <composite-id/>} (notice the lack of both a name and class attribute
 * declarations).  The term {@link org.hibernate.mapping.Component#isEmbedded() "embedded"}
 * here refers to the Hibernate usage which is actually the exact opposite of the JPA
 * meaning of "embedded".  Essentially this means that the entity class itself holds
 * the named composite pk properties.  This is very similar to the JPA {@code @IdClass}
 * usage, though without a separate pk-class for loading.
 * </li>
 * <li>
 * <i>pk-class as entity attribute</i> - this is possible in both annotations ({@code @EmbeddedId})
 * and HBM mappings ({@code <composite-id name="idAttributeName" class="PkClassName"/>})
 * </li>
 * <li>
 * <i>"embedded" composite identifier with a pk-class</i> - this is the JPA {@code @IdClass} use case
 * and is only possible in annotations
 * </li>
 * </ul>
 * <p>
 * Most of the grunt work is done in {@link org.hibernate.mapping.Component}.
 *
 * @author Steve Ebersole
 */
@Internal
public class CompositeNestedGeneratedValueGenerator
		implements IdentifierGenerator, IdentifierGeneratorAggregator, OnExecutionGenerator, Serializable {
	/**
	 * Contract for declaring how to locate the context for sub-value injection.
	 */
	public interface GenerationContextLocator {
		/**
		 * Given the incoming object, determine the context for injecting back its generated
		 * id sub-values.
		 *
		 * @param session The current session
		 * @param incomingObject The entity for which we are generating id
		 *
		 * @return The injection context
		 */
		Object locateGenerationContext(SharedSessionContractImplementor session, Object incomingObject);
	}

	/**
	 * Contract for performing the actual sub-value generation, usually injecting it into the
	 * determined {@linkplain GenerationContextLocator#locateGenerationContext context}
	 */
	public interface GenerationPlan extends ExportableProducer {
		/**
		 * Initializes this instance, in particular pre-generates SQL as necessary.
		 * <p>
		 * This method is called after {@link #registerExportables(Database)}, before first use.
		 *
		 * @param context A context to help generate SQL strings
		 */
		void initialize(SqlStringGenerationContext context);

		/**
		 * Retrieve the generator for this generation plan
		 */
		BeforeExecutionGenerator getGenerator();

		/**
		 * Returns the {@link Setter injector} for the generated property.
		 * Used when the {@link CompositeType} is {@linkplain CompositeType#isMutable() mutable}.
		 *
		 * @see #getPropertyIndex()
		 */
		Setter getInjector();

		/**
		 * Returns the index of the generated property.
		 * Used when the {@link CompositeType} is not {@linkplain CompositeType#isMutable() mutable}.
		 *
		 * @see #getInjector()
		 */
		int getPropertyIndex();
	}

	private final GenerationContextLocator generationContextLocator;
	private final ComponentType componentType;
	private final List<GenerationPlan> generationPlans = new ArrayList<>();
	private final List<Generator> generators;
	private final String[] columnValues;
	private final boolean[] columnInclusions;
	private final boolean[] generatedOnExecutionColumns;
	private final boolean hasOnExecutionGenerators;
	private final boolean hasIncludedColumns;
	private final boolean needsValueBinding;
	private final boolean hasAssignedValues;

	public CompositeNestedGeneratedValueGenerator(
			GenerationContextLocator generationContextLocator,
			ComponentType componentType) {
		this( generationContextLocator, componentType,
				List.of(), null, null, null );
	}

	// Used by Hibernate Reactive
	public CompositeNestedGeneratedValueGenerator(
			GenerationContextLocator generationContextLocator,
			ComponentType componentType,
			List<GenerationPlan> generationPlans) {
		this( generationContextLocator, componentType,
				List.of(), null, null, null );
		this.generationPlans.addAll( generationPlans );
	}

	public CompositeNestedGeneratedValueGenerator(
			GenerationContextLocator generationContextLocator,
			ComponentType componentType,
			List<Generator> generators,
			String[] columnValues,
			boolean[] columnInclusions,
			boolean[] generatedOnExecutionColumns) {
		this.generationContextLocator = generationContextLocator;
		this.componentType = componentType;
		this.generators = generators == null ? List.of() : new ArrayList<>( generators );
		this.columnValues = columnValues;
		this.columnInclusions = columnInclusions;
		this.generatedOnExecutionColumns = generatedOnExecutionColumns;
		this.hasOnExecutionGenerators = hasOnExecutionGenerators( this.generators );
		this.hasIncludedColumns = hasIncludedColumns( columnInclusions );
		this.needsValueBinding = needsValueBinding( columnValues, columnInclusions );
		this.hasAssignedValues = generators.stream()
				.anyMatch( g -> g == null || g instanceof org.hibernate.generator.Assigned );
	}

	public void addGeneratedValuePlan(GenerationPlan plan) {
		generationPlans.add( plan );
	}

	public boolean hasAssignedValues() {
		return hasAssignedValues;
	}

	@Override
	public Object generate(SharedSessionContractImplementor session, Object object) {
		final Object context = generationContextLocator.locateGenerationContext( session, object );
		final Object result = context != null ? context : instantiateEmptyComposite();
		final var generatedValues = generatedValues( session, object, result );
		if ( generatedValues != null) {
			final var values = componentType.getPropertyValues( result );
			for ( int i = 0; i < generatedValues.size(); i++ ) {
				values[generationPlans.get( i ).getPropertyIndex()] = generatedValues.get( i );
			}
			return componentType.replacePropertyValues( result, values, session );
		}
		else {
			return result;
		}
	}

	private Object instantiateEmptyComposite() {
		final var mappingModelPart = componentType.getMappingModelPart();
		final var embeddable = mappingModelPart.getEmbeddableTypeDescriptor();
		return embeddable.getRepresentationStrategy().getInstantiator()
				.instantiate( () -> defaultedPrimitiveIds( embeddable, embeddable ) );
	}

	private List<Object> generatedValues(SharedSessionContractImplementor session, Object object, Object context) {
		final boolean mutable = componentType.isMutable();
		final List<Object> generatedValues = mutable ? null : new ArrayList<>( generationPlans.size() );
		for ( var generationPlan : generationPlans ) {
			final Object value = generatedValue( session, object, context, generationPlan, mutable );
			if ( !mutable ) {
				generatedValues.add( value );
			}
		}
		return generatedValues;
	}

	private Object generatedValue(
			SharedSessionContractImplementor session,
			Object object,
			Object context,
			GenerationPlan generationPlan,
			boolean inject) {
		final var generator = generationPlan.getGenerator();
		final Object existingValue =
				componentType.getPropertyValue( context, generationPlan.getPropertyIndex(), session );
		if ( generator.generatedBeforeExecution( object, session ) ) {
			final Object currentValue = generator.allowAssignedIdentifiers() ? existingValue : null;
			final Object generatedValue = generator.generate( session, object, currentValue, INSERT );
			if ( inject ) {
				generationPlan.getInjector().set( context, generatedValue );
			}
			return generatedValue;
		}
		else {
			return existingValue;
		}
	}

	@Override
	public void registerExportables(Database database) {
		for ( var plan : generationPlans ) {
			plan.registerExportables( database );
		}
	}

	@Override
	public void initialize(SqlStringGenerationContext context) {
		for ( var plan : generationPlans ) {
			plan.initialize( context );
		}
	}

	@Override
	public boolean generatedOnExecution() {
		return hasOnExecutionGenerators;
	}

	@Override
	public boolean generatedOnExecution(Object entity, SharedSessionContractImplementor session) {
		for ( var generator : generators ) {
			if ( generator instanceof OnExecutionGenerator
					&& generator.generatesOnInsert()
					&& generator.generatedOnExecution( entity, session ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean generatedBeforeExecution(Object entity, SharedSessionContractImplementor session) {
		for ( var generator : generators ) {
			if ( generator instanceof BeforeExecutionGenerator
					&& generator.generatesOnInsert()
					&& generator.generatedBeforeExecution( entity, session ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean referenceColumnsInSql(Dialect dialect) {
		return hasIncludedColumns;
	}

	@Override
	public boolean referenceColumnsInSql(Dialect dialect, EventType eventType) {
		return eventType == EventType.INSERT && hasIncludedColumns;
	}

	@Override
	public boolean writePropertyValue() {
		return needsValueBinding;
	}

	@Override
	public boolean writePropertyValue(EventType eventType) {
		return eventType == EventType.INSERT && needsValueBinding;
	}

	@Override
	public InsertGeneratedIdentifierDelegate getGeneratedIdentifierDelegate(EntityPersister persister) {
		for ( var generator : generators ) {
			if ( generator instanceof PostInsertIdentifierGenerator postInsertIdentifierGenerator ) {
				final var delegate = postInsertIdentifierGenerator.getGeneratedIdentifierDelegate( persister );
				if ( delegate != null ) {
					return delegate;
				}
			}
		}
		return OnExecutionGenerator.super.getGeneratedIdentifierDelegate( persister );
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect) {
		return columnValues;
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect, EventType eventType) {
		return eventType == EventType.INSERT ? columnValues : null;
	}

	@Override
	public boolean[] getColumnInclusions(Dialect dialect, EventType eventType) {
		return eventType == EventType.INSERT ? columnInclusions : null;
	}

	// Used by Hibernate Reactive
	public List<GenerationPlan> getGenerationPlans() {
		return generationPlans;
	}

	// Used by Hibernate Reactive
	public GenerationContextLocator getGenerationContextLocator() {
		return generationContextLocator;
	}

	// Used by Hibernate Reactive
	public CompositeType getComponentType() {
		return componentType;
	}

	public boolean[] getGeneratedOnExecutionColumnInclusions() {
		return generatedOnExecutionColumns;
	}

	private static boolean hasOnExecutionGenerators(List<Generator> generators) {
		for ( var generator : generators ) {
			if ( generator instanceof OnExecutionGenerator
					&& generator.generatesOnInsert() ) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasIncludedColumns(boolean[] columnInclusions) {
		if ( columnInclusions != null ) {
			for ( boolean columnInclusion : columnInclusions ) {
				if ( columnInclusion ) {
					return true;
				}
			}
		}
		return false;
	}


	private static boolean needsValueBinding(String[] columnValues, boolean[] columnInclusions) {
		if ( columnValues != null && columnInclusions != null ) {
			for ( int i = 0; i < columnValues.length; i++ ) {
				if ( columnInclusions[i] && "?".equals( columnValues[i] ) ) {
					return true;
				}
			}
		}
		return false;
	}
}
