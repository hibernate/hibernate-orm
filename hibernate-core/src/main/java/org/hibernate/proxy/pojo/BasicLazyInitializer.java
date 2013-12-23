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
 * @author Aleksander Dukhno
 */
public abstract class BasicLazyInitializer extends AbstractLazyInitializer {

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

	public abstract Object serializableProxy();

	public final Object getReplacement() {
		return replacement;
	}

	public final boolean isReplacementNull() {
		return replacement == null;
	}

	public final void generateReplacement() {
		replacement = serializableProxy();
	}

	public final boolean isOverridesEquals() {
		return overridesEquals;
	}

	public final Method getGetIdentifierMethod() {
		return getIdentifierMethod;
	}

	public final Method getSetIdentifierMethod() {
		return setIdentifierMethod;
	}

	public final CompositeType getComponentIdType() {
		return componentIdType;
	}

	public final Class getPersistentClass() {
		return persistentClass;
	}

}
