package org.hibernate.cfg.annotations.schema.entities.dynamic;

import org.hibernate.cfg.annotations.schema.SchemaNameResolverImpl;
import org.hibernate.cfg.annotations.schema.filter.SchemaFilterImpl;


public class DynamicSchemaFilter extends SchemaFilterImpl {

	public DynamicSchemaFilter(String schemaName) {
		super( SchemaNameResolverImpl.getDynamicSchemaName() );
	}

}
