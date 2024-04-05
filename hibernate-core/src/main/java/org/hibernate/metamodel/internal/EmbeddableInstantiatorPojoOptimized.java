/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.util.function.Supplier;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.ValueAccess;

import static org.hibernate.bytecode.spi.ReflectionOptimizer.InstantiationOptimizer;

/**
 * Support for instantiating embeddables as POJO representation
 * using bytecode optimizer
 */
public class EmbeddableInstantiatorPojoOptimized extends AbstractPojoInstantiator implements StandardEmbeddableInstantiator {
	private final Supplier<EmbeddableMappingType> embeddableMappingAccess;
	private final InstantiationOptimizer instantiationOptimizer;

	public EmbeddableInstantiatorPojoOptimized(
			Class<?> embeddableClass,
			Supplier<EmbeddableMappingType> embeddableMappingAccess,
			InstantiationOptimizer instantiationOptimizer) {
		super( embeddableClass );
		this.embeddableMappingAccess = embeddableMappingAccess;
		this.instantiationOptimizer = instantiationOptimizer;
	}

	@Override
	public Object instantiate(ValueAccess valuesAccess, SessionFactoryImplementor sessionFactory) {
		final Object embeddable = instantiationOptimizer.newInstance();
		final EmbeddableMappingType embeddableMapping = embeddableMappingAccess.get();
		final Object[] values = valuesAccess.getValues();
		if ( values != null ) {
			embeddableMapping.setValues( embeddable, values );
		}
		return embeddable;
	}
}
