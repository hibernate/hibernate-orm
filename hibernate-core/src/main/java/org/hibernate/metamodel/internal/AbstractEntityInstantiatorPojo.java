/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.internal;


import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.EntityInstantiator;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptableType;

/**
 * Base support for instantiating entity values as POJO representation
 *
 * @author Steve Ebersole
 */
public abstract class AbstractEntityInstantiatorPojo extends AbstractPojoInstantiator implements EntityInstantiator {
	private final EntityMetamodel entityMetamodel;
	private final Class<?> proxyInterface;
	private final boolean applyBytecodeInterception;

	public AbstractEntityInstantiatorPojo(
			EntityMetamodel entityMetamodel,
			PersistentClass persistentClass,
			JavaType<?> javaType) {
		super( javaType.getJavaTypeClass() );

		this.entityMetamodel = entityMetamodel;
		this.proxyInterface = persistentClass.getProxyInterface();

		//TODO this PojoEntityInstantiator appears to not be reused ?!
		this.applyBytecodeInterception = isPersistentAttributeInterceptableType( persistentClass.getMappedClass() );
	}

	protected Object applyInterception(Object entity) {
		if ( !applyBytecodeInterception ) {
			return entity;
		}

		PersistentAttributeInterceptor interceptor = new LazyAttributeLoadingInterceptor(
				entityMetamodel.getName(),
				null,
				entityMetamodel.getBytecodeEnhancementMetadata()
						.getLazyAttributesMetadata()
						.getLazyAttributeNames(),
				null
		);
		asPersistentAttributeInterceptable( entity ).$$_hibernate_setInterceptor( interceptor );
		return entity;
	}

	@Override
	public boolean isInstance(Object object, SessionFactoryImplementor sessionFactory) {
		return super.isInstance( object, sessionFactory ) ||
				//this one needed only for guessEntityMode()
				( proxyInterface!=null && proxyInterface.isInstance(object) );
	}
}
