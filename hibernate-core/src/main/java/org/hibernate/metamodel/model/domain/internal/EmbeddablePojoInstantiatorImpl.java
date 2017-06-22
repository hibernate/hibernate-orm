/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.bytecode.spi.ReflectionOptimizer.InstantiationOptimizer;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;

/**
 * EmbeddableInstantiator for POJO representation
 *
 * @author Steve Ebersole
 */
public class EmbeddablePojoInstantiatorImpl extends AbstractPojoInstantiator {
	public EmbeddablePojoInstantiatorImpl(
			Class mappedPojoClass,
			InstantiationOptimizer optimizer) {
		super( mappedPojoClass, optimizer );
	}

	public EmbeddablePojoInstantiatorImpl(
			EmbeddedTypeDescriptor runtimeDescriptor,
			InstantiationOptimizer optimizer) {
		super( runtimeDescriptor.getJavaType(), optimizer );
	}

	@Override
	public Object instantiate(SharedSessionContractImplementor session) {
		return instantiatePojo( session );
	}
}
