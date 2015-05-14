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
