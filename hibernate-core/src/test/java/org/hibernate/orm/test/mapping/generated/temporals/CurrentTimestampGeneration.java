/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.generated.temporals;

import org.hibernate.tuple.AnnotationValueGeneration;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.ValueGenerator;

/**
 * @author Steve Ebersole
 */
public class CurrentTimestampGeneration implements AnnotationValueGeneration<CurrentTimestamp> {
	private GenerationTiming timing;
	private Class<?> propertyType;

	@Override
	public void initialize(CurrentTimestamp annotation, Class<?> propertyType) {
		this.timing = annotation.timing();
		this.propertyType = propertyType;
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return timing;
	}

	@Override
	public ValueGenerator<?> getValueGenerator() {
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
