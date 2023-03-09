/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.dialect.Dialect;
import org.hibernate.service.Service;
import org.hibernate.sql.ast.internal.JdbcParameterRendererStandard;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Extension point, intended for use from Hibernate Reactive, to render JDBC
 * parameter placeholders into the SQL query string being generated.
 *
 * @author Steve Ebersole
 */
public interface JdbcParameterRenderer extends Service {
	/**
	 * Render the parameter for the given position
	 *
	 * @param position The 1-based position of the parameter.
	 * @param jdbcType The type of the parameter
	 * @param appender The appender where the parameter should be rendered
	 * @param dialect The Dialect in use within the SessionFactory
	 */
	void renderJdbcParameter(int position, JdbcType jdbcType, SqlAppender appender, Dialect dialect);

	static boolean isStandardRenderer(JdbcParameterRenderer check) {
		return check == null || JdbcParameterRendererStandard.class.equals( check.getClass() );
	}
}
