/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.annotations.Generated;

/**
 * A {@link AnnotationValueGeneration} which marks a property as generated in the database.
 *
 * @author Steve Ebersole
 * @author Gunnar Morling
 */
public class GeneratedValueGeneration implements AnnotationValueGeneration<Generated> {

	private GenerationTiming timing;
	private boolean writable;

	public GeneratedValueGeneration() {
	}

	public GeneratedValueGeneration(GenerationTiming timing) {
		this.timing = timing;
	}

	@Override
	public void initialize(Generated annotation, Class<?> propertyType) {
		timing = annotation.value().getEquivalent();
		writable = annotation.writable();
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return timing;
	}

	@Override
	public ValueGenerator<?> getValueGenerator() {
		// database generated values do not have a value generator
		return null;
	}

	@Override
	public boolean referenceColumnInSql() {
		return writable;
	}

	@Override
	public String getDatabaseGeneratedReferencedColumnValue() {
		return null;
	}
}

