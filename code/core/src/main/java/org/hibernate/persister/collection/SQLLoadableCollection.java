package org.hibernate.persister.collection;

public interface SQLLoadableCollection extends QueryableCollection {

	public abstract String[] getCollectionPropertyColumnAliases(String propertyName, String string);
	
	public abstract String getIdentifierColumnName();
	
}
