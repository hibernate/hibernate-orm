package org.hibernate.tool.schema.internal;

import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;

public class DefaultSchemaFilterProvider implements SchemaFilterProvider {

	public static final DefaultSchemaFilterProvider INSTANCE = new DefaultSchemaFilterProvider();

	@Override
	public SchemaFilter getCreateFilter() {
		return DefaultSchemaFilter.INSTANCE;
	}

	@Override
	public SchemaFilter getDropFilter() {
		return DefaultSchemaFilter.INSTANCE;
	}

	@Override
	public SchemaFilter getMigrateFilter() {
		return DefaultSchemaFilter.INSTANCE;
	}

	@Override
	public SchemaFilter getValidateFilter() {
		return DefaultSchemaFilter.INSTANCE;
	}
}
