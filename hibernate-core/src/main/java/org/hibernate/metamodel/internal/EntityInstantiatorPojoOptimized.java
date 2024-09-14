/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.internal;

import org.hibernate.bytecode.spi.ReflectionOptimizer.InstantiationOptimizer;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Support for instantiating entity values as POJO representation using
 * bytecode optimizer
 *
 * @author Steve Ebersole
 */
public class EntityInstantiatorPojoOptimized extends AbstractEntityInstantiatorPojo {
	private final InstantiationOptimizer instantiationOptimizer;

	public EntityInstantiatorPojoOptimized(
			EntityMetamodel entityMetamodel,
			PersistentClass persistentClass,
			JavaType<?> javaType,
			InstantiationOptimizer instantiationOptimizer) {
		super( entityMetamodel, persistentClass, javaType );
		this.instantiationOptimizer = instantiationOptimizer;
	}

	@Override
	public Object instantiate(SessionFactoryImplementor sessionFactory) {
		return applyInterception( instantiationOptimizer.newInstance() );
	}
}
