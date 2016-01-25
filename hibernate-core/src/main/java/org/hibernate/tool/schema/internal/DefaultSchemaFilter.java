/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.SchemaFilter;

/**
 * Default implementation of the SchemaFilter contract, which is to just include everything.
 */
public class DefaultSchemaFilter implements SchemaFilter {
	public static final DefaultSchemaFilter INSTANCE = new DefaultSchemaFilter();

	@Override
	public boolean includeNamespace( Namespace namespace ) {
		return true;
	}

	@Override
	public boolean includeTable( Table table ) {
		return true;
	}

	@Override
	public boolean includeSequence( Sequence sequence ) {
		return true;
	}
}
