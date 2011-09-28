/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate;

import java.io.Serializable;

/**
 * Loads an entity by its primary identifier
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public interface IdentifierLoadAccess<T> {
    /**
     * Set the {@link LockOptions} to use when retrieving the entity.
     */
    public IdentifierLoadAccess<T> with(LockOptions lockOptions);

    /**
     * Same behavior as {@link Session#load(Class, java.io.Serializable)}
     * 
     * @param id The primary key of the entity
     * @return The entity 
     * @throws HibernateException if the entity does not exist
     */
    public T getReference(Serializable id);

    /**
     * Same behavior as {@link Session#get(Class, java.io.Serializable)}
     * 
     * @param id The primary key of the entity
     * @return The entity or null if it does not exist
     */
    public T load(Serializable id);
}
