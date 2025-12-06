/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Specialized type mapping for {@link SQLXML} and the XML SQL data type.
 *
 * @author Christian Beikov
 */
public class XmlJdbcType implements AggregateJdbcType {
	/**
	 * Singleton access
	 */
	public static final XmlJdbcType INSTANCE = new XmlJdbcType( null );

	private final EmbeddableMappingType embeddableMappingType;

	protected XmlJdbcType(EmbeddableMappingType embeddableMappingType) {
		this.embeddableMappingType = embeddableMappingType;
	}

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.SQLXML;
	}

	@Override
	public String toString() {
		return "XmlJdbcType";
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
		return new XmlJdbcType( mappingType );
	}

	@Override
	public EmbeddableMappingType getEmbeddableMappingType() {
		return embeddableMappingType;
	}

	@Override
	public Object createJdbcValue(Object domainValue, WrapperOptions options) throws SQLException {
		assert embeddableMappingType != null;
		return XmlHelper.toString( embeddableMappingType, domainValue, options );
	}

	@Override
	public Object[] extractJdbcValues(Object rawJdbcValue, WrapperOptions options) throws SQLException {
		assert embeddableMappingType != null;
		return XmlHelper.fromString( embeddableMappingType, (String) rawJdbcValue, false, options );
	}

	protected <X> String toString(X value, JavaType<X> javaType, WrapperOptions options) throws SQLException {
		if ( embeddableMappingType != null ) {
			return XmlHelper.toString( embeddableMappingType, value, options );
		}
		return options.getXmlFormatMapper().toString( value, javaType, options );
	}

	protected <X> X fromString(String string, JavaType<X> javaType, WrapperOptions options) throws SQLException {
		if ( embeddableMappingType != null ) {
			return XmlHelper.fromString(
					embeddableMappingType,
					string,
					javaType.getJavaTypeClass() != Object[].class,
					options
			);
		}
		if ( javaType.getJavaType() == SQLXML.class ) {
			final SQLXML sqlxml =
					options.getSession().getJdbcCoordinator()
							.getLogicalConnection().getPhysicalConnection()
							.createSQLXML();
			sqlxml.setString( string );
			return javaType.cast( sqlxml );
		}
		return options.getXmlFormatMapper().fromString( string, javaType, options );
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new XmlValueBinder<>( javaType, this );
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getObject( rs.getSQLXML( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getObject( statement.getSQLXML( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return getObject( statement.getSQLXML( name ), options );
			}

			private X getObject(SQLXML sqlxml, WrapperOptions options) throws SQLException {
				return sqlxml == null ? null
						: getXmlJdbcType().fromString( sqlxml.getString(), getJavaType(), options );
			}

			private XmlJdbcType getXmlJdbcType() {
				return (XmlJdbcType) getJdbcType();
			}
		};
	}

	protected static class XmlValueBinder<X> extends BasicBinder<X> {
		public XmlValueBinder(JavaType<X> javaType, JdbcType jdbcType) {
			super( javaType, jdbcType );
		}

		private XmlJdbcType getXmlJdbcType() {
			return (XmlJdbcType) getJdbcType();
		}

		private SQLXML getSqlxml(PreparedStatement st, X value, WrapperOptions options) throws SQLException {
			final String xml = getXmlJdbcType().toString( value, getJavaType(), options );
			SQLXML sqlxml = st.getConnection().createSQLXML();
			sqlxml.setString( xml );
			return sqlxml;
		}

		@Override
		protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
				throws SQLException {
			st.setSQLXML( index, getSqlxml( st, value, options ) );
		}

		@Override
		protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
				throws SQLException {
			st.setSQLXML( name, getSqlxml( st, value, options ) );
		}
	}
}
