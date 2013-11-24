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
import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.jpa.internal.metamodel.EntityTypeImpl;

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
	public SessionFactory getSessionFactory();

	/**
	 * Retrieve the EntityTypeImpl by name.  Use of the Hibernate O/RM notion the "entity name" allows support
	 * for non-strictly-JPA models to be used in JPA APIs
	 *
	 * @param entityName The entity name
	 *
	 * @return The EntityTypeImpl
	 */
	public EntityTypeImpl getEntityTypeByName(String entityName);
}
