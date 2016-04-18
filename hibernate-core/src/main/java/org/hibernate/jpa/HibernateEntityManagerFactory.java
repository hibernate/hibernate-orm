/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa;

import java.io.Serializable;
import java.util.List;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Contract giving access to the underlying {@link org.hibernate.SessionFactory} from an {@link javax.persistence.EntityManagerFactory}
 *
 * @author Gavin King
 */
public interface HibernateEntityManagerFactory extends EntityManagerFactory, Serializable {
	/**
	 * Obtain the underlying Hibernate SessionFactory.
	 *
	 * @return The underlying Hibernate SessionFactory
	 */
	SessionFactoryImplementor getSessionFactory();

	/**
	 * Find all  {@code EntityGraph}s associated with a given entity type.
	 *
	 * @param entityClass the entity type for which to find all {@code EntityGraph}s.
	 *
	 * @return A list of {@code EntityGraph} instances associated with the given entity type. The empty list is
	 * returned in case there are not entity graphs.
	 */
	<T> List<EntityGraph<? super T>> findEntityGraphsByType(Class<T> entityClass);

	/**
	 * Returns the name of the factory. The name is either can be specified via the property <i>hibernate.ejb.entitymanager_factory_name</i>.
	 * If the property is not set the persistence unit name is used. If persistence unit name is not available, a unique
	 * name will be generated.
	 *
	 * @return the name of the factory.
	 */
	String getEntityManagerFactoryName();

	/**
	 * Find an entity type by name
	 *
	 * @param entityName entity name
	 * @return the {@code EntityType} for the specified name
	 */
	EntityType getEntityTypeByName(String entityName);
}
