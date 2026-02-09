/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tuple.entity;

import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

import static java.lang.System.arraycopy;
import static org.hibernate.generator.EventTypeSets.NONE;

/**
 * Handles value generation for composite properties.
 */
@Internal
class CompositeGeneratorBuilder {
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
			if ( generator instanceof OnExecutionGenerator ) {
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
			if ( generator instanceof OnExecutionGenerator onExecutionGenerator ) {
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

	private record CompositeBeforeExecutionGenerator(
			String entityName,
			List<Generator> generators,
			Property mappingProperty,
			List<Property> properties,
			EnumSet<EventType> eventTypes)
				implements BeforeExecutionGenerator {
		@Override
		public EnumSet<EventType> getEventTypes() {
			return eventTypes;
		}
		@Override
		public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
			final var persister = session.getEntityPersister( entityName, owner );
			final int index = persister.getPropertyIndex( mappingProperty.getName() );
			final var descriptor =
					persister.getAttributeMapping( index ).asEmbeddedAttributeMapping()
							.getEmbeddableTypeDescriptor();
			final int size = properties.size();
			if ( currentValue == null ) {
				final var generatedValues = new Object[size];
				for ( int i = 0; i < size; i++ ) {
					final var generator = generators.get( i );
					if ( generator instanceof BeforeExecutionGenerator beforeExecutionGenerator
							&& generator.getEventTypes().contains( eventType )
							&& generator.generatedBeforeExecution( owner, session ) ) {
						generatedValues[i] = beforeExecutionGenerator.generate( session, owner, null, eventType );
					}
				}
				return descriptor.getRepresentationStrategy().getInstantiator().instantiate( () -> generatedValues );
			}
			else {
				for ( int i = 0; i < size; i++ ) {
					final var generator = generators.get( i );
					if ( generator instanceof BeforeExecutionGenerator beforeExecutionGenerator
							&& generator.getEventTypes().contains( eventType )
							&& generator.generatedBeforeExecution( owner, session ) ) {
						final Object value = descriptor.getValue( currentValue, i );
						final Object generatedValue = beforeExecutionGenerator.generate( session, owner, value, eventType );
						descriptor.setValue( currentValue, i, generatedValue );
					}
				}
				return currentValue;
			}
		}
	}

	private record CompositeOnAndBeforeExecutionGenerator(
			EnumSet<EventType> eventTypes,
			List<Generator> generators,
			CompositeOnExecutionGenerator onExecutionGenerator,
			CompositeBeforeExecutionGenerator beforeExecutionGenerator)
				implements OnExecutionGenerator, BeforeExecutionGenerator {

		@Override
		public EnumSet<EventType> getEventTypes() {
			return eventTypes;
		}

		@Override
		public boolean generatedOnExecution() {
			return true;
		}

		@Override
		public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
			return beforeExecutionGenerator.generate( session, owner, currentValue, eventType );
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
		public boolean allowMutation() {
			return true;
		}

		@Override
		public boolean allowAssignedIdentifiers() {
			return true;
		}
	}
}
