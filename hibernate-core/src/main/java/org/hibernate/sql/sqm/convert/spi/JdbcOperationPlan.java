/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.convert.spi;

import java.util.List;

/**
 * Represents the information needed to perform a JDBC operation.
 *
 * @author Steve Ebersole
 * @author John O'Hara
 */
public interface JdbcOperationPlan {
	/**
	 * The SQL to be performed.
	 *
	 * @return The SQL.
	 */
	String getSql();

	/**
	 * One or more binders responsible for applying any query parameter bindings to be applied
	 * to the PreparedStatement prior to execution.
	 *
	 * @return The query parameter binders
	 */
	List<ParameterBinder> getParameterBinders();

	List<QueryOptionBinder> getQueryOptionBinders();
}
