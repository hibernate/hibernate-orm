/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import java.lang.reflect.Constructor;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.Session;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.GeneratorType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.InMemoryGenerator;

import static org.hibernate.internal.util.ReflectHelper.getDefaultConstructor;

/**
 * An {@link InMemoryGenerator} which delegates to a {@link ValueGenerator}.
 * Underlies the {@link GeneratorType} annotation.
 *
 * @author Gunnar Morling
 *
 * @deprecated since {@link GeneratorType} is deprecated
 */
@Internal
@Deprecated(since = "6.2")
public class VmValueGeneration implements InMemoryGenerator {

	private final GenerationTime timing;
	private final ValueGenerator<?> generator;

	public VmValueGeneration(GeneratorType annotation) {
		Constructor<? extends ValueGenerator<?>> constructor = getDefaultConstructor( annotation.type() );
		try {
			generator = constructor.newInstance();
		}
		catch (Exception e) {
			throw new HibernateException( "Couldn't instantiate value generator", e );
		}
		timing = annotation.when();
	}

	/**
	 * @return {@code true}
	 */
	@Override
	public boolean generatedOnInsert() {
		return timing.includesInsert();
	}

	/**
	 * @return {@code false}
	 */
	@Override
	public boolean generatedOnUpdate() {
		return timing.includesUpdate();
	}


	@Override
	public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue) {
		return generator.generateValue( (Session) session, owner, currentValue );
	}
}
