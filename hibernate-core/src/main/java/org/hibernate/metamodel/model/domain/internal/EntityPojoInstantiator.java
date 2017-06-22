/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;

/**
 * @author Steve Ebersole
 */
public class EntityPojoInstantiator extends AbstractPojoInstantiator {
	private final EntityDescriptor entityDescriptor;
	private final boolean applyBytecodeInterception;
	private final Class proxyJavaType;

	public EntityPojoInstantiator(
			EntityMapping entityMapping,
			EntityDescriptor entityDescriptor,
			ReflectionOptimizer.InstantiationOptimizer optimizer) {
		super( entityDescriptor.getMappedClass(), optimizer );
		this.entityDescriptor = entityDescriptor;

		this.applyBytecodeInterception = PersistentAttributeInterceptable.class.isAssignableFrom( entityDescriptor.getMappedClass() );
		this.proxyJavaType = entityDescriptor.getConcreteProxyClass();
	}

	@Override
	public boolean isInstance(Object object, SharedSessionContractImplementor session) {
		return super.isInstance( object, session )
				|| ( proxyJavaType != null && proxyJavaType.isInstance( object ) );

	}

	@Override
	public Object instantiate(SharedSessionContractImplementor session) {
		final Object entityInstance = instantiatePojo( session );

		if ( !applyBytecodeInterception ) {
			return entityInstance;
		}

		final PersistentAttributeInterceptor interceptor = new LazyAttributeLoadingInterceptor(
				entityDescriptor.getName(),
				entityDescriptor.getBytecodeEnhancementMetadata()
						.getLazyAttributesMetadata()
						.getLazyAttributeNames(),
				session
		);
		( (PersistentAttributeInterceptable) entityInstance ).$$_hibernate_setInterceptor( interceptor );
		return entityInstance;
	}
}
