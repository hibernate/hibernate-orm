/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Constructor;

import org.hibernate.InstantiationException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptableType;

/**
 * Support for instantiating entity values as POJO representation
 */
public class EntityInstantiatorPojoStandard extends AbstractEntityInstantiatorPojo {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( EntityInstantiatorPojoStandard.class );

	private final EntityMetamodel entityMetamodel;
	private final Class<?> proxyInterface;
	private final boolean applyBytecodeInterception;

	private final Constructor<?> constructor;

	public EntityInstantiatorPojoStandard(
			EntityMetamodel entityMetamodel,
			PersistentClass persistentClass,
			JavaType<?> javaType) {
		super( entityMetamodel, persistentClass, javaType );
		this.entityMetamodel = entityMetamodel;
		proxyInterface = persistentClass.getProxyInterface();
		constructor = isAbstract() ? null : resolveConstructor( getMappedPojoClass() );
		applyBytecodeInterception = isPersistentAttributeInterceptableType( persistentClass.getMappedClass() );
	}

	protected static Constructor<?> resolveConstructor(Class<?> mappedPojoClass) {
		try {
			return ReflectHelper.getDefaultConstructor( mappedPojoClass);
		}
		catch ( PropertyNotFoundException e ) {
			LOG.noDefaultConstructor( mappedPojoClass.getName() );
			return null;
		}
	}

	@Override
	public boolean canBeInstantiated() {
		return constructor != null;
	}

	@Override
	protected Object applyInterception(Object entity) {
		if ( applyBytecodeInterception ) {
			asPersistentAttributeInterceptable( entity )
					.$$_hibernate_setInterceptor( new LazyAttributeLoadingInterceptor(
							entityMetamodel.getName(),
							null,
							entityMetamodel.getBytecodeEnhancementMetadata()
									.getLazyAttributesMetadata()
									.getLazyAttributeNames(),
							null
					) );
		}
		return entity;

	}

	@Override
	public boolean isInstance(Object object, SessionFactoryImplementor sessionFactory) {
		return super.isInstance( object, sessionFactory )
			//this one needed only for guessEntityMode()
			|| proxyInterface != null && proxyInterface.isInstance( object );
	}

	@Override
	public Object instantiate(SessionFactoryImplementor sessionFactory) {
		if ( isAbstract() ) {
			throw new InstantiationException( "Cannot instantiate abstract class or interface", getMappedPojoClass() );
		}
		else if ( constructor == null ) {
			throw new InstantiationException( "No default constructor for entity", getMappedPojoClass() );
		}
		else {
			try {
				return applyInterception( constructor.newInstance( (Object[]) null ) );
			}
			catch ( Exception e ) {
				throw new InstantiationException( "Could not instantiate entity", getMappedPojoClass(), e );
			}
		}
	}
}
