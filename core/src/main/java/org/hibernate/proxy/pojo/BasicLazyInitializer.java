/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.proxy.pojo;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.hibernate.engine.EntityKey;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.util.MarkerObject;
import org.hibernate.util.ReflectHelper;
import org.hibernate.proxy.AbstractLazyInitializer;

/**
 * Lazy initializer for POJOs
 * 
 * @author Gavin King
 */
public abstract class BasicLazyInitializer extends AbstractLazyInitializer {

	protected static final Object INVOKE_IMPLEMENTATION = new MarkerObject("INVOKE_IMPLEMENTATION");

	protected Class persistentClass;
	protected Method getIdentifierMethod;
	protected Method setIdentifierMethod;
	protected boolean overridesEquals;
	private Object replacement;
	protected AbstractComponentType componentIdType;

	protected BasicLazyInitializer(
			String entityName,
	        Class persistentClass,
	        Serializable id,
	        Method getIdentifierMethod,
	        Method setIdentifierMethod,
	        AbstractComponentType componentIdType,
	        SessionImplementor session) {
		super(entityName, id, session);
		this.persistentClass = persistentClass;
		this.getIdentifierMethod = getIdentifierMethod;
		this.setIdentifierMethod = setIdentifierMethod;
		this.componentIdType = componentIdType;
		overridesEquals = ReflectHelper.overridesEquals(persistentClass);
	}

	protected abstract Object serializableProxy();

	protected final Object invoke(Method method, Object[] args, Object proxy) throws Throwable {

		String methodName = method.getName();
		int params = args.length;

		if ( params==0 ) {

			if ( "writeReplace".equals(methodName) ) {
				return getReplacement();
			}
			else if ( !overridesEquals && "hashCode".equals(methodName) ) {
				return new Integer( System.identityHashCode(proxy) );
			}
			else if ( isUninitialized() && method.equals(getIdentifierMethod) ) {
				return getIdentifier();
			}

			else if ( "getHibernateLazyInitializer".equals(methodName) ) {
				return this;
			}

		}
		else if ( params==1 ) {

			if ( !overridesEquals && "equals".equals(methodName) ) {
				return args[0]==proxy ? Boolean.TRUE : Boolean.FALSE;
			}
			else if ( method.equals(setIdentifierMethod) ) {
				initialize();
				setIdentifier( (Serializable) args[0] );
				return INVOKE_IMPLEMENTATION;
			}

		}

		//if it is a property of an embedded component, invoke on the "identifier"
		if ( componentIdType!=null && componentIdType.isMethodOf(method) ) {
			return method.invoke( getIdentifier(), args );
		}

		// otherwise:
		return INVOKE_IMPLEMENTATION;

	}

	private Object getReplacement() {

		final SessionImplementor session = getSession();
		if ( isUninitialized() && session != null && session.isOpen()) {
			final EntityKey key = new EntityKey(
					getIdentifier(),
			        session.getFactory().getEntityPersister( getEntityName() ),
			        session.getEntityMode()
				);
			final Object entity = session.getPersistenceContext().getEntity(key);
			if (entity!=null) setImplementation( entity );
		}

		if ( isUninitialized() ) {
			if (replacement==null) {
				replacement = serializableProxy();
			}
			return replacement;
		}
		else {
			return getTarget();
		}

	}

	public final Class getPersistentClass() {
		return persistentClass;
	}

}
