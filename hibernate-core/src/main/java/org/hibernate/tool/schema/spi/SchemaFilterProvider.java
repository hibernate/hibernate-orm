package org.hibernate.tool.schema.spi;

/**
 * Used to specify the {@link org.hibernate.tool.schema.spi.SchemaFilter}s to be used by create, drop, migrate and validate
 * operations on the database schema. These filters can be used to limit the scope of operations to specific namespaces, 
 * tables and sequences.
 * 
 * @since 5.1
 */
public interface SchemaFilterProvider {

	SchemaFilter getCreateFilter();
	
	SchemaFilter getDropFilter();
	
	SchemaFilter getMigrateFilter();
	
	SchemaFilter getValidateFilter();
    
}
