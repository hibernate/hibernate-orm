/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import java.lang.reflect.Constructor;

import org.hibernate.HibernateException;
import org.hibernate.annotations.GeneratorType;
import org.hibernate.internal.util.ReflectHelper;

/**
 * An {@link AnnotationValueGeneration} which delegates to a {@link ValueGenerator}.
 *
 * @author Gunnar Morling
 */
public class VmValueGeneration
		implements AnnotationValueGenerationStrategy<GeneratorType>, InMemoryValueGenerationStrategy {

	private GenerationTiming generationTiming;
	private Constructor<? extends ValueGenerator<?>> constructor;

	@Override
	public void initialize(GeneratorType annotation, Class<?> propertyType, String entityName, String propertyName) {
		constructor = ReflectHelper.getDefaultConstructor( annotation.type() );
		generationTiming = annotation.when().getEquivalent();
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
}
