/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	Map getConfigurationValues();
	boolean shouldManageNamespaces();
	ExceptionHandler getExceptionHandler();
}
