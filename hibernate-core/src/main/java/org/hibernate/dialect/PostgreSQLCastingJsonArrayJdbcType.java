/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.jdbc.JsonArrayJdbcType;

/**
 * @author Christian Beikov
 */
public class PostgreSQLCastingJsonArrayJdbcType extends JsonArrayJdbcType {

	public static final PostgreSQLCastingJsonArrayJdbcType JSON_INSTANCE = new PostgreSQLCastingJsonArrayJdbcType( false );
	public static final PostgreSQLCastingJsonArrayJdbcType JSONB_INSTANCE = new PostgreSQLCastingJsonArrayJdbcType( true );

	private final boolean jsonb;

	public PostgreSQLCastingJsonArrayJdbcType(boolean jsonb) {
		this.jsonb = jsonb;
	}

	@Override
	public void appendWriteExpression(
			String writeExpression,
			SqlAppender appender,
			Dialect dialect) {
		appender.append( "cast(" );
		appender.append( writeExpression );
		appender.append( " as " );
		if ( jsonb ) {
			appender.append( "jsonb)" );
		}
		else {
			appender.append( "json)" );
		}
	}
}
