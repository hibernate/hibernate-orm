/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.dialect.JsonHelper;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Specialized type mapping for {@code JSON} and the JSON SQL data type.
 *
 * @author Christian Beikov
 */
public class JsonJdbcType implements AggregateJdbcType {
	/**
	 * Singleton access
	 */
	public static final JsonJdbcType INSTANCE = new JsonJdbcType( null );

	private final EmbeddableMappingType embeddableMappingType;

	protected JsonJdbcType(EmbeddableMappingType embeddableMappingType) {
		this.embeddableMappingType = embeddableMappingType;
	}

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.VARCHAR;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.JSON;
	}

	@Override
	public String toString() {
		return "JsonJdbcType";
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		// No literal support for now
		return null;
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new JsonJdbcType( mappingType );
	}

	@Override
	public EmbeddableMappingType getEmbeddableMappingType() {
		return embeddableMappingType;
	}

	protected <X> X fromString(String string, JavaType<X> javaType, WrapperOptions options) throws SQLException {
		if ( string == null ) {
			return null;
		}
		if ( embeddableMappingType != null ) {
			return JsonHelper.fromString(
					embeddableMappingType,
					string,
					javaType.getJavaTypeClass() != Object[].class,
					options
			);
		}
		return options.getSessionFactory().getFastSessionServices().getJsonFormatMapper().fromString(
				string,
				javaType,
				options
		);
	}

	@Override
	public Object createJdbcValue(Object domainValue, WrapperOptions options) throws SQLException {
		assert embeddableMappingType != null;
		return JsonHelper.toString( embeddableMappingType, domainValue, options );
	}

	@Override
	public Object[] extractJdbcValues(Object rawJdbcValue, WrapperOptions options) throws SQLException {
		assert embeddableMappingType != null;
		return JsonHelper.fromString( embeddableMappingType, (String) rawJdbcValue, false, options );
	}

	protected <X> String toString(X value, JavaType<X> javaType, WrapperOptions options) {
		if ( embeddableMappingType != null ) {
			return JsonHelper.toString( embeddableMappingType, value, options );
		}
		return options.getSessionFactory().getFastSessionServices().getJsonFormatMapper().toString(
				value,
				javaType,
				options
		);
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final String json = ( (JsonJdbcType) getJdbcType() ).toString( value, getJavaType(), options );
				st.setString( index, json );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final String json = ( (JsonJdbcType) getJdbcType() ).toString( value, getJavaType(), options );
				st.setString( name, json );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return fromString( rs.getString( paramIndex ), getJavaType(), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return fromString( statement.getString( index ), getJavaType(), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return fromString( statement.getString( name ), getJavaType(), options );
			}

		};
	}
}
