/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.tuple.AnnotationValueGeneration;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.TimestampGenerators;
import org.hibernate.tuple.ValueGenerator;

/**
 * Value generation strategy using the database {@code current_timestamp}
 * function or the JVM current instant.
 *
 * @see CurrentTimestamp
 *
 * @author Steve Ebersole
 */
@Internal
public class CurrentTimestampGeneration implements AnnotationValueGeneration<CurrentTimestamp> {
	private GenerationTiming timing;
	private ValueGenerator<?> generator;

	@Override
	public void initialize(CurrentTimestamp annotation, Class<?> propertyType) {
		if ( annotation.source() == SourceType.VM ) {
			// ValueGenerator is only used for in-VM generation
			generator = TimestampGenerators.get( propertyType );
		}
		timing = annotation.timing();
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return timing;
	}

	@Override
	public ValueGenerator<?> getValueGenerator() {
		return generator;
	}

	@Override
	public boolean referenceColumnInSql() {
		return true;
	}

	@Override
	public String getDatabaseGeneratedReferencedColumnValue() {
		return "current_timestamp";
	}

	@Override
	public String getDatabaseGeneratedReferencedColumnValue(Dialect dialect) {
		return dialect.currentTimestamp();
	}
}
