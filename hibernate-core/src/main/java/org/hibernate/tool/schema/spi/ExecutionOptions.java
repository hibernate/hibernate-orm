/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import java.util.Map;

import org.hibernate.Incubating;
import org.hibernate.boot.model.relational.Exportable;

/**
 * Parameter object representing options for schema management tool execution
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ExecutionOptions {
	Map<String,Object> getConfigurationValues();

	boolean shouldManageNamespaces();

	ExceptionHandler getExceptionHandler();

	/**
	 * @deprecated No longer used, see {@link org.hibernate.cfg.SchemaToolingSettings#HBM2DDL_FILTER_PROVIDER}
	 */
	@Deprecated( forRemoval = true )
	default SchemaFilter getSchemaFilter() {
		throw new UnsupportedOperationException();
	}
}
