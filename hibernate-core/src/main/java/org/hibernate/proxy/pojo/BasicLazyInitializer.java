/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.proxy.pojo;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.proxy.AbstractLazyInitializer;
import org.hibernate.type.CompositeType;

/**
 * Lazy initializer for POJOs
 *
 * @author Gavin King
 */
public abstract class BasicLazyInitializer extends AbstractLazyInitializer {

	protected static final Object INVOKE_IMPLEMENTATION = new MarkerObject("INVOKE_IMPLEMENTATION");

	protected final Class persistentClass;
	protected final Method getIdentifierMethod;
	protected final Method setIdentifierMethod;
	protected final boolean overridesEquals;
	protected final CompositeType componentIdType;

	private Object replacement;

	protected BasicLazyInitializer(
			String entityName,
	        Class persistentClass,
	        Serializable id,
	        Method getIdentifierMethod,
	        Method setIdentifierMethod,
	        CompositeType componentIdType,
	        SessionImplementor session,
	        boolean overridesEquals) {
		super(entityName, id, session);
		this.persistentClass = persistentClass;
		this.getIdentifierMethod = getIdentifierMethod;
		this.setIdentifierMethod = setIdentifierMethod;
		this.componentIdType = componentIdType;
		this.overridesEquals = overridesEquals;
	}

	protected abstract Object serializableProxy();

	@SuppressWarnings({ "UnnecessaryBoxing" })
	protected final Object invoke(Method method, Object[] args, Object proxy) throws Throwable {
		String methodName = method.getName();
		int params = args.length;

		if ( params==0 ) {
			if ( "writeReplace".equals(methodName) ) {
				return getReplacement();
			}
			else if ( !overridesEquals && "hashCode".equals(methodName) ) {
				return Integer.valueOf( System.identityHashCode(proxy) );
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
				return args[0]==proxy;
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
			final EntityKey key = session.generateEntityKey(
					getIdentifier(),
					session.getFactory().getEntityPersister( getEntityName() )
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
