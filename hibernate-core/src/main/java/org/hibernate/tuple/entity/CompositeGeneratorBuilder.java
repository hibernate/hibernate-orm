/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.CompositeBeforeExecutionGenerator;
import org.hibernate.generator.CompositeGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.generator.internal.OnExecutionCompositeGenerator;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.AttributeMapping;
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
class CompositeGeneratorBuilder {
	private final String entityName;
	private final Property mappingProperty;
	private final Dialect dialect;

	private boolean hadBeforeExecutionGeneration;
	private boolean hadOnExecutionGeneration;

	private final Map<String,Generator> generators = new HashMap<>();
	private final List<Generator> generatorsByStateArrayPosition = new ArrayList<>();

	public CompositeGeneratorBuilder(String entityName, Property mappingProperty, Dialect dialect) {
		this.entityName = entityName;
		this.mappingProperty = mappingProperty;
		this.dialect = dialect;
	}

	public void add(String attributeName, Generator generator) {
		generators.put( attributeName, generator );
		generatorsByStateArrayPosition.add( generator );

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
			return new CompositeGenerator() {
				@Override
				public boolean generatedOnExecution() {
					return false;
				}

				@Override
				public EnumSet<EventType> getEventTypes() {
					return NONE;
				}

				@Override
				public Generator getGenerator(int attributeStateArrayPosition) {
					return new Generator() {
						@Override
						public EnumSet<EventType> getEventTypes() {
							return NONE;
						}

						@Override
						public boolean generatedOnExecution() {
							return false;
						}
					};
				}
			};
		}
	}

	private Generator createCompositeOnExecutionGenerator() {
		final Component composite = (Component) mappingProperty.getValue();
		// the base-line values for the aggregated OnExecutionGenerator we will build here.
		final EnumSet<EventType> eventTypes = EnumSet.noneOf(EventType.class);
		final List<Property> properties = composite.getProperties();
		boolean writePropertyValue = false;
		boolean referenceColumnsInSql = false;
		final String[] columnValues = new String[composite.getColumnSpan()];
		int columnIndex = 0;
		for ( int i = 0; i < properties.size(); i++ ) {
			final Property property = properties.get( i );
//			final OnExecutionGenerator generator = (OnExecutionGenerator) generators.get( property.getName() );
			final OnExecutionGenerator generator = (OnExecutionGenerator) generatorsByStateArrayPosition.get( i );
			if ( generator != null ) {
				if ( generator.generatesSometimes() ) {
					eventTypes.addAll( generator.getEventTypes() );
					if ( generator.referenceColumnsInSql( dialect ) ) {
						referenceColumnsInSql = true;
						final String[] referencedColumnValues = generator.getReferencedColumnValues( dialect );
						if ( referencedColumnValues != null ) {
							final int span = property.getColumnSpan();
							if ( referencedColumnValues.length != span ) {
								throw new CompositeValueGenerationException(
										"Mismatch between number of collected generated column values and number of columns for composite attribute: "
												+ mappingProperty.getName() + '.' + property.getName()
								);
							}
							arraycopy( referencedColumnValues, 0, columnValues, columnIndex, span );
							columnIndex += span;
						}
					}
					if ( generator.writePropertyValue() ) {
						writePropertyValue = true;
					}
				}
				else {
					generators.put( property.getName(), null );
					generatorsByStateArrayPosition.set( i, null );
				}
			}
		}
		return new OnExecutionCompositeGenerator( generatorsByStateArrayPosition, eventTypes, writePropertyValue, referenceColumnsInSql, columnValues );
	}

	private CompositeBeforeExecutionGenerator createCompositeBeforeExecutionGenerator() {
		final Component composite = (Component) mappingProperty.getValue();
		final EnumSet<EventType> eventTypes = EnumSet.noneOf(EventType.class);
		final List<Property> properties = composite.getProperties();
		for ( int i = 0; i < properties.size(); i++ ) {
			final Generator generator = generators.get(properties.get( i ).getName());
			if ( generator != null ) {
				eventTypes.addAll( generator.getEventTypes() );
			}
		}
		return new CompositeBeforeExecutionGenerator() {
			@Override
			public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
				final EntityPersister persister = session.getEntityPersister( entityName, owner );
				final int index = persister.getPropertyIndex( mappingProperty.getName() );
				final EmbeddableMappingType descriptor =
						persister.getAttributeMapping(index).asEmbeddedAttributeMapping()
								.getEmbeddableTypeDescriptor();
				final int size = properties.size();
				if ( currentValue == null ) {
					final Object[] generatedValues = new Object[size];
					for ( int i = 0; i < size; i++ ) {
						final Generator generator = generators.get( properties.get( i ).getName() );
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
						final Generator generator = generators.get( properties.get( i ).getName() );
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

			@Override
			public EnumSet<EventType> getEventTypes() {
				return eventTypes;
			}

			@Override
			public Generator getGenerator(int attributeStateArrayPosition) {
				return generatorsByStateArrayPosition.get( attributeStateArrayPosition );
			}
		};
	}
}
