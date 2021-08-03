/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import org.hibernate.tuple.AnnotationValueGeneration;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.ValueGenerator;

/**
 * Value generation strategy for using the database `current_timestamp` function to generate
 * the values
 *
 * @see CurrentTimestamp
 *
 * @author Steve Ebersole
 */
public class CurrentTimestampGeneration implements AnnotationValueGeneration<CurrentTimestamp> {
	private GenerationTiming timing;

	@Override
	public void initialize(CurrentTimestamp annotation, Class<?> propertyType) {
		this.timing = annotation.timing();
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return timing;
	}

	@Override
	public ValueGenerator<?> getValueGenerator() {
		// ValueGenerator is only used for in-VM generations
		return null;
	}

	@Override
	public boolean referenceColumnInSql() {
		return true;
	}

	@Override
	public String getDatabaseGeneratedReferencedColumnValue() {
		return "current_timestamp";
	}
}
