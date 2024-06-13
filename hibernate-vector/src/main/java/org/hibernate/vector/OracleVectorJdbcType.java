/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.vector;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleTypes;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Specialized type mapping for generic vector {@link SqlTypes#VECTOR} SQL data type for Oracle.
 * <p>
 * This class handles generic vectors represented by an asterisk (*) in the format,
 * allowing for different element types within the vector.
 *
 * @author Hassan AL Meftah
 */
public class OracleVectorJdbcType extends OracleFloatVectorJdbcType {


	public OracleVectorJdbcType(JdbcType elementJdbcType, boolean isVectorSupported) {
		super( elementJdbcType, isVectorSupported );
	}

	@Override
	public String getFriendlyName() {
		return "VECTOR";
	}

	@Override
	public void appendWriteExpression(String writeExpression, SqlAppender appender, Dialect dialect) {
		appender.append( "to_vector(" );
		appender.append( writeExpression );
		appender.append( ", *, *)" );
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.VECTOR;
	}

	@Override
	protected int getNativeTypeCode() {
		return OracleTypes.VECTOR;
	}
}
