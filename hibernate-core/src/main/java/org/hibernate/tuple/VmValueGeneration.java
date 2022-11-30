/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import java.lang.reflect.Constructor;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.annotations.GeneratorType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ReflectHelper;

/**
 * An {@link AnnotationValueGeneration} which delegates to a {@link ValueGenerator}.
 *
 * @author Gunnar Morling
 */
public class VmValueGeneration
		implements AnnotationGenerator<GeneratorType>, InMemoryGenerator {

	private GenerationTiming generationTiming;
	ValueGenerator<?> generator;

	@Override
	public void initialize(GeneratorType annotation, Class<?> propertyType, String entityName, String propertyName) {
		Constructor<? extends ValueGenerator<?>> constructor = ReflectHelper.getDefaultConstructor(annotation.type());
		try {
			generator = constructor.newInstance();
		}
		catch (Exception e) {
			throw new HibernateException( "Couldn't instantiate value generator", e );
		}
		generationTiming = annotation.when().getEquivalent();
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return generationTiming;
	}

	@Override
	public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue) {
		return generator.generateValue( (Session) session, owner, currentValue );
	}
}
