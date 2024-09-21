/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import java.util.Map;

import org.hibernate.Incubating;

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
