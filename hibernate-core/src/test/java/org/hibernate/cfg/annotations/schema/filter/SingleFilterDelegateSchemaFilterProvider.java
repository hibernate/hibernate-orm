/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations.schema.filter;

import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;

/**
 * Simple SchemaFilterProvider implementation delegating on the same filter for all modes : create/drop/migrate.
 *
 * @author Benoit Besson
 */
public class SingleFilterDelegateSchemaFilterProvider implements SchemaFilterProvider {

	private SchemaFilter filter;

	public SingleFilterDelegateSchemaFilterProvider(SchemaFilter filter) {
		this.filter = filter;
	}

	@Override
	public SchemaFilter getCreateFilter() {
		return this.filter;
	}

	@Override
	public SchemaFilter getDropFilter() {
		return this.filter;
	}

	@Override
	public SchemaFilter getMigrateFilter() {
		return this.filter;
	}

	@Override
	public SchemaFilter getValidateFilter() {
		return this.filter;
	}
}
