/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import java.util.Map;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.tuple.Tuplizer;

/**
 * Defines further responsibilities regarding tuplization based on
 * a mapped entity.
 * <p/>
 * EntityTuplizer implementations should have the following constructor signatures:
 *      (org.hibernate.tuple.entity.EntityMetamodel, org.hibernate.mapping.PersistentClass)
 *      (org.hibernate.tuple.entity.EntityMetamodel, org.hibernate.metamodel.binding.EntityBinding)
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @deprecated See {@link org.hibernate.metamodel.spi.EntityRepresentationStrategy}
 */
@Deprecated
public interface EntityTuplizer extends Tuplizer {

	/**
     * Extract the identifier value from the given entity.
     *
     * @param entity The entity from which to extract the identifier value.
	 * @param session The session from which is requests originates
	 *
     * @return The identifier value.
     */
	Object getIdentifier(Object entity, SharedSessionContractImplementor session);

	/**
	 * Extract the values of the insertable properties of the entity (including backrefs)
	 *
	 * @param entity The entity from which to extract.
	 * @param mergeMap a map of instances being merged to merged instances
	 * @param session The session in which the result set is being made.
	 * @return The insertable property values.
	 * @throws HibernateException Indicates a problem access the properties
	 */
	Object[] getPropertyValuesToInsert(Object entity, Map mergeMap, SharedSessionContractImplementor session);

	/**
	 * Get any {@link EntityNameResolver EntityNameResolvers} associated with this {@link Tuplizer}.
	 *
	 * @return The associated resolvers.  May be null or empty.
	 */
	EntityNameResolver[] getEntityNameResolvers();

	/**
	 * Given an entity instance, determine the most appropriate (most targeted) entity-name which represents it.
	 * This is called in situations where we already know an entity name for the given entityInstance; we are being
	 * asked to determine if there is a more appropriate entity-name to use, specifically within an inheritance
	 * hierarchy.
	 * <p/>
	 * For example, consider a case where a user calls <tt>session.update( "Animal", cat );</tt>.  Here, the
	 * user has explicitly provided <tt>Animal</tt> as the entity-name.  However, they have passed in an instance
	 * of <tt>Cat</tt> which is a subclass of <tt>Animal</tt>.  In this case, we would return <tt>Cat</tt> as the
	 * entity-name.
	 * <p/>
	 * <tt>null</tt> may be returned from calls to this method.  The meaning of <tt>null</tt> in that case is assumed
	 * to be that we should use whatever explicit entity-name the user provided (<tt>Animal</tt> rather than <tt>Cat</tt>
	 * in the example above).
	 *
	 * @param entityInstance The entity instance.
	 * @param factory Reference to the SessionFactory.
	 *
	 * @return The most appropriate entity name to use.
	 *
	 * @throws HibernateException If we are unable to determine an entity-name within the inheritance hierarchy.
	 */
	String determineConcreteSubclassEntityName(Object entityInstance, SessionFactoryImplementor factory);

}
