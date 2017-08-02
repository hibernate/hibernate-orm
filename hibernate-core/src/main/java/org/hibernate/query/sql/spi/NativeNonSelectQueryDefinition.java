/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import java.util.List;

import org.hibernate.sql.exec.spi.JdbcParameterBinder;

/**
 * Access the values defining a native non-select query
 *
 * @author Steve Ebersole
 */
public interface NativeNonSelectQueryDefinition {

	// todo (6.0) : would prefer to drop support for executing calls via NativeQuery at which point this can simply be replaced with JdbcMutation

	String getSqlString();
	boolean isCallable();
	List<JdbcParameterBinder> getParameterBinders();
}
