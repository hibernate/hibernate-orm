package org.hibernate.metamodel.internal;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.CompositeType;

import java.lang.reflect.Method;
import java.util.Set;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

/// ProxyFactory for cases where we need a non-null ProxyFactory, but will never be able to
/// create a runtime proxy because the entity default constructor is not accessible.
///
/// @since 8.0
/// @author Steve Ebersole
public class InaccessibleConstructorProxyFactory implements ProxyFactory {
	private final String entityName;

	public InaccessibleConstructorProxyFactory(String entityName) {
		this.entityName = entityName;
		CORE_LOGGER.missingProxyableConstructor( entityName );
	}

	@Override
	public HibernateProxy getProxy(Object id, SharedSessionContractImplementor session) throws HibernateException {
		throw new HibernateException(
				"Unable to create runtime proxy due to missing public, protected or package-private default constructor: " + entityName
		);
	}

	@Override
	public void postInstantiate(
			String entityName,
			Class<?> persistentClass,
			Set<Class<?>> interfaces,
			Method getIdentifierMethod,
			Method setIdentifierMethod,
			CompositeType componentIdType) throws HibernateException {
	}
}
