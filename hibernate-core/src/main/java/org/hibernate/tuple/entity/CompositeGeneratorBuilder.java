/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tuple.entity;

import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.CompositeOnExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.persister.entity.EntityPersister;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private final Map<String,Generator> generatorsByPropertyName = new HashMap<>();

	public CompositeGeneratorBuilder(String entityName, Property mappingProperty, Dialect dialect) {
		this.entityName = entityName;
		this.mappingProperty = mappingProperty;
		this.dialect = dialect;
	}

	public void add(String propertyName, Generator generator) {
		generatorsByPropertyName.put( propertyName, generator );

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
		final Component composite = (Component) mappingProperty.getValue();
		// the base-line values for the aggregated OnExecutionGenerator we will build here.
		final EnumSet<EventType> eventTypes = EnumSet.noneOf(EventType.class);
		boolean referenceColumns = false;
		boolean writable = false;
		boolean mutable = false;
		final String[] columnValues = new String[composite.getColumnSpan()];
		final boolean[] writesColumnValues = new boolean[composite.getColumnSpan()];

		// start building the aggregate values
		int columnIndex = 0;
		final List<Property> properties = composite.getProperties();
		final Map<String, OnExecutionGenerator> generatorsByName = new HashMap<>(properties.size());
		for ( int i = 0; i < properties.size(); i++ ) {
			final Property property = properties.get(i);
			final String name = property.getName();
			final OnExecutionGenerator generator = (OnExecutionGenerator) generatorsByPropertyName.get( name );
			generatorsByName.put( name, generator );
			final int span = property.getColumnSpan();
			final String[] referencedColumnValues;
			if ( generator != null ) {
				eventTypes.addAll( generator.getEventTypes() );
				if ( generator.referenceColumnsInSql( dialect ) ) {
					// override base-line value
					referenceColumns = true;
					referencedColumnValues = generator.getReferencedColumnValues( dialect );
					if ( referencedColumnValues != null ) {
						if ( referencedColumnValues.length != span ) {
							throw new CompositeValueGenerationException(
									"Mismatch between number of collected generated column values and number of columns for composite attribute: "
											+ mappingProperty.getName() + '.' + name
							);
						}
						arraycopy( referencedColumnValues, 0, columnValues, columnIndex, span );

					}
				}
				if ( generator.writePropertyValue() ) {
					writable = true;
					for ( int j = columnIndex; j < columnIndex + span; j++ ) {
						writesColumnValues[j] = true;
					}
				}
				if ( generator.allowMutation() ) {
					mutable = true;
				}
			}
			else {
				writable = true;
				for ( int j = columnIndex; j < columnIndex + span; j++ ) {
					writesColumnValues[j] = true;
				}
				mutable = true;
			}
			columnIndex += span;
		}

		// then use the aggregated values to build an OnExecutionGenerator
		return new CompositeOnExecutionGeneratorImpl( eventTypes, generatorsByName, referenceColumns, columnValues, writable, writesColumnValues, mutable );
	}

	private BeforeExecutionGenerator createCompositeBeforeExecutionGenerator() {
		final Component composite = (Component) mappingProperty.getValue();
		final EnumSet<EventType> eventTypes = EnumSet.noneOf(EventType.class);
		final List<Property> properties = composite.getProperties();
		final List<Generator> generators = new ArrayList<>(properties.size());
		for ( int i = 0; i < properties.size(); i++ ) {
			final Generator generator = generatorsByPropertyName.get( properties.get( i ).getName() );
			generators.add( generator );
			if ( generator != null ) {
				eventTypes.addAll( generator.getEventTypes() );
			}
		}
		return new CompositeBeforeExecutionGenerator( entityName, generators, mappingProperty, properties, eventTypes );
	}

	public record CompositeOnExecutionGeneratorImpl(
			EnumSet<EventType> eventTypes,
			Map<String,OnExecutionGenerator> generatedPropertiesByName,
			boolean referenceColumnsInSql,
			String[] columnValues,
			boolean writePropertyValue,
			boolean[] writePropertyValues,
			boolean allowMutation)
				implements CompositeOnExecutionGenerator {
		@Override
		public boolean referenceColumnsInSql(Dialect dialect) {
			return referenceColumnsInSql;
		}
		@Override
		public String[] getReferencedColumnValues(Dialect dialect) {
			return columnValues;
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return eventTypes;
		}

		@Override
		public OnExecutionGenerator getPropertyGenerator(String propertyName) {
			return generatedPropertiesByName.get( propertyName );
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
			final EntityPersister persister = session.getEntityPersister( entityName, owner );
			final int index = persister.getPropertyIndex( mappingProperty.getName() );
			final EmbeddableMappingType descriptor =
					persister.getAttributeMapping( index ).asEmbeddedAttributeMapping()
							.getEmbeddableTypeDescriptor();
			final int size = properties.size();
			if ( currentValue == null ) {
				final Object[] generatedValues = new Object[size];
				for ( int i = 0; i < size; i++ ) {
					final Generator generator = generators.get( i );
					if ( generator != null ) {
						generatedValues[i] = ((BeforeExecutionGenerator) generator)
								.generate( session, owner, null, eventType );
					}
				}
				return descriptor.getRepresentationStrategy().getInstantiator()
						.instantiate( () -> generatedValues, session.getFactory() );
			}
			else {
				for ( int i = 0; i < size; i++ ) {
					final Generator generator = generators.get( i );
					if ( generator != null ) {
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
