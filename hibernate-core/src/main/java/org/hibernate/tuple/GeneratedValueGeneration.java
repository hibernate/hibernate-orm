/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

	public GeneratedValueGeneration() {
	}

	public GeneratedValueGeneration(GenerationTiming timing) {
		this.timing = timing;
	}

	@Override
	public void initialize(Generated annotation, Class<?> propertyType) {
		this.timing = annotation.value().getEquivalent();
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
		// historically these columns are not referenced in the SQL
		return false;
	}

	@Override
	public String getDatabaseGeneratedReferencedColumnValue() {
		return null;
	}
}
