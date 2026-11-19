package org.hibernate.metamodel.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.reflect.Constructor;

import org.hibernate.InstantiationException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptableType;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.ReflectHelper.getDefaultConstructor;

/**
 * Support for instantiating entity values as POJO representation
 */
public class EntityInstantiatorPojoStandard extends AbstractEntityInstantiatorPojo {

	private final Class<?> proxyInterface;
	private final boolean applyBytecodeInterception;
	private final LazyAttributeLoadingInterceptor.EntityRelatedState loadingInterceptorState;
	private final Constructor<?> constructor;

	public EntityInstantiatorPojoStandard(
			@Nonnull EntityPersister persister,
			@Nonnull PersistentClass persistentClass,
			@Nonnull JavaType<?> javaType) {
		super( persister, persistentClass, javaType );
		proxyInterface = persistentClass.getProxyInterface();
		constructor = isAbstract() ? null : resolveConstructor( getMappedPojoClass() );
		applyBytecodeInterception = isPersistentAttributeInterceptableType( persistentClass.getMappedClass() );
		if ( applyBytecodeInterception ) {
			loadingInterceptorState = new LazyAttributeLoadingInterceptor.EntityRelatedState(
					persister.getEntityName(),
					persister.getBytecodeEnhancementMetadata()
							.getLazyAttributesMetadata()
							.getLazyAttributeNames()
			);
		}
		else {
			loadingInterceptorState = null;
		}
	}

	@Nullable
	protected static Constructor<?> resolveConstructor(@Nonnull Class<?> mappedPojoClass) {
		try {
			return getDefaultConstructor( mappedPojoClass);
		}
		catch ( PropertyNotFoundException e ) {
			CORE_LOGGER.noDefaultConstructor( mappedPojoClass.getName() );
			return null;
		}
	}

	@Override
	public boolean canBeInstantiated() {
		return constructor != null;
	}

	@Override
	@Nonnull
	protected Object applyInterception(@Nonnull Object entity) {
		if ( applyBytecodeInterception ) {
			asPersistentAttributeInterceptable( entity )
					.$$_hibernate_setInterceptor( new LazyAttributeLoadingInterceptor(
							loadingInterceptorState,
							null,
							null
					) );
		}
		return entity;

	}

	@Override
	public boolean isInstance(@Nullable Object object) {
		return super.isInstance( object )
			// this one needed only for guessEntityMode()
			|| proxyInterface != null && proxyInterface.isInstance( object );
	}

	@Override
	@Nonnull
	public Object instantiate() {
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
