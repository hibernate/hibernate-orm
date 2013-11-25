/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.mapping;

import org.hibernate.property.BackrefPropertyAccessor;
import org.hibernate.property.PropertyAccessor;

/**
 * @author Gavin King
 */
public class Backref extends Property {
	private String collectionRole;
	private String entityName;

	@Override
	public boolean isBackRef() {
		return true;
	}

	@Override
	public boolean isSynthetic() {
		return true;
	}

	public String getCollectionRole() {
		return collectionRole;
	}

	public void setCollectionRole(String collectionRole) {
		this.collectionRole = collectionRole;
	}

	@Override
	public boolean isBasicPropertyAccessor() {
		return false;
	}

	@Override
	public PropertyAccessor getPropertyAccessor(Class clazz) {
		return new BackrefPropertyAccessor(collectionRole, entityName);
	}
	
	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}
}
