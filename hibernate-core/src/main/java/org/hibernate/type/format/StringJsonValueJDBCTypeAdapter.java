/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

import org.hibernate.internal.util.CharSequenceHelper;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JdbcDateJavaType;
import org.hibernate.type.descriptor.java.JdbcTimeJavaType;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.StructAttributeValues;
import org.hibernate.type.descriptor.jdbc.StructHelper;

import java.sql.SQLException;

import static org.hibernate.type.descriptor.jdbc.StructHelper.instantiate;

/**
 * JDBC type adapter for String-based JSON document reader.
 */
public class StringJsonValueJDBCTypeAdapter implements JsonValueJDBCTypeAdapter {

	private boolean returnEmbeddable;
	public StringJsonValueJDBCTypeAdapter(boolean returnEmbeddable) {
		this.returnEmbeddable = returnEmbeddable;
	}

	@Override
	public Object fromValue(JavaType<?> jdbcJavaType, JdbcType jdbcType, JsonDocumentReader source, WrapperOptions options)
			throws SQLException {
		return fromAnyValue (jdbcJavaType, jdbcType, source, options);
	}

	private Object fromAnyValue(JavaType<?> jdbcJavaType, JdbcType jdbcType, JsonDocumentReader source, WrapperOptions options)
			throws SQLException {

		String string = source.getStringValue();

		switch ( jdbcType.getDefaultSqlTypeCode() ) {
			case SqlTypes.BINARY:
			case SqlTypes.VARBINARY:
			case SqlTypes.LONGVARBINARY:
			case SqlTypes.LONG32VARBINARY:
				return jdbcJavaType.wrap(
						PrimitiveByteArrayJavaType.INSTANCE.fromEncodedString(
								string,
								0, string.length()),
						options
				);
			case SqlTypes.UUID:
				return jdbcJavaType.wrap(
						PrimitiveByteArrayJavaType.INSTANCE.fromString(
								string.substring( 0, string.length() ).replace( "-", "" )
						),
						options
				);
			case SqlTypes.DATE:
				return jdbcJavaType.wrap(
						JdbcDateJavaType.INSTANCE.fromEncodedString(
								string,
								0,
								string.length()
						),
						options
				);
			case SqlTypes.TIME:
			case SqlTypes.TIME_WITH_TIMEZONE:
			case SqlTypes.TIME_UTC:
				return jdbcJavaType.wrap(
						JdbcTimeJavaType.INSTANCE.fromEncodedString(
								string,
								0,
								string.length()
						),
						options
				);
			case SqlTypes.TIMESTAMP:
				return jdbcJavaType.wrap(
						JdbcTimestampJavaType.INSTANCE.fromEncodedString(
								string,
								0,
								string.length()
						),
						options
				);
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
			case SqlTypes.TIMESTAMP_UTC:
				return jdbcJavaType.wrap(
						OffsetDateTimeJavaType.INSTANCE.fromEncodedString(
								string,
								0,
								string.length()),
						options
				);
			case SqlTypes.TINYINT:
			case SqlTypes.SMALLINT:
			case SqlTypes.INTEGER:
				if ( jdbcJavaType.getJavaTypeClass() == Boolean.class ) {
					return jdbcJavaType.wrap( Integer.parseInt( string, 0, string.length(), 10 ), options );
				}
				else if ( jdbcJavaType instanceof EnumJavaType<?> ) {
					return jdbcJavaType.wrap( Integer.parseInt( string, 0, string.length(), 10 ), options );
				}
			case SqlTypes.CHAR:
			case SqlTypes.NCHAR:
			case SqlTypes.VARCHAR:
			case SqlTypes.NVARCHAR:
				if ( jdbcJavaType.getJavaTypeClass() == Boolean.class && (string.length() == 1 ) ) {
					return jdbcJavaType.wrap( string.charAt( 0 ), options );
				}
			default:
				if ( jdbcType instanceof AggregateJdbcType aggregateJdbcType ) {
					final Object[] subValues = aggregateJdbcType.extractJdbcValues(
							CharSequenceHelper.subSequence(
									string,
									0,
									string.length()),
							options
					);
					if ( returnEmbeddable ) {
						final StructAttributeValues subAttributeValues = StructHelper.getAttributeValues(
								aggregateJdbcType.getEmbeddableMappingType(),
								subValues,
								options
						);
						final EmbeddableMappingType embeddableMappingType = aggregateJdbcType.getEmbeddableMappingType();
						return instantiate( embeddableMappingType, subAttributeValues ) ;
					}
					return subValues;
				}

				return jdbcJavaType.fromEncodedString(string);
		}
	}

	@Override
	public Object fromNumericValue(JavaType<?> jdbcJavaType, JdbcType jdbcType, JsonDocumentReader source, WrapperOptions options) throws SQLException  {
		return fromAnyValue (jdbcJavaType, jdbcType, source, options);
	}
}
