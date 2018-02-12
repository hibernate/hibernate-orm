package org.hibernate.cfg.annotations.schema;

import org.hibernate.cfg.schema.SchemaNameResolver;

public class SchemaNameResolverImpl implements SchemaNameResolver {

	public static final String SCHEMA_DYNAMIC_NAME_PROPERTY_NAME = "schemaDynamicName";
	private static final String SCHEMA_GLOBAL_NAME = "global";

	@Override
	public String getSchema(Class<?> clazz) {
		String packageName = clazz.getPackage().getName();
		String schemaName = EMPTY_SCHEMA_NAME;
		if ( packageName.contains( "dynamic" ) ) {
			schemaName = getDynamicSchemaName();
		}
		if ( clazz.getPackage().getName().contains( SCHEMA_GLOBAL_NAME ) ) {
			schemaName = getGlobalSchemaName();
		}
		return schemaName;
	}
	
	public static String getDynamicSchemaName()
	{
		return System.getenv( SCHEMA_DYNAMIC_NAME_PROPERTY_NAME );
	}
	
	public static String getGlobalSchemaName()
	{
		return SCHEMA_GLOBAL_NAME;
	}

}
