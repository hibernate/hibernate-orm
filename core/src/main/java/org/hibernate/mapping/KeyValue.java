//$Id: KeyValue.java 6514 2005-04-26 06:37:54Z oneovthafew $
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.IdentifierGenerator;

/**
 * Represents an identifying key of a table: the value for primary key
 * of an entity, or a foreign key of a collection or join table or
 * joined subclass table.
 * @author Gavin King
 */
public interface KeyValue extends Value {
	
	public void createForeignKeyOfEntity(String entityName);
	
	public boolean isCascadeDeleteEnabled();
	
	public boolean isIdentityColumn(Dialect dialect);
	
	public String getNullValue();
	
	public boolean isUpdateable();

	public IdentifierGenerator createIdentifierGenerator(
			Dialect dialect, 
			String defaultCatalog, 
			String defaultSchema, 
			RootClass rootClass) throws MappingException;
}
