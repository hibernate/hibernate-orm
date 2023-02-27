/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.ast.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.sql.ast.spi.JdbcParameterRenderer;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * @author Steve Ebersole
 */
public class JdbcParameterRendererStandard implements JdbcParameterRenderer {
	/**
	 * Singleton access
	 */
	public static final JdbcParameterRendererStandard INSTANCE = new JdbcParameterRendererStandard();

	@Override
	public void renderJdbcParameter(int position, JdbcType jdbcType, SqlAppender appender, Dialect dialect) {
		jdbcType.appendWriteExpression( "?", appender, dialect );
	}
}
