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
package org.hibernate.tuple.entity;
import java.io.Serializable;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.property.Getter;
import org.hibernate.tuple.Tuplizer;

/**
 * Defines further responsibilities regarding tuplization based on
 * a mapped entity.
 * <p/>
 * EntityTuplizer implementations should define a constructor with the following arguments:<ol>
 *     <li>{@link org.hibernate.service.ServiceRegistry serviceRegistry}</li>
 *     <li>{@link org.hibernate.tuple.entity.EntityMetamodel}</li>
 *     <li>{@link org.hibernate.metamodel.spi.binding.EntityBinding}</li>
 * </ol>
 * An easy way to check this is to extend the {@link org.hibernate.tuple.entity.AbstractEntityTuplizer} class.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface EntityTuplizer extends Tuplizer {
	/**
	 * Return the entity-mode handled by this tuplizer instance.
	 *
	 * @return The entity-mode
	 */
	public EntityMode getEntityMode();

    /**
     * Create an entity instance initialized with the given identifier.
     *
     * @param id The identifier value for the entity to be instantiated.
     * @return The instantiated entity.
     * @throws HibernateException
	 *
	 * @deprecated Use {@link #instantiate(Serializable, SessionImplementor)} instead.
     */
	@SuppressWarnings( {"JavaDoc"})
	public Object instantiate(Serializable id) throws HibernateException;

    /**
     * Create an entity instance initialized with the given identifier.
     *
     * @param id The identifier value for the entity to be instantiated.
	 * @param session The session from which is requests originates
	 *
     * @return The instantiated entity.
     */
	public Object instantiate(Serializable id, SessionImplementor session);

    /**
     * Extract the identifier value from the given entity.
     *
     * @param entity The entity from which to extract the identifier value.
	 *
     * @return The identifier value.
	 *
     * @throws HibernateException If the entity does not define an identifier property, or an
     * error occurs accessing its value.
	 *
	 * @deprecated Use {@link #getIdentifier(Object,SessionImplementor)} instead.
     */
	public Serializable getIdentifier(Object entity) throws HibernateException;

    /**
     * Extract the identifier value from the given entity.
     *
     * @param entity The entity from which to extract the identifier value.
	 * @param session The session from which is requests originates
	 *
     * @return The identifier value.
     */
	public Serializable getIdentifier(Object entity, SessionImplementor session);

    /**
     * Inject the identifier value into the given entity.
     * </p>
     * Has no effect if the entity does not define an identifier property
     *
     * @param entity The entity to inject with the identifier value.
     * @param id The value to be injected as the identifier.
	 *
	 * @deprecated Use {@link #setIdentifier(Object, Serializable, SessionImplementor)} instead.
     */
	@SuppressWarnings( {"JavaDoc"})
	public void setIdentifier(Object entity, Serializable id) throws HibernateException;

    /**
     * Inject the identifier value into the given entity.
     * </p>
     * Has no effect if the entity does not define an identifier property
     *
     * @param entity The entity to inject with the identifier value.
     * @param id The value to be injected as the identifier.
	 * @param session The session from which is requests originates
     */
	public void setIdentifier(Object entity, Serializable id, SessionImplementor session);

	/**
	 * Inject the given identifier and version into the entity, in order to
	 * "roll back" to their original values.
	 *
	 * @param entity The entity for which to reset the id/version values
	 * @param currentId The identifier value to inject into the entity.
	 * @param currentVersion The version value to inject into the entity.
	 *
	 * @deprecated Use {@link #resetIdentifier(Object, Serializable, Object, SessionImplementor)} instead
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion);

	/**
	 * Inject the given identifier and version into the entity, in order to
	 * "roll back" to their original values.
	 *
	 * @param entity The entity for which to reset the id/version values
	 * @param currentId The identifier value to inject into the entity.
	 * @param currentVersion The version value to inject into the entity.
	 * @param session The session from which the request originated
	 */
	public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion, SessionImplementor session);

    /**
     * Extract the value of the version property from the given entity.
     *
     * @param entity The entity from which to extract the version value.
     * @return The value of the version property, or null if not versioned.
	 * @throws HibernateException Indicates a problem accessing the version property
     */
	public Object getVersion(Object entity) throws HibernateException;

	/**
	 * Inject the value of a particular property.
	 *
	 * @param entity The entity into which to inject the value.
	 * @param i The property's index.
	 * @param value The property value to inject.
	 * @throws HibernateException Indicates a problem access the property
	 */
	public void setPropertyValue(Object entity, int i, Object value) throws HibernateException;

	/**
	 * Inject the value of a particular property.
	 *
	 * @param entity The entity into which to inject the value.
	 * @param propertyName The name of the property.
	 * @param value The property value to inject.
	 * @throws HibernateException Indicates a problem access the property
	 */
	public void setPropertyValue(Object entity, String propertyName, Object value) throws HibernateException;

	/**
	 * Extract the values of the insertable properties of the entity (including backrefs)
	 *
	 * @param entity The entity from which to extract.
	 * @param mergeMap a map of instances being merged to merged instances
	 * @param session The session in which the resuest is being made.
	 * @return The insertable property values.
	 * @throws HibernateException Indicates a problem access the properties
	 */
	public Object[] getPropertyValuesToInsert(Object entity, Map mergeMap, SessionImplementor session)
	throws HibernateException;

	/**
	 * Extract the value of a particular property from the given entity.
	 *
	 * @param entity The entity from which to extract the property value.
	 * @param propertyName The name of the property for which to extract the value.
	 * @return The current value of the given property on the given entity.
	 * @throws HibernateException Indicates a problem access the property
	 */
	public Object getPropertyValue(Object entity, String propertyName) throws HibernateException;

    /**
     * Called just after the entities properties have been initialized.
     *
     * @param entity The entity being initialized.
     * @param lazyPropertiesAreUnfetched Are defined lazy properties currently unfecthed
     * @param session The session initializing this entity.
     */
	public void afterInitialize(Object entity, boolean lazyPropertiesAreUnfetched, SessionImplementor session);

	/**
	 * Does this entity, for this mode, present a possibility for proxying?
	 *
	 * @return True if this tuplizer can generate proxies for this entity.
	 */
	public boolean hasProxy();

	/**
	 * Generates an appropriate proxy representation of this entity for this
	 * entity-mode.
	 *
	 * @param id The id of the instance for which to generate a proxy.
	 * @param session The session to which the proxy should be bound.
	 * @return The generate proxies.
	 * @throws HibernateException Indicates an error generating the proxy.
	 */
	public Object createProxy(Serializable id, SessionImplementor session) throws HibernateException;

	/**
	 * Does the {@link #getMappedClass() class} managed by this tuplizer implement
	 * the {@link org.hibernate.classic.Lifecycle} interface.
	 *
	 * @return True if the Lifecycle interface is implemented; false otherwise.
	 */
	public boolean isLifecycleImplementor();

	/**
	 * Returns the java class to which generated proxies will be typed.
	 * <p/>
	 * todo : look at fully encapsulating {@link org.hibernate.engine.spi.PersistenceContext#narrowProxy} here,
	 * since that is the only external use of this method
	 *
	 * @return The java class to which generated proxies will be typed
	 */
	public Class getConcreteProxyClass();
	
    /**
     * Does the given entity instance have any currently uninitialized lazy properties?
     *
     * @param entity The entity to be check for uninitialized lazy properties.
     * @return True if uninitialized lazy properties were found; false otherwise.
     */
	public boolean hasUninitializedLazyProperties(Object entity);
	
	/**
	 * Is it an instrumented POJO?
	 *
	 * @return {@code true} if the entity class is instrumented; {@code false} otherwise.
	 */
	public boolean isInstrumented();

	/**
	 * Get any {@link EntityNameResolver EntityNameResolvers} associated with this {@link Tuplizer}.
	 *
	 * @return The associated resolvers.  May be null or empty.
	 */
	public EntityNameResolver[] getEntityNameResolvers();

	/**
	 * Given an entity instance, determine the most appropriate (most targeted) entity-name which represents it.
	 * This is called in situations where we already know an entity name for the given entityInstance; we are being
	 * asked to determine if there is a more appropriate entity-name to use, specifically within an inheritence
	 * hierarchy.
	 * <p/>
	 * For example, consider a case where a user calls <tt>session.update( "Animal", cat );</tt>.  Here, the
	 * user has explicitly provided <tt>Animal</tt> as the entity-name.  However, they have passed in an instance
	 * of <tt>Cat</tt> which is a subclass of <tt>Animal</tt>.  In this case, we would return <tt>Cat</tt> as the
	 * entity-name.
	 * <p/>
	 * <tt>null</tt> may be returned from calls to this method.  The meaining of <tt>null</tt> in that case is assumed
	 * to be that we should use whatever explicit entity-name the user provided (<tt>Animal</tt> rather than <tt>Cat</tt>
	 * in the example above).
	 *
	 * @param entityInstance The entity instance.
	 * @param factory Reference to the SessionFactory.
	 *
	 * @return The most appropriate entity name to use.
	 *
	 * @throws HibernateException If we are unable to determine an entity-name within the inheritence hierarchy.
	 */
	public String determineConcreteSubclassEntityName(Object entityInstance, SessionFactoryImplementor factory);

	/**
	 * Retrieve the getter for the identifier property.  May return null.
	 *
	 * @return The getter for the identifier property.
	 */
	public Getter getIdentifierGetter();

	/**
	 * Retrieve the getter for the version property.  May return null.
	 *
	 * @return The getter for the version property.
	 */
	public Getter getVersionGetter();
}
