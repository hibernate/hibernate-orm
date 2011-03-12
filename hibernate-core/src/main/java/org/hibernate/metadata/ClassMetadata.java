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
package org.hibernate.metadata;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.EntityMode;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;

/**
 * Exposes entity class metadata to the application
 *
 * @see org.hibernate.SessionFactory#getClassMetadata(Class)
 * @author Gavin King
 */
public interface ClassMetadata {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // stuff that is persister-centric and/or EntityInfo-centric ~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The name of the entity
	 */
	public String getEntityName();

	/**
	 * Get the name of the identifier property (or return null)
	 */
	public String getIdentifierPropertyName();

	/**
	 * Get the names of the class' persistent properties
	 */
	public String[] getPropertyNames();

	/**
	 * Get the identifier Hibernate type
	 */
	public Type getIdentifierType();

	/**
	 * Get the Hibernate types of the class properties
	 */
	public Type[] getPropertyTypes();

	/**
	 * Get the type of a particular (named) property
	 */
	public Type getPropertyType(String propertyName) throws HibernateException;

	/**
	 * Does this class support dynamic proxies?
	 */
	public boolean hasProxy();

	/**
	 * Are instances of this class mutable?
	 */
	public boolean isMutable();

	/**
	 * Are instances of this class versioned by a timestamp or version number column?
	 */
	public boolean isVersioned();

	/**
	 * Get the index of the version property
	 */
	public int getVersionProperty();

	/**
	 * Get the nullability of the class' persistent properties
	 */
	public boolean[] getPropertyNullability();


	/**
	 * Get the "laziness" of the properties of this class
	 */
	public boolean[] getPropertyLaziness();

	/**
	 * Does this class have an identifier property?
	 */
	public boolean hasIdentifierProperty();

	/**
	 * Does this entity declare a natural id?
	 */
	public boolean hasNaturalIdentifier();

	/**
	 * Which properties hold the natural id?
	 */
	public int[] getNaturalIdentifierProperties();
	
	/**
	 * Does this entity have mapped subclasses?
	 */
	public boolean hasSubclasses();
	
	/**
	 * Does this entity extend a mapped superclass?
	 */
	public boolean isInherited();
	
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// stuff that is tuplizer-centric, but is passed a session ~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Return the values of the mapped properties of the object
	 */
	public Object[] getPropertyValuesToInsert(Object entity, Map mergeMap, SessionImplementor session) 
	throws HibernateException;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// stuff that is Tuplizer-centric ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The persistent class, or null
	 */
	public Class getMappedClass(EntityMode entityMode);

	/**
	 * Create a class instance initialized with the given identifier
	 *
	 * @deprecated Use {@link #instantiate(Serializable, SessionImplementor)} instead
	 * @noinspection JavaDoc
	 */
	public Object instantiate(Serializable id, EntityMode entityMode) throws HibernateException;

	/**
	 * Create a class instance initialized with the given identifier
	 *
	 * @param id The identifier value to use (may be null to represent no value)
	 * @param session The session from which the request originated.
	 *
	 * @return The instantiated entity.
	 */
	public Object instantiate(Serializable id, SessionImplementor session);

	/**
	 * Get the value of a particular (named) property
	 */
	public Object getPropertyValue(Object object, String propertyName, EntityMode entityMode) throws HibernateException;

	/**
	 * Extract the property values from the given entity.
	 *
	 * @param entity The entity from which to extract the property values.
	 * @param entityMode The entity-mode of the given entity
	 * @return The property values.
	 * @throws HibernateException
	 */
	public Object[] getPropertyValues(Object entity, EntityMode entityMode) throws HibernateException;

	/**
	 * Set the value of a particular (named) property
	 */
	public void setPropertyValue(Object object, String propertyName, Object value, EntityMode entityMode) throws HibernateException;

	/**
	 * Set the given values to the mapped properties of the given object
	 */
	public void setPropertyValues(Object object, Object[] values, EntityMode entityMode) throws HibernateException;

	/**
	 * Get the identifier of an instance (throw an exception if no identifier property)
	 * @deprecated Use {@link #getIdentifier(Object,SessionImplementor)} instead
	 * @noinspection JavaDoc
	 */
	public Serializable getIdentifier(Object object, EntityMode entityMode) throws HibernateException;

	/**
	 * Get the identifier of an instance (throw an exception if no identifier property)
	 *
	 * @param entity The entity for which to get the identifier
	 * @param session The session from which the request originated
	 *
	 * @return The identifier
	 */
	public Serializable getIdentifier(Object entity, SessionImplementor session);

	/**
	 * Inject the identifier value into the given entity.
	 * </p>
	 * Has no effect if the entity does not define an identifier property
	 *
	 * @param entity The entity to inject with the identifier value.
	 * @param id The value to be injected as the identifier.
	 * @param entityMode The entity mode
	 *
	 * @deprecated Use {@link #setIdentifier(Object, Serializable, SessionImplementor)} instead.
	 * @noinspection JavaDoc
	 */
	public void setIdentifier(Object entity, Serializable id, EntityMode entityMode) throws HibernateException;

	/**
	 * Inject the identifier value into the given entity.
	 *
	 * @param entity The entity to inject with the identifier value.
	 * @param id The value to be injected as the identifier.
	 * @param session The session from which is requests originates
	 */
	public void setIdentifier(Object entity, Serializable id, SessionImplementor session);


	/**
	 * Does the class implement the <tt>Lifecycle</tt> interface?
	 */
	public boolean implementsLifecycle(EntityMode entityMode);

	/**
	 * Does the class implement the <tt>Validatable</tt> interface?
	 */
	public boolean implementsValidatable(EntityMode entityMode);

	/**
	 * Get the version number (or timestamp) from the object's version property
	 * (or return null if not versioned)
	 */
	public Object getVersion(Object object, EntityMode entityMode) throws HibernateException;

}
