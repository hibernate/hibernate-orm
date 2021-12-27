/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.annotations.ColumnDefault;

/**
 * For {@link ColumnDefault}
 *
 * @author Gavin King
 */
public class DefaultValueGeneration implements AnnotationValueGeneration<ColumnDefault> {

	public DefaultValueGeneration() {}

	@Override
	public void initialize(ColumnDefault annotation, Class<?> propertyType) {}

	@Override
	public GenerationTiming getGenerationTiming() {
		return GenerationTiming.INSERT;
	}

	@Override
	public ValueGenerator<?> getValueGenerator() {
		// database generated values do not have a value generator
		return null;
	}

	@Override
	public boolean referenceColumnInSql() {
		return false;
	}

	@Override
	public String getDatabaseGeneratedReferencedColumnValue() {
		return null;
	}
}
