package org.hibernate.cfg.annotations.schema.entities.global;

import org.hibernate.cfg.annotations.schema.SchemaNameResolverImpl;
import org.hibernate.cfg.annotations.schema.filter.SchemaFilterImpl;

public class GlobalSchemaFilter extends SchemaFilterImpl {

	public GlobalSchemaFilter(String schemaName) {
		super( SchemaNameResolverImpl.getGlobalSchemaName() );
	}

}
