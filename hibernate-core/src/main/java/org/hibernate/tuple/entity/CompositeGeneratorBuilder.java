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
			if ( generator.generatedOnExecution() ) {
				hadOnExecutionGeneration = true;
			}
			else {
				hadBeforeExecutionGeneration = true;
			}
		}
	}

	public Generator build() {
		if ( hadBeforeExecutionGeneration && hadOnExecutionGeneration ) {
			throw new CompositeValueGenerationException(
					"Composite attribute contained both on-execution and before-execution generators: "
							+ mappingProperty.getName()
			);
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

	private OnExecutionGenerator createCompositeOnExecutionGenerator() {
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
			if ( generators.get( i ) instanceof OnExecutionGenerator generator ) {
				mutable = mutable || generator.allowMutation();
				final var generatorEventTypes = generator.getEventTypes();
				for ( var eventType : eventTypes ) {
					final var details = columnValueDetailsByEvent.get( eventType );
					if ( details != null ) {
						if ( generatorEventTypes.contains( eventType ) ) {
							if ( !generator.referenceColumnsInSql( dialect, eventType ) ) {
								details.excludeColumns( columnIndex, span );
							}
							else if ( generator.writePropertyValue( eventType ) ) {
								// leave the default parameter marker values in place
							}
							else {
								final String[] referencedColumnValues =
										generator.getReferencedColumnValues( dialect, eventType );
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
						else if ( !generator.allowMutation() ) {
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

	private BeforeExecutionGenerator createCompositeBeforeExecutionGenerator() {
		final var composite = (Component) mappingProperty.getValue();
		final var eventTypes = EnumSet.noneOf(EventType.class);
		final var properties = composite.getProperties();
		for ( int i = 0; i < properties.size(); i++ ) {
			final var generator = generators.get(i);
			if ( generator != null ) {
				eventTypes.addAll( generator.getEventTypes() );
			}
		}
		return new CompositeBeforeExecutionGenerator( entityName, generators, mappingProperty, properties, eventTypes );
	}

	private static final class CompositeOnExecutionGenerator implements OnExecutionGenerator {
		private final EnumSet<EventType> eventTypes;
		private final EnumMap<EventType, ColumnValueDetails> columnValueDetailsByEvent;
		private final boolean allowMutation;

		private CompositeOnExecutionGenerator(
				EnumSet<EventType> eventTypes,
				EnumMap<EventType, ColumnValueDetails> columnValueDetailsByEvent,
				boolean allowMutation) {
			this.eventTypes = eventTypes;
			this.columnValueDetailsByEvent = columnValueDetailsByEvent;
			this.allowMutation = allowMutation;
		}

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

		@Override
		public boolean allowMutation() {
			return allowMutation;
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
					if ( generator != null && generator.getEventTypes().contains( eventType ) ) {
						generatedValues[i] = ((BeforeExecutionGenerator) generator)
								.generate( session, owner, null, eventType );
					}
				}
				return descriptor.getRepresentationStrategy().getInstantiator().instantiate( () -> generatedValues );
			}
			else {
				for ( int i = 0; i < size; i++ ) {
					final var generator = generators.get( i );
					if ( generator != null && generator.getEventTypes().contains( eventType ) ) {
						final Object value = descriptor.getValue( currentValue, i );
						final Object generatedValue = ((BeforeExecutionGenerator) generator)
								.generate( session, owner, value, eventType );
						descriptor.setValue( currentValue, i, generatedValue );
					}
				}
				return currentValue;
			}
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
