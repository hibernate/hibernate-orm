/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
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
		return options.getSessionFactory().getFastSessionServices().getXmlFormatMapper().fromString(
				string,
				javaType,
				options
		);
	}

	protected <X> String toString(X value, JavaType<X> javaType, WrapperOptions options) {
		return options.getSessionFactory().getFastSessionServices().getXmlFormatMapper().toString(
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
				final String xml = ( (XmlArrayJdbcType ) getJdbcType() ).toString( value, getJavaType(), options );
				SQLXML sqlxml = st.getConnection().createSQLXML();
				sqlxml.setString( xml );
				st.setSQLXML( index, sqlxml );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final String xml = ( (XmlArrayJdbcType ) getJdbcType() ).toString( value, getJavaType(), options );
				SQLXML sqlxml = st.getConnection().createSQLXML();
				sqlxml.setString( xml );
				st.setSQLXML( name, sqlxml );
			}
		};
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
				if ( sqlxml == null ) {
					return null;
				}
				return ( (XmlArrayJdbcType ) getJdbcType() ).fromString(
						sqlxml.getString(),
						getJavaType(),
						options
				);
			}

		};
	}
}
