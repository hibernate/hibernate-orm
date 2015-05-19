/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
