//$Id: ProxyFactory.java 9210 2006-02-03 22:15:19Z steveebersole $
package org.hibernate.proxy;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.AbstractComponentType;

/**
 * Contract for run-time, proxy-based lazy initialization proxies.
 *
 * @author Gavin King
 */
public interface ProxyFactory {

	/**
	 * Called immediately after instantiation of this factory.
	 * <p/>
	 * Essentially equivalent to contructor injection, but contracted
	 * here via interface.
	 *
	 * @param entityName The name of the entity for which this factory should
	 * generate proxies.
	 * @param persistentClass The entity class for which to generate proxies;
	 * not always the same as the entityName.
	 * @param interfaces The interfaces to expose in the generated proxy;
	 * {@link HibernateProxy} is already included in this collection.
	 * @param getIdentifierMethod Reference to the identifier getter method;
	 * invocation on this method should not force initialization
	 * @param setIdentifierMethod Reference to the identifier setter method;
	 * invocation on this method should not force initialization
	 * @param componentIdType For composite identifier types, a reference to
	 * the {@link org.hibernate.type.ComponentType type} of the identifier
	 * property; again accessing the id should generally not cause
	 * initialization - but need to bear in mind <key-many-to-one/>
	 * mappings.
	 * @throws HibernateException Indicates a problem completing post
	 * instantiation initialization.
	 */
	public void postInstantiate(
			String entityName,
	        Class persistentClass,
	        Set interfaces,
	        Method getIdentifierMethod,
	        Method setIdentifierMethod,
	        AbstractComponentType componentIdType) throws HibernateException;

	/**
	 * Create a new proxy instance
	 *
	 * @param id The id value for the proxy to be generated.
	 * @param session The session to which the generated proxy will be
	 * associated.
	 * @return The generated proxy.
	 * @throws HibernateException Indicates problems generating the requested
	 * proxy.
	 */
	public HibernateProxy getProxy(Serializable id,SessionImplementor session) throws HibernateException;

}
