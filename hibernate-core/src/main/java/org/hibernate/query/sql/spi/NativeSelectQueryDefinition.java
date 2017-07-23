/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import java.util.List;

import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.ResultSetMapping;
import org.hibernate.sql.exec.spi.RowTransformer;

/**
 * Access the values defining a native select query
 *
 * @author Steve Ebersole
 */
public interface NativeSelectQueryDefinition<R> {
	String getSqlString();
	boolean isCallable();
	List<JdbcParameterBinder> getParameterBinders();
	ResultSetMapping getResultSetMapping();
	RowTransformer<R> getRowTransformer();
}
