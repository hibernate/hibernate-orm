/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.annotations.ColumnGeneratedAlways;

/**
 * For {@link ColumnGeneratedAlways}
 *
 * @author Gavin King
 */
public class GeneratedAlwaysValueGeneration implements AnnotationValueGeneration<ColumnGeneratedAlways> {

	private boolean select;

	public GeneratedAlwaysValueGeneration() {}

	@Override
	public void initialize(ColumnGeneratedAlways annotation, Class<?> propertyType) {
		select = annotation.fetch();
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return select ? GenerationTiming.ALWAYS : GenerationTiming.NEVER;
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
