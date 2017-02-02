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
