/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import org.hibernate.dialect.Dialect;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static java.lang.System.arraycopy;
import static org.hibernate.generator.EventTypeSets.NONE;

/**
 * Handles value generation for composite properties.
 */
class CompositeGeneratorBuilder {
	private final Property mappingProperty;
	private final Dialect dialect;

	private boolean hadBeforeExecutionGeneration;
	private boolean hadOnExecutionGeneration;

	private List<OnExecutionGenerator> onExecutionGenerators;

	public CompositeGeneratorBuilder(Property mappingProperty, Dialect dialect) {
		this.mappingProperty = mappingProperty;
		this.dialect = dialect;
	}

	public void add(Generator generator) {
		if ( generator != null ) {
			if ( generator.generatedOnExecution() ) {
				if ( generator instanceof OnExecutionGenerator ) {
					add( (OnExecutionGenerator) generator );
				}
			}
			else {
				if ( generator instanceof BeforeExecutionGenerator ) {
					add( (BeforeExecutionGenerator) generator );
				}
			}
		}
	}

	private void add(BeforeExecutionGenerator beforeExecutionGenerator) {
		if ( beforeExecutionGenerator.generatesSometimes() ) {
			hadBeforeExecutionGeneration = true;
		}
	}

	private void add(OnExecutionGenerator onExecutionGenerator) {
		if ( onExecutionGenerators == null ) {
			onExecutionGenerators = new ArrayList<>();
		}
		onExecutionGenerators.add( onExecutionGenerator );

		if ( onExecutionGenerator.generatesSometimes() ) {
			hadOnExecutionGeneration = true;
		}
	}

	public Generator build() {
		if ( hadBeforeExecutionGeneration && hadOnExecutionGeneration) {
			throw new CompositeValueGenerationException(
					"Composite attribute [" + mappingProperty.getName() + "] contained both in-memory"
							+ " and in-database value generation"
			);
		}
		else if ( hadBeforeExecutionGeneration ) {
			throw new UnsupportedOperationException("Composite in-memory value generation not supported");

		}
		else if ( hadOnExecutionGeneration ) {
			final Component composite = (Component) mappingProperty.getValue();

			// we need the numbers to match up so that we can properly handle 'referenced sql column values'
			if ( onExecutionGenerators.size() != composite.getPropertySpan() ) {
				throw new CompositeValueGenerationException(
						"Internal error : mismatch between number of collected in-db generation strategies" +
								" and number of attributes for composite attribute : " + mappingProperty.getName()
				);
			}

			// the base-line values for the aggregated OnExecutionGenerator we will build here.
			final EnumSet<EventType> eventTypes = EnumSet.noneOf(EventType.class);
			boolean referenceColumns = false;
			final String[] columnValues = new String[composite.getColumnSpan()];

			// start building the aggregate values
			int propertyIndex = -1;
			int columnIndex = 0;
			for ( Property property : composite.getProperties() ) {
				propertyIndex++;
				final OnExecutionGenerator generator = onExecutionGenerators.get( propertyIndex );
				eventTypes.addAll( generator.getEventTypes() );
				if ( generator.referenceColumnsInSql( dialect ) ) {
					// override base-line value
					referenceColumns = true;
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
					}
				}
			}
			final boolean referenceColumnsInSql = referenceColumns;

			// then use the aggregated values to build an OnExecutionGenerator
			return new OnExecutionGenerator() {
				@Override
				public EnumSet<EventType> getEventTypes() {
					return eventTypes;
				}

				@Override
				public boolean referenceColumnsInSql(Dialect dialect) {
					return referenceColumnsInSql;
				}

				@Override
				public String[] getReferencedColumnValues(Dialect dialect) {
					return columnValues;
				}

				@Override
				public boolean writePropertyValue() {
					return false;
				}
			};
		}
		else {
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
	}
}
