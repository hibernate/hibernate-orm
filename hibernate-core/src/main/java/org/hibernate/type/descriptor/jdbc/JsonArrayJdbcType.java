/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.StringJsonDocumentWriter;

/**
 * Specialized type mapping for {@code JSON_ARRAY} and the JSON ARRAY SQL data type.
 *
 * @author Christian Beikov
 */
public class JsonArrayJdbcType extends ArrayJdbcType {

	public JsonArrayJdbcType(JdbcType elementJdbcType) {
		super( elementJdbcType );
	}

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.VARCHAR;
	}

	@Override
	public int getDdlTypeCode() {
		return SqlTypes.JSON;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.JSON_ARRAY;
	}

	@Override
	public String toString() {
		return "JsonArrayJdbcType";
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		// No literal support for now
		return null;
	}

	protected <X> X fromString(String string, JavaType<X> javaType, WrapperOptions options) throws SQLException {
		if ( string == null ) {
			return null;
		}
		return JsonHelper.arrayFromString( javaType, this.getElementJdbcType(), string, options );
	}

	protected <X> String toString(X value, JavaType<X> javaType, WrapperOptions options) {
		final JdbcType elementJdbcType = getElementJdbcType();
		final Object[] domainObjects = javaType.unwrap( value, Object[].class, options );
		StringBuilder sb = new StringBuilder();
		StringJsonDocumentWriter writer = new StringJsonDocumentWriter(new JsonHelper.JsonAppender(sb) );
		if ( elementJdbcType instanceof JsonJdbcType jsonElementJdbcType ) {
			final EmbeddableMappingType embeddableMappingType = jsonElementJdbcType.getEmbeddableMappingType();
			JsonHelper.serializeArray( embeddableMappingType, domainObjects, options,  writer);
		}
		else {
			assert !( elementJdbcType instanceof AggregateJdbcType );
			final JavaType<?> elementJavaType = ( (BasicPluralJavaType<?>) javaType ).getElementJavaType();
			JsonHelper.serializeArray( elementJavaType, elementJdbcType, domainObjects, options, writer );
		}
		return sb.toString();
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final String json = ( (JsonArrayJdbcType) getJdbcType() ).toString( value, getJavaType(), options );
				st.setString( index, json );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final String json = ( (JsonArrayJdbcType) getJdbcType() ).toString( value, getJavaType(), options );
				st.setString( name, json );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getObject( rs.getString( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getObject( statement.getString( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return getObject( statement.getString( name ), options );
			}

			private X getObject(String json, WrapperOptions options) throws SQLException {
				return ( (JsonArrayJdbcType) getJdbcType() ).fromString(
						json,
						getJavaType(),
						options
				);
			}

		};
	}
}
