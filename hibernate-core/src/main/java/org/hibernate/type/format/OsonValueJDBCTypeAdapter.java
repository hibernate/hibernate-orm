/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JdbcDateJavaType;
import org.hibernate.type.descriptor.java.JdbcTimeJavaType;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.sql.SQLException;


/**
 * JDBC type adapter for OSON-based JSON document reader.
 */
public class OsonValueJDBCTypeAdapter implements JsonValueJDBCTypeAdapter {
	@Override
	public Object fromValue(JavaType<?> jdbcJavaType, JdbcType jdbcType, JsonDocumentReader source, WrapperOptions options)
			throws SQLException {
		Object valueToBeWrapped = null;
		switch ( jdbcType.getDefaultSqlTypeCode() ) {
			case SqlTypes.BINARY:
			case SqlTypes.VARBINARY:
			case SqlTypes.LONGVARBINARY:
			case SqlTypes.LONG32VARBINARY:
				valueToBeWrapped = source.getValue( PrimitiveByteArrayJavaType.INSTANCE, options );
				break;
			case SqlTypes.DATE:
				valueToBeWrapped = source.getValue( JdbcDateJavaType.INSTANCE , options);
				break;
			case SqlTypes.TIME:
			case SqlTypes.TIME_WITH_TIMEZONE:
			case SqlTypes.TIME_UTC:
				valueToBeWrapped = source.getValue( JdbcTimeJavaType.INSTANCE , options);
				break;
			case SqlTypes.TIMESTAMP:
				valueToBeWrapped = source.getValue( JdbcTimestampJavaType.INSTANCE , options);
				break;
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
			case SqlTypes.TIMESTAMP_UTC:
				valueToBeWrapped = source.getValue( OffsetDateTimeJavaType.INSTANCE , options);
				break;
			case SqlTypes.TINYINT:
			case SqlTypes.SMALLINT:
			case SqlTypes.INTEGER:
				if ( jdbcJavaType.getJavaTypeClass() == Boolean.class ) {
					valueToBeWrapped = source.getIntegerValue();
					break;
				}
				else if ( jdbcJavaType instanceof EnumJavaType<?> ) {
					valueToBeWrapped = source.getIntegerValue();
					break;
				}
			case SqlTypes.CHAR:
			case SqlTypes.NCHAR:
			case SqlTypes.VARCHAR:
			case SqlTypes.NVARCHAR:
				if ( jdbcJavaType.getJavaTypeClass() == Boolean.class ) {
					valueToBeWrapped = source.getBooleanValue();
					break;
				}
		}
		if (valueToBeWrapped == null) {
			valueToBeWrapped = source.getValue( jdbcJavaType , options);
		}
		return jdbcJavaType.wrap(valueToBeWrapped, options);
	}

	@Override
	public Object fromNumericValue(JavaType<?> jdbcJavaType, JdbcType jdbcType, JsonDocumentReader source, WrapperOptions options)
			throws SQLException {
		return fromValue( jdbcJavaType, jdbcType, source, options );
	}
}
