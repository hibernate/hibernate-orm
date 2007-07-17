//$Id: SerializableProxy.java 9210 2006-02-03 22:15:19Z steveebersole $
package org.hibernate.proxy.pojo.cglib;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.hibernate.HibernateException;
import org.hibernate.type.AbstractComponentType;

/**
 * Serializable placeholder for <tt>CGLIB</tt> proxies
 */
public final class SerializableProxy implements Serializable {

	private String entityName;
	private Class persistentClass;
	private Class[] interfaces;
	private Serializable id;
	private Class getIdentifierMethodClass;
	private Class setIdentifierMethodClass;
	private String getIdentifierMethodName;
	private String setIdentifierMethodName;
	private Class[] setIdentifierMethodParams;
	private AbstractComponentType componentIdType;

	public SerializableProxy() {}

	public SerializableProxy(
		final String entityName,
		final Class persistentClass,
		final Class[] interfaces,
		final Serializable id,
		final Method getIdentifierMethod,
		final Method setIdentifierMethod,
		AbstractComponentType componentIdType
	) {
		this.entityName = entityName;
		this.persistentClass = persistentClass;
		this.interfaces = interfaces;
		this.id = id;
		if (getIdentifierMethod!=null) {
			getIdentifierMethodClass = getIdentifierMethod.getDeclaringClass();
			getIdentifierMethodName = getIdentifierMethod.getName();
		}
		if (setIdentifierMethod!=null) {
			setIdentifierMethodClass = setIdentifierMethod.getDeclaringClass();
			setIdentifierMethodName = setIdentifierMethod.getName();
			setIdentifierMethodParams = setIdentifierMethod.getParameterTypes();
		}
		this.componentIdType = componentIdType;
	}

	private Object readResolve() {
		try {
			return CGLIBLazyInitializer.getProxy(
				entityName,
				persistentClass,
				interfaces,
				getIdentifierMethodName==null ?
					null :
					getIdentifierMethodClass.getDeclaredMethod(getIdentifierMethodName, null),
				setIdentifierMethodName==null ?
					null :
					setIdentifierMethodClass.getDeclaredMethod(setIdentifierMethodName, setIdentifierMethodParams),
					componentIdType,
				id,
				null
			);
		}
		catch (NoSuchMethodException nsme) {
			throw new HibernateException("could not create proxy for entity: " + entityName, nsme);
		}
	}

}
