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
package org.hibernate.engine.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityEssentials;
import org.hibernate.type.Type;


public class EssentialEntityPersisterDetails implements EntityEssentials {

	private final Type identifierType;
	private final boolean isBatchLoadable;
	private final String entityName;
	private final String rootEntityName;

	public EssentialEntityPersisterDetails(Type identifierType, boolean isBatchLoadable, String entityName, String rootEntityName) {
		this.identifierType = identifierType;
		this.isBatchLoadable = isBatchLoadable;
		this.entityName = entityName;
		this.rootEntityName = rootEntityName;
	}

	@Override
	public Type getIdentifierType() {
		return identifierType;
	}

	@Override
	public boolean isBatchLoadable() {
		return isBatchLoadable;
	}

	@Override
	public String getEntityName() {
		return entityName;
	}

	@Override
	public String getRootEntityName() {
		return rootEntityName;
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return null;
	}

}
