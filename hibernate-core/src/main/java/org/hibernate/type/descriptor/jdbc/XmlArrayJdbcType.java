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
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Specialized type mapping for {@code XML_ARRAY} and the XML ARRAY SQL data type.
 *
 * @author Christian Beikov
 */
public class XmlArrayJdbcType extends ArrayJdbcType {

	public XmlArrayJdbcType(JdbcType elementJdbcType) {
		super( elementJdbcType );
	}

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.SQLXML;
	}

	@Override
	public int getDdlTypeCode() {
		return SqlTypes.SQLXML;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.XML_ARRAY;
	}

	@Override
	public String toString() {
		return "XmlArrayJdbcType";
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
		if ( javaType.getJavaType() == SQLXML.class ) {
			final SQLXML sqlxml =
					options.getSession().getJdbcCoordinator().getLogicalConnection().getPhysicalConnection()
							.createSQLXML();
			sqlxml.setString( string );
			//noinspection unchecked
			return (X) sqlxml;
		}
		return XmlHelper.arrayFromString( javaType, this, string, options );
	}

	protected <X> String toString(X value, JavaType<X> javaType, WrapperOptions options) {
		final var elementJdbcType = getElementJdbcType();
		final Object[] domainObjects = javaType.unwrap( value, Object[].class, options );
		if ( elementJdbcType instanceof XmlJdbcType xmlElementJdbcType ) {
			final EmbeddableMappingType embeddableMappingType = xmlElementJdbcType.getEmbeddableMappingType();
			return XmlHelper.arrayToString( embeddableMappingType, domainObjects, options );
		}
		else {
			assert !( elementJdbcType instanceof AggregateJdbcType );
			final JavaType<?> elementJavaType = ( (BasicPluralJavaType<?>) javaType ).getElementJavaType();
			return XmlHelper.arrayToString( elementJavaType, elementJdbcType, domainObjects, options );
		}
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new XmlArrayBinder<>( javaType, this );
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

			private XmlArrayJdbcType getXmlArrayJdbcType() {
				return (XmlArrayJdbcType) getJdbcType();
			}

			private X getObject(SQLXML sqlxml, WrapperOptions options) throws SQLException {
				return sqlxml == null ? null
						: getXmlArrayJdbcType().fromString( sqlxml.getString(), getJavaType(), options );
			}

		};
	}

	protected static class XmlArrayBinder<X> extends BasicBinder<X> {
		public XmlArrayBinder(JavaType<X> javaType, XmlArrayJdbcType jdbcType) {
			super( javaType, jdbcType );
		}

		private XmlArrayJdbcType getXmlArrayJdbcType() {
			return (XmlArrayJdbcType) getJdbcType();
		}

		private SQLXML getSqlxml(PreparedStatement st, X value, WrapperOptions options) throws SQLException {
			final String xml = getXmlArrayJdbcType().toString( value, getJavaType(), options );
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
