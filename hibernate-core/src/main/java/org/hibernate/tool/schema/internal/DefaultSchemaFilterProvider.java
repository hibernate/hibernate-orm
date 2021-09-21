/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;

/**
 * Default implementation of the SchemaFilterProvider contract, which returns
 * {@link DefaultSchemaFilter} for all filters.
 */
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
