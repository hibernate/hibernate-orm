package org.hibernate.generator.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.GenerationRequest;
import org.hibernate.generator.GenerationRequests;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.type.ComponentType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

import static java.lang.System.arraycopy;
import static org.hibernate.action.internal.DelayableEntityInsertAction.defaultedPrimitiveIds;
import static org.hibernate.generator.EventTypeSets.NONE;

/**
 * Handles value generation for composite properties.
 */
@Internal
public class CompositeGeneratorBuilder {
	/**
	 * Access to the generators of embeddable attributes in mapping order.
	 */
	@Internal
	public interface CompositeGenerator extends Generator {
		List<Generator> generators();
	}

	private final String entityName;
	private final Property mappingProperty;
	private final Dialect dialect;

	private boolean hadBeforeExecutionGeneration;
	private boolean hadOnExecutionGeneration;

	private final List<Generator> generators = new ArrayList<>();

	public CompositeGeneratorBuilder(String entityName, Property mappingProperty, Dialect dialect) {
		this.entityName = entityName;
		this.mappingProperty = mappingProperty;
		this.dialect = dialect;
	}

	public void add(Generator generator) {
		generators.add( generator );

		if ( generator != null && generator.generatesSometimes() ) {
			if ( generator instanceof OnExecutionGenerator
					&& generator.generatedOnExecution() ) {
				hadOnExecutionGeneration = true;
			}
			if ( generator instanceof BeforeExecutionGenerator ) {
				hadBeforeExecutionGeneration = true;
			}
		}
	}

	public Generator build() {
		if ( hadBeforeExecutionGeneration && hadOnExecutionGeneration ) {
			return createCompositeMixedTimingGenerator();
		}
		else if ( hadBeforeExecutionGeneration ) {
			return createCompositeBeforeExecutionGenerator();
		}
		else if ( hadOnExecutionGeneration ) {
			return createCompositeOnExecutionGenerator();
		}
		else {
			return DummyGenerator.INSTANCE;
		}
	}

	private Generator createCompositeMixedTimingGenerator() {
		final var onExecutionGenerator = createCompositeOnExecutionGenerator();
		final var beforeExecutionGenerator = createCompositeBeforeExecutionGenerator();
		final var eventTypes = EnumSet.noneOf( EventType.class );
		eventTypes.addAll( onExecutionGenerator.getEventTypes() );
		eventTypes.addAll( beforeExecutionGenerator.getEventTypes() );
		return new CompositeOnAndBeforeExecutionGenerator(
				eventTypes,
				generators,
				onExecutionGenerator,
				beforeExecutionGenerator
		);
	}

	private CompositeOnExecutionGenerator createCompositeOnExecutionGenerator() {
		final var composite = (Component) mappingProperty.getValue();
		final var properties = composite.getProperties();
		final int columnSpan = composite.getColumnSpan();

		final var eventTypes = EnumSet.noneOf( EventType.class );
		for ( int i = 0; i < properties.size(); i++ ) {
			final var generator = generators.get( i );
			if ( generator != null && generator.generatesSometimes() ) {
				eventTypes.addAll( generator.getEventTypes() );
			}
		}

		final var columnValueDetailsByEvent =
				new EnumMap<EventType, ColumnValueDetails>( EventType.class );
		for ( var eventType : eventTypes ) {
			columnValueDetailsByEvent.put( eventType,
					new ColumnValueDetails( columnSpan ) );
		}

		boolean mutable = false;
		int columnIndex = 0;
		for ( int i = 0; i < properties.size(); i++ ) {
			final var property = properties.get( i );
			final int span = property.getColumnSpan();
			final var generator = generators.get( i );
			if ( generator != null ) {
				mutable = mutable || generator.allowMutation();
			}
			if ( generator instanceof OnExecutionGenerator onExecutionGenerator
					&& generator.generatedOnExecution() ) {
				final var generatorEventTypes = generator.getEventTypes();
				for ( var eventType : eventTypes ) {
					final var details = columnValueDetailsByEvent.get( eventType );
					if ( details != null ) {
						if ( generatorEventTypes.contains( eventType ) ) {
							if ( !onExecutionGenerator.referenceColumnsInSql( dialect, eventType ) ) {
								details.excludeColumns( columnIndex, span );
							}
							else if ( onExecutionGenerator.writePropertyValue( eventType ) ) {
								// leave the default parameter marker values in place
							}
							else {
								final String[] referencedColumnValues =
										onExecutionGenerator.getReferencedColumnValues( dialect, eventType );
								if ( referencedColumnValues == null ) {
									throw new CompositeValueGenerationException(
											"Generated column values were not provided for composite attribute: "
											+ mappingProperty.getName() + '.' + property.getName()
									);
								}
								if ( referencedColumnValues.length != span ) {
									throw new CompositeValueGenerationException(
											"Mismatch between number of collected generated column values and number of columns for composite attribute: "
											+ mappingProperty.getName() + '.' + property.getName()
									);
								}
								details.setColumnValues( columnIndex, referencedColumnValues );
							}
						}
						else if ( !onExecutionGenerator.allowMutation() ) {
							details.excludeColumns( columnIndex, span );
						}
					}
				}
			}
			columnIndex += span;
		}

		for ( var details : columnValueDetailsByEvent.values() ) {
			details.finalizeDetails();
		}

		return new CompositeOnExecutionGenerator( eventTypes, columnValueDetailsByEvent, mutable );
	}

	private CompositeBeforeExecutionGenerator createCompositeBeforeExecutionGenerator() {
		final var composite = (Component) mappingProperty.getValue();
		final var eventTypes = EnumSet.noneOf(EventType.class);
		final var properties = composite.getProperties();
		for ( int i = 0; i < properties.size(); i++ ) {
			final var generator = generators.get(i);
			if ( generator instanceof BeforeExecutionGenerator ) {
				eventTypes.addAll( generator.getEventTypes() );
			}
		}
		return new CompositeBeforeExecutionGenerator( entityName, generators, mappingProperty, properties, eventTypes );
	}

	private record CompositeOnExecutionGenerator(
			EnumSet<EventType> eventTypes,
			EnumMap<EventType, ColumnValueDetails> columnValueDetailsByEvent,
			boolean allowMutation)
				implements OnExecutionGenerator {

		@Override
		public EnumSet<EventType> getEventTypes() {
			return eventTypes;
		}

		@Override
		public boolean referenceColumnsInSql(Dialect dialect) {
			for ( var details : columnValueDetailsByEvent.values() ) {
				if ( details.hasIncludedColumns() ) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean referenceColumnsInSql(Dialect dialect, EventType eventType) {
			final var details = columnValueDetailsByEvent.get( eventType );
			return details != null && details.hasIncludedColumns();
		}

		@Override
		public boolean writePropertyValue() {
			for ( var details : columnValueDetailsByEvent.values() ) {
				if ( details.needsValueBinding() ) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean writePropertyValue(EventType eventType) {
			final var details = columnValueDetailsByEvent.get( eventType );
			return details != null && details.needsValueBinding();
		}

		@Override
		public String[] getReferencedColumnValues(Dialect dialect) {
			final var details = columnValueDetailsByEvent.get( EventType.INSERT );
			return details != null
					? details.columnValues()
					: columnValueDetailsByEvent.values().iterator().next().columnValues();
		}

		@Override
		public String[] getReferencedColumnValues(Dialect dialect, EventType eventType) {
			final var details = columnValueDetailsByEvent.get( eventType );
			return details == null ? null : details.columnValues();
		}

		@Override
		public boolean[] getColumnInclusions(Dialect dialect, EventType eventType) {
			final var details = columnValueDetailsByEvent.get( eventType );
			return details == null ? null : details.columnInclusions();
		}

	}

	private static final class ColumnValueDetails {
		private final String[] columnValues;
		private final boolean[] columnInclusions;
		private boolean hasIncludedColumns;
		private boolean needsValueBinding;

		private ColumnValueDetails(int columnSpan) {
			columnValues = new String[columnSpan];
			columnInclusions = new boolean[columnSpan];
			for ( int i = 0; i < columnSpan; i++ ) {
				columnValues[i] = "?";
				columnInclusions[i] = true;
			}
		}

		private void excludeColumns(int start, int span) {
			for ( int i = 0; i < span; i++ ) {
				columnInclusions[start + i] = false;
			}
		}

		private void setColumnValues(int start, String[] values) {
			arraycopy( values, 0, columnValues, start, values.length );
		}

		private void finalizeDetails() {
			boolean included = false;
			boolean needsBinding = false;
			for ( int i = 0; i < columnValues.length; i++ ) {
				if ( columnInclusions[i] ) {
					included = true;
					if ( "?".equals( columnValues[i] ) ) {
						needsBinding = true;
					}
				}
			}
			hasIncludedColumns = included;
			needsValueBinding = needsBinding;
		}

		private boolean hasIncludedColumns() {
			return hasIncludedColumns;
		}

		private boolean needsValueBinding() {
			return needsValueBinding;
		}

		private String[] columnValues() {
			return columnValues;
		}

		private boolean[] columnInclusions() {
			return columnInclusions;
		}
	}

	private record BeforeExecutionGenerationPlan(
			BeforeExecutionGenerator generator,
			int propertyIndex
	) {
		public static List<BeforeExecutionGenerationPlan> forGenerators(List<Generator> generators) {
			final var result = new ArrayList<BeforeExecutionGenerationPlan>( generators.size() );
			for ( int i = 0; i < generators.size(); i++ ) {
				final var generator = generators.get( i );
				if ( generator instanceof BeforeExecutionGenerator beforeExecutionGenerator ) {
					result.add( new BeforeExecutionGenerationPlan( beforeExecutionGenerator, i ) );
				}
			}
			return result;
		}
	}

	private record CompositeBeforeExecutionGenerator(
			String entityName,
			List<BeforeExecutionGenerationPlan> generationPlans,
			List<Generator> generators,
			ComponentType componentType,
			int propertiesCount,
			EnumSet<EventType> eventTypes)
				implements BeforeExecutionGenerator, CompositeGenerator {

		public CompositeBeforeExecutionGenerator(
				String entityName,
				List<Generator> generators,
				Property mappingProperty,
				List<Property> properties,
				EnumSet<EventType> eventTypes) {
			this(
					entityName,
					BeforeExecutionGenerationPlan.forGenerators( generators ),
					generators,
					(ComponentType) mappingProperty.getType(),
					properties.size(),
					eventTypes
			);
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return eventTypes;
		}

		@Override
		public Class<?> getGeneratedType() {
			return componentType.getJavaType();
		}

		@Override
		public @Nonnull Object generate(
				@Nonnull SharedSessionContractImplementor session,
				@Nullable Object owner,
				@Nullable Object currentValue,
				@Nonnull EventType eventType) {
			final var descriptor = componentType.getMappingModelPart().getEmbeddableTypeDescriptor();
			if ( currentValue == null || !componentType.isMutable() ) {
				final var generatedValues = descriptor.getValues(
						currentValue == null ? instantiateEmptyComposite( descriptor ) : currentValue
				);
				for ( final var generationPlan : generationPlans ) {
					final var beforeExecutionGenerator = generationPlan.generator;
					if ( beforeExecutionGenerator.getEventTypes().contains( eventType )
							&& beforeExecutionGenerator.generatedBeforeExecution( owner, session ) ) {
						generatedValues[generationPlan.propertyIndex] = beforeExecutionGenerator.generate( session, owner, generatedValues[generationPlan.propertyIndex], eventType );
					}
				}
				return descriptor.getRepresentationStrategy().getInstantiator().instantiate( () -> generatedValues );
			}
			else {
				for ( final var generationPlan : generationPlans ) {
					final var beforeExecutionGenerator = generationPlan.generator;
					if ( beforeExecutionGenerator.getEventTypes().contains( eventType )
						&& beforeExecutionGenerator.generatedBeforeExecution( owner, session ) ) {
						final Object value = descriptor.getValue( currentValue, generationPlan.propertyIndex );
						final Object generatedValue = beforeExecutionGenerator.generate( session, owner, value, eventType );
						descriptor.setValue( currentValue, generationPlan.propertyIndex, generatedValue );
					}
				}
				return currentValue;
			}
		}

		@Override
		public @Nonnull Object[] generateBatch(
				@Nonnull SharedSessionContractImplementor session,
				@Nonnull GenerationRequests requests,
				@Nonnull EventType eventType) {
			final Object[] results = new Object[requests.size()];
			final Object[][] propertyValues = new Object[requests.size()][];
			final ArrayList<Integer>[] generationRequestIndexMapping = new ArrayList[generationPlans.size()];
			EmbeddableMappingType[] embeddableMappingTypes = null;
			EmbeddableMappingType singleEmbeddableMappingType = null;
			// Build result objects, extract property values and build an index mapping for generatedValues <=> results
			for ( int i = 0; i < results.length; i++ ) {
				final Object object = requests.get( i ).currentValue();
				final var descriptor = componentType.getMappingModelPart().getEmbeddableTypeDescriptor();
				if ( singleEmbeddableMappingType == null ) {
					singleEmbeddableMappingType = descriptor;
				}
				else if ( singleEmbeddableMappingType != descriptor ) {
					if ( embeddableMappingTypes == null ) {
						embeddableMappingTypes = new EmbeddableMappingType[requests.size()];
					}
					Arrays.fill( embeddableMappingTypes, descriptor );
				}
				if ( embeddableMappingTypes != null ) {
					embeddableMappingTypes[i] = descriptor;
				}
				results[i] = object != null ? object : instantiateEmptyComposite( descriptor );
				propertyValues[i] = descriptor.getValues( results[i] );

				for ( int j = 0; j < generationPlans.size(); j++ ) {
					final var generationPlan = generationPlans.get( j );
					final var generator = generationPlan.generator;
					final var currentValue = propertyValues[i][generationPlan.propertyIndex];
					// Only add index mappings if the generator shall run for this object
					if ( generator.getEventTypes().contains( eventType )
						&& generator.generatedBeforeExecution( currentValue, session ) ) {
						final ArrayList<Integer> generationRequestMapping;
						if ( generationRequestIndexMapping[j] == null ) {
							generationRequestMapping = generationRequestIndexMapping[j] = new ArrayList<>( requests.size() );
						}
						else {
							generationRequestMapping = generationRequestIndexMapping[j];
						}
						generationRequestMapping.add( i );
					}
				}
			}
			for ( int i = 0; i < generationPlans.size(); i++ ) {
				if ( generationRequestIndexMapping[i] != null ) {
					final var generationPlan = generationPlans.get( i );
					// If the generator should be applied to at least one value, will be non-null
					// If we encounter size equals results size, we run the generator for every result,
					// so use null as sentinel to indicate this optimized case to avoid index lookups
					final int[] generatedValueMapping = generationRequestIndexMapping[i].size() == results.length
							? null
							: ArrayHelper.toIntArray( generationRequestIndexMapping[i] );
					// Run the batch generation for once
					final Object[] generatedValues = generationPlan.generator.generateBatch(
							session,
							new CompositeGenerationRequests( results, propertyValues, generatedValueMapping, generationPlan ),
							eventType
					);
					// Apply generated values into the property values arrays
					final int propertyIndex = generationPlan.propertyIndex();
					if ( generatedValueMapping == null ) {
						for ( int j = 0; j < propertyValues.length; j++ ) {
							propertyValues[j][propertyIndex] = generatedValues[j];
						}
					}
					else {
						for ( int j = 0; j < generatedValueMapping.length; j++ ) {
							propertyValues[generatedValueMapping[j]][propertyIndex] = generatedValues[j];
						}
					}
				}
			}
			if ( componentType.isMutable() ) {
				if ( embeddableMappingTypes == null ) {
					for ( int i = 0; i < results.length; i++ ) {
						singleEmbeddableMappingType.setValues( results[i], propertyValues[i] );
					}
				}
				else {
					for ( int i = 0; i < results.length; i++ ) {
						embeddableMappingTypes[i].setValues( results[i], propertyValues[i] );
					}
				}
			}
			else {
				// Build new components with the new property values as result
				final var valueAccess = new ValueAccess() {
					int index;
					@Override
					public Object[] getValues() {
						return propertyValues[index];
					}
				};
				if ( embeddableMappingTypes == null ) {
					final var instantiator = singleEmbeddableMappingType.getRepresentationStrategy().getInstantiator();
					for ( ; valueAccess.index < results.length; valueAccess.index++ ) {
						results[valueAccess.index] = instantiator.instantiate( valueAccess );
					}
				}
				else {
					for ( ; valueAccess.index < results.length; valueAccess.index++ ) {
						results[valueAccess.index] = embeddableMappingTypes[valueAccess.index].getRepresentationStrategy()
								.getInstantiator()
								.instantiate( valueAccess );
					}
				}
			}
			return results;
		}

		private Object instantiateEmptyComposite(EmbeddableMappingType embeddable) {
			return embeddable.getRepresentationStrategy().getInstantiator()
					.instantiate( () -> defaultedPrimitiveIds( embeddable, embeddable ) );
		}

		private record CompositeGenerationRequests(
				Object[] results,
				Object[][] propertyValues,
				int[] generationRequestIndexMapping,
				BeforeExecutionGenerationPlan generationPlan
		) implements GenerationRequests {
			@Override
			public int size() {
				return generationRequestIndexMapping == null ? results.length : generationRequestIndexMapping.length;
			}

			@Override
			public GenerationRequest get(int index) {
				final int idx = generationRequestIndexMapping == null ? index : generationRequestIndexMapping[index];
				return GenerationRequest.of(
						results[idx],
						propertyValues[idx][generationPlan.propertyIndex]
				);
			}
		}

		@Override
		public List<BeforeExecutionGenerator> resolveGenerationDependencies(RuntimeModelCreationContext context) {
			final List<BeforeExecutionGenerator> deps = new ArrayList<>();
			for ( BeforeExecutionGenerationPlan generationPlan : generationPlans ) {
				deps.addAll( generationPlan.generator().resolveGenerationDependencies( context ) );
			}
			return deps;
		}

		@Override
		public boolean supportsBatchGeneration() {
			for ( BeforeExecutionGenerationPlan generationPlan : generationPlans ) {
				if ( generationPlan.generator().supportsBatchGeneration() ) {
					return true;
				}
			}
			return false;
		}
	}

	private record CompositeOnAndBeforeExecutionGenerator(
			EnumSet<EventType> eventTypes,
			List<Generator> generators,
			CompositeOnExecutionGenerator onExecutionGenerator,
			CompositeBeforeExecutionGenerator beforeExecutionGenerator)
				implements OnExecutionGenerator, BeforeExecutionGenerator, CompositeGenerator {

		@Override
		public EnumSet<EventType> getEventTypes() {
			return eventTypes;
		}

		@Override
		public boolean generatedOnExecution() {
			return true;
		}

		@Override
		public @Nonnull Object generate(
				@Nonnull SharedSessionContractImplementor session,
				@Nullable Object owner,
				@Nullable Object currentValue,
				@Nonnull EventType eventType) {
			return beforeExecutionGenerator.generate( session, owner, currentValue, eventType );
		}

		@Override
		public Class<?> getGeneratedType() {
			return beforeExecutionGenerator.getGeneratedType();
		}

		@Override
		public boolean generatedOnExecution(Object entity, SharedSessionContractImplementor session) {
			for ( var generator : generators ) {
				if ( generator instanceof OnExecutionGenerator
						&& generator.generatesSometimes()
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
						&& generator.generatesSometimes()
						&& generator.generatedBeforeExecution( entity, session ) ) {
					return true;
				}
			}
			return false;
		}
		@Override
		public boolean requiresGeneration(Object entity, Object currentValue, SharedSessionContractImplementor session) {
			return onExecutionGenerator.requiresGeneration( entity, currentValue, session )
					|| beforeExecutionGenerator.requiresGeneration( entity, currentValue, session );
		}

		@Override
		public boolean referenceColumnsInSql(Dialect dialect) {
			return onExecutionGenerator.referenceColumnsInSql( dialect );
		}

		@Override
		public boolean referenceColumnsInSql(Dialect dialect, EventType eventType) {
			return onExecutionGenerator.referenceColumnsInSql( dialect, eventType );
		}

		@Override
		public boolean writePropertyValue() {
			return onExecutionGenerator.writePropertyValue();
		}

		@Override
		public boolean writePropertyValue(EventType eventType) {
			return onExecutionGenerator.writePropertyValue( eventType );
		}

		@Override
		public String[] getReferencedColumnValues(Dialect dialect) {
			return onExecutionGenerator.getReferencedColumnValues( dialect );
		}

		@Override
		public String[] getReferencedColumnValues(Dialect dialect, EventType eventType) {
			return onExecutionGenerator.getReferencedColumnValues( dialect, eventType );
		}

		@Override
		public boolean[] getColumnInclusions(Dialect dialect, EventType eventType) {
			return onExecutionGenerator.getColumnInclusions( dialect, eventType );
		}

		@Override
		public boolean allowMutation() {
			return onExecutionGenerator.allowMutation();
		}

		@Override
		public List<BeforeExecutionGenerator> resolveGenerationDependencies(RuntimeModelCreationContext context) {
			return beforeExecutionGenerator.resolveGenerationDependencies( context );
		}
	}

	private record DummyGenerator() implements Generator {
		private static final Generator INSTANCE = new DummyGenerator();

		@Override
		public EnumSet<EventType> getEventTypes() {
			return NONE;
		}

		@Override
		public boolean generatedOnExecution() {
			return false;
		}

		@Override
		public boolean generatedBeforeExecution(Object entity, SharedSessionContractImplementor session) {
			return false;
		}

		@Override
		public boolean allowMutation() {
			return true;
		}

		@Override
		public boolean allowAssignedIdentifiers() {
			return true;
		}
	}
}
