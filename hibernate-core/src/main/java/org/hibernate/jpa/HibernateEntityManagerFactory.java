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

import org.hibernate.Metamodel;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Contract giving access to the underlying {@link org.hibernate.SessionFactory} from an {@link javax.persistence.EntityManagerFactory}
 *
 * @author Gavin King
 *
 * @deprecated (since 5.2) Use SessionFactory (or SessionFactoryImplementor) as it now extends EntityManagerFactory directly
 */
@Deprecated
public interface HibernateEntityManagerFactory extends EntityManagerFactory, Serializable {
	/**
	 * Obtain the underlying Hibernate SessionFactory.
	 *
	 * @return The underlying Hibernate SessionFactory
	 *
	 * @deprecated The expectation is that SessionFactory implementors also implement EntityManagerFactory; so this call
	 * really should just return {@code this}
	 */
	@Deprecated
	default SessionFactoryImplementor getSessionFactory() {
		return (SessionFactoryImplementor) this;
	}

	/**
	 * Find all  {@code EntityGraph}s associated with a given entity type.
	 *
	 * @param entityClass the entity type for which to find all {@code EntityGraph}s.
	 *
	 * @return A list of {@code EntityGraph} instances associated with the given entity type. The empty list is
	 * returned in case there are not entity graphs.
	 */
	<T> List<EntityGraph<? super T>> findEntityGraphsByType(Class<T> entityClass);

	@Override
	Metamodel getMetamodel();

	/**
	 * Find an entity type by name
	 *
	 * @param entityName entity name
	 *
	 * @return the {@code EntityType} for the specified name
	 *
	 * @deprecated Use org.hibernate.MetamodelImplementor#getEntityTypeByName instead.
	 */
	@Deprecated
	default EntityType getEntityTypeByName(String entityName) {
		final EntityType entityType = getMetamodel().getEntityTypeByName( entityName );
		if ( entityType == null ) {
			throw new IllegalArgumentException( "[" + entityName + "] did not refer to EntityType" );
		}
		return entityType;
	}
}
