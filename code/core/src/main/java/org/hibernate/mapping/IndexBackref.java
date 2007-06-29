//$Id: IndexBackref.java 6924 2005-05-27 01:30:18Z oneovthafew $
package org.hibernate.mapping;

import org.hibernate.property.IndexPropertyAccessor;
import org.hibernate.property.PropertyAccessor;

/**
 * @author Gavin King
 */
public class IndexBackref extends Property {
	private String collectionRole;
	private String entityName;
	
	public boolean isBackRef() {
		return true;
	}
	public String getCollectionRole() {
		return collectionRole;
	}
	public void setCollectionRole(String collectionRole) {
		this.collectionRole = collectionRole;
	}

	public boolean isBasicPropertyAccessor() {
		return false;
	}

	public PropertyAccessor getPropertyAccessor(Class clazz) {
		return new IndexPropertyAccessor(collectionRole, entityName);
	}
	
	public String getEntityName() {
		return entityName;
	}
	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}
}
