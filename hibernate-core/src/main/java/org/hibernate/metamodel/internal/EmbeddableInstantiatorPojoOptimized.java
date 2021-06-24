/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.util.function.Supplier;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

import static org.hibernate.bytecode.spi.ReflectionOptimizer.InstantiationOptimizer;

/**
 * Support for instantiating embeddables as POJO representation
 * using bytecode optimizer
 *
 * @author Steve Ebersole
 */
public class EmbeddableInstantiatorPojoOptimized extends AbstractPojoInstantiator implements EmbeddableInstantiator {
	private final InstantiationOptimizer instantiationOptimizer;

	public EmbeddableInstantiatorPojoOptimized(
			JavaTypeDescriptor<?> javaTypeDescriptor,
			InstantiationOptimizer instantiationOptimizer) {
		super( javaTypeDescriptor.getJavaTypeClass() );
		this.instantiationOptimizer = instantiationOptimizer;
	}

	@Override
	public Object instantiate(Supplier<Object[]> valuesAccess, SessionFactoryImplementor sessionFactory) {
		return instantiationOptimizer.newInstance();
	}
}
