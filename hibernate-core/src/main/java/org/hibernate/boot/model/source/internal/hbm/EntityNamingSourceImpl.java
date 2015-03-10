/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.model.source.spi.EntityNamingSource;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PersistentClass;

/**
 * Implementation of EntityNamingSource
 *
 * @author Steve Ebersole
 */
class EntityNamingSourceImpl implements EntityNamingSource {
	private final String entityName;
	private final String className;
	private final String jpaEntityName;

	private final String typeName;

	public EntityNamingSourceImpl(String entityName, String className, String jpaEntityName) {
		this.entityName = entityName;
		this.className = className;
		this.jpaEntityName = jpaEntityName;

		this.typeName = StringHelper.isNotEmpty( className ) ? className : entityName;
	}

	public EntityNamingSourceImpl(PersistentClass entityBinding) {
		this( entityBinding.getEntityName(), entityBinding.getClassName(), entityBinding.getJpaEntityName() );
	}

	@Override
	public String getEntityName() {
		return entityName;
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public String getJpaEntityName() {
		return jpaEntityName;
	}

	@Override
	public String getTypeName() {
		return typeName;
	}
}
