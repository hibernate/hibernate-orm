/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.persister.entity;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * This the internal contract exposed by EntityPersister to EntityKey instances.
 * The purpose is to keep the number of fields for EntityKey to a minimum amount
 * and still be able to set it the properties listed here without having to create
 * a complete EntityPersister implementation.
 *
 * @see org.hibernate.persister.entity.EntityPersister
 * @see org.hibernate.engine.spi.EntityKey
 *
 * @author Sanne Grinovero
 */
public interface EntityEssentials {

	Type getIdentifierType();

	boolean isBatchLoadable();

	String getEntityName();

	String getRootEntityName();

	SessionFactoryImplementor getFactory();

}
