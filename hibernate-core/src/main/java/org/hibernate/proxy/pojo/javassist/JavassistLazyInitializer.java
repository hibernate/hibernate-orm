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
package org.hibernate.proxy.pojo.javassist;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.proxy.pojo.BasicLazyInitializer;
import org.hibernate.type.CompositeType;

/**
 * A Javassist-based lazy initializer proxy.
 *
 * @author Muga Nishizawa
 * @author Aleksander Dukhno
 */
public class JavassistLazyInitializer extends BasicLazyInitializer {

	private Class[] interfaces;

	JavassistLazyInitializer(
			final String entityName,
			final Class persistentClass,
			final Class[] interfaces,
			final Serializable id,
			final Method getIdentifierMethod,
			final Method setIdentifierMethod,
			final CompositeType componentIdType,
			final SessionImplementor session,
			final boolean overridesEquals) {
		super( entityName, persistentClass, id, getIdentifierMethod, setIdentifierMethod, componentIdType, session, overridesEquals );
		this.interfaces = interfaces;
	}

	@Override
	public Object serializableProxy() {
		return new SerializableProxy(
				getEntityName(),
				persistentClass,
				interfaces,
				getIdentifier(),
				( isReadOnlySettingAvailable() ? Boolean.valueOf( isReadOnly() ) : isReadOnlyBeforeAttachedToSession() ),
				getIdentifierMethod,
				setIdentifierMethod,
				componentIdType
		);
	}
}
