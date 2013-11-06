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

import java.lang.reflect.Constructor;

import org.hibernate.HibernateException;
import org.hibernate.annotations.GeneratorType;
import org.hibernate.internal.util.ReflectHelper;

/**
 * A {@link AnnotationValueGeneration} which allows to specify the {@link ValueGenerator} to be used to determine the
 * value of the annotated property.
 *
 * @author Gunnar Morling
 */
public class VmValueGeneration implements AnnotationValueGeneration<GeneratorType> {

	private GenerationTiming generationTiming;
	private Constructor<? extends ValueGenerator<?>> constructor;

	@Override
	public void initialize(GeneratorType annotation, Class<?> propertyType) {
		Class<? extends ValueGenerator<?>> generatorType = annotation.type();
		constructor = ReflectHelper.getDefaultConstructor( generatorType );
		this.generationTiming = annotation.when().getEquivalent();
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return generationTiming;
	}

	@Override
	public ValueGenerator<?> getValueGenerator() {
		try {
			return constructor.newInstance();
		}
		catch (Exception e) {
			throw new HibernateException( "Couldn't instantiate value generator", e );
		}
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
