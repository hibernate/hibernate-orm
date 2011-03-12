/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.persister;

import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Provides persister classes based on the entity or collection role.
 * The persister class is chosen according to the following rules in decreasing priority:
 *  - the persister class defined explicitly via annotation or XML
 *  - the persister class returned by the PersisterClassProvider implementation (if not null)
 *  - the default provider as chosen by Hibernate Core (best choice most of the time)
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface PersisterClassProvider {
	/**
	 * Returns the entity persister class for a given entityName or null
	 * if the entity persister class should be the default.
	 */
	Class<? extends EntityPersister> getEntityPersisterClass(String entityName);

	/**
	 * Returns the collection persister class for a given collection role or null
	 * if the collection persister class should be the default.
	 */
	Class<? extends CollectionPersister> getCollectionPersisterClass(String collectionPersister);
}
