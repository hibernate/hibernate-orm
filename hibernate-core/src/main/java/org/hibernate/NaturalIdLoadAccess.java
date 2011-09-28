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


/**
 * Loads an entity by its natural identifier
 * 
 * @author Eric Dalquist
 * @version $Revision$
 * @see org.hibernate.annotations.NaturalId
 */
public interface NaturalIdLoadAccess<T> {
    /**
     * Set the {@link LockOptions} to use when retrieving the entity.
     */
    public NaturalIdLoadAccess<T> with(LockOptions lockOptions);

    /**
     * Add a NaturalId attribute value.
     * 
     * @param attributeName The entity attribute name that is marked as a NaturalId
     * @param value The value of the attribute
     */
    public NaturalIdLoadAccess<T> using(String attributeName, Object value);

    /**
     * Same behavior as {@link Session#load(Class, java.io.Serializable)}
     * 
     * @return The entity 
     * @throws HibernateException if the entity does not exist
     */
    public T getReference();

    /**
     * Same behavior as {@link Session#get(Class, java.io.Serializable)}
     * 
     * @return The entity or null if it does not exist
     */
    public T load();

}
