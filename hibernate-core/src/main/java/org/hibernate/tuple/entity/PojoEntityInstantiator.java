/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.engine.internal.ManagedTypeHelper;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.tuple.PojoInstantiator;

/**
 * @author Steve Ebersole
 */
public class PojoEntityInstantiator extends PojoInstantiator {
	private final EntityMetamodel entityMetamodel;
	private final Class proxyInterface;
	private final boolean applyBytecodeInterception;

	public PojoEntityInstantiator(
			EntityMetamodel entityMetamodel,
			PersistentClass persistentClass,
			ReflectionOptimizer.InstantiationOptimizer optimizer) {
		super(
				persistentClass.getMappedClass(),
				optimizer,
				persistentClass.hasEmbeddedIdentifier()
		);
		this.entityMetamodel = entityMetamodel;

		this.proxyInterface = persistentClass.getProxyInterface();

		//TODO this PojoEntityInstantiator appears to not be reused ?!
		this.applyBytecodeInterception = ManagedTypeHelper.isPersistentAttributeInterceptableType( persistentClass.getMappedClass() );
	}

	@Override
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
		ManagedTypeHelper.asPersistentAttributeInterceptable( entity ).$$_hibernate_setInterceptor( interceptor );
		return entity;
	}

	@Override
	public boolean isInstance(Object object) {
		return super.isInstance( object ) ||
				//this one needed only for guessEntityMode()
				( proxyInterface!=null && proxyInterface.isInstance(object) );
	}

}
