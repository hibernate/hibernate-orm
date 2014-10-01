/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.type;

import org.hibernate.cache.spi.CacheKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.TypeFactory;
import org.hibernate.type.TypeFactory.TypeScope;

/**
 * This class mocks the behaviour of {@link TypeFactory.TypeScopeImpl} which holds a reference
 * to {@link SessionFactoryImpl}.  While SessionFactoryImpl is serializable within a JVM, it
 * is not serializable across JVM instances.  This causes a problem when serializing a
 * {@link CacheKey} instance for a composite key that is sent to other nodes in a distributed
 * cluster.  The instance of CacheKey will have a type field set to
 * {@link EmbeddedComponentType} which in turn references the not-quite-serializable
 * TypeScopeImpl.
 */
public class NonSerializableTypeScope implements TypeScope
{
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final Object notSerializable = new Object();

    @Override
    public SessionFactoryImplementor resolveFactory()
    {
        return null;
    }
}
